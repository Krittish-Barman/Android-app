package com.krittish.dailytracker.ui.stats;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.db.Category;
import com.krittish.dailytracker.data.db.CategoryTotal;
import com.krittish.dailytracker.data.db.FocusSession;
import com.krittish.dailytracker.data.db.Habit;
import com.krittish.dailytracker.data.db.HabitLog;
import com.krittish.dailytracker.domain.StreakCalculator;
import com.krittish.dailytracker.util.AppExecutors;
import com.krittish.dailytracker.util.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsViewModel extends AndroidViewModel {

    public static class Row {
        public String label;
        public int percent;
        public String valueText;
    }

    public static class Data {
        public float[] study;
        public String[] labels;
        public int totalMinutes;
        public String bestTime;
        public List<Row> habits = new ArrayList<>();
        public List<Row> spending = new ArrayList<>();
        public long incomePaise;
        public long expensePaise;
    }

    private final MutableLiveData<Data> data = new MutableLiveData<>();

    public StatsViewModel(@NonNull Application app) { super(app); }

    public LiveData<Data> data() { return data; }

    /** Loads stats for the last 7 or 30 days on a background thread. */
    public void load(boolean month) {
        final AppDatabase db = AppDatabase.get(getApplication());
        AppExecutors.DISK_IO.execute(() -> {
            long today = DateUtils.today();
            int days = month ? 30 : 7;
            long start = today - days + 1;
            long fromMs = DateUtils.startOfDayMillis(start);
            long toMs = DateUtils.startOfDayMillis(today + 1);
            Data d = new Data();

            // ---- study minutes per day + best study hours ----
            d.study = new float[days];
            d.labels = new String[days];
            String[] letters = {"M", "T", "W", "T", "F", "S", "S"};
            for (int i = 0; i < days; i++) {
                long day = start + i;
                d.labels[i] = month ? ((i % 5 == 4 || i == days - 1) ? String.valueOf(LocalDay(day)) : "")
                        : letters[DateUtils.dayOfWeek(day)];
            }
            int[] hourMinutes = new int[24];
            for (FocusSession s : db.focusDao().between(fromMs, toMs)) {
                int idx = (int) (DateUtils.epochDayOf(s.startTime) - start);
                if (idx >= 0 && idx < days) d.study[idx] += s.focusedMinutes;
                hourMinutes[DateUtils.hourOf(s.startTime)] += s.focusedMinutes;
                d.totalMinutes += s.focusedMinutes;
            }
            int bestHour = -1, bestSum = 0;
            for (int h = 0; h < 23; h++) {
                int sum = hourMinutes[h] + hourMinutes[h + 1];
                if (sum > bestSum) { bestSum = sum; bestHour = h; }
            }
            d.bestTime = bestHour < 0 ? "No study sessions yet"
                    : "Best study time: " + DateUtils.formatTime(bestHour * 60) + " – " + DateUtils.formatTime((bestHour + 2) * 60 % 1440);

            // ---- habit completion ----
            Map<Long, Integer> doneByHabit = new HashMap<>();
            for (HabitLog l : db.habitLogDao().between(start, today)) {
                if ("DONE".equals(l.status)) {
                    Integer c = doneByHabit.get(l.habitId);
                    doneByHabit.put(l.habitId, c == null ? 1 : c + 1);
                }
            }
            for (Habit h : db.habitDao().activeNow()) {
                long from = Math.max(start, DateUtils.epochDayOf(h.createdAt > 0 ? h.createdAt : System.currentTimeMillis()));
                int scheduled = 0;
                if ("TIMES_PER_WEEK".equals(h.scheduleType)) {
                    scheduled = Math.max(1, Math.round(h.timesPerWeek * (today - from + 1) / 7f));
                } else {
                    for (long day = from; day <= today; day++) {
                        if (StreakCalculator.isScheduled(h.scheduleType, h.weekdaysMask, day)) scheduled++;
                    }
                }
                Integer done = doneByHabit.get(h.id);
                Row r = new Row();
                r.label = h.name;
                r.percent = scheduled == 0 ? 0 : Math.min(100, (done == null ? 0 : done) * 100 / scheduled);
                r.valueText = r.percent + "%";
                d.habits.add(r);
            }

            // ---- spending by category ----
            Map<Long, String> names = new HashMap<>();
            for (Category c : db.categoryDao().allNow()) names.put(c.id, c.name);
            List<CategoryTotal> totals = db.moneyDao().expenseByCategoryNow(fromMs, toMs);
            long sum = 0;
            for (CategoryTotal t : totals) sum += t.total;
            for (CategoryTotal t : totals) {
                Row r = new Row();
                String name = names.get(t.categoryId);
                r.label = name == null ? "No category" : name;
                r.percent = sum == 0 ? 0 : (int) (t.total * 100 / sum);
                r.valueText = r.percent + "% · " + com.krittish.dailytracker.util.MoneyFormat.format(t.total);
                d.spending.add(r);
            }
            d.expensePaise = db.moneyDao().expenseSumNow(fromMs, toMs);
            d.incomePaise = db.moneyDao().incomeSumNow(fromMs, toMs);
            data.postValue(d);
        });
    }

    private static int LocalDay(long epochDay) {
        return java.time.LocalDate.ofEpochDay(epochDay).getDayOfMonth();
    }
}
