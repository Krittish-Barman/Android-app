package com.krittish.dailytracker.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.db.Habit;
import com.krittish.dailytracker.data.db.HabitLog;
import com.krittish.dailytracker.domain.HabitRow;
import com.krittish.dailytracker.notifications.NotificationScheduler;
import com.krittish.dailytracker.util.AppExecutors;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.DateUtils;

import java.util.List;

public class HabitRepository {
    private final Context app;
    private final AppDatabase db;

    public HabitRepository(Context context) {
        this.app = context.getApplicationContext();
        this.db = AppDatabase.get(app);
    }

    /** Rows for "today"; call recompute() in onResume so a new day is picked up. */
    public static class RowsLiveData extends MediatorLiveData<List<HabitRow>> {
        private List<Habit> habits;
        private List<HabitLog> logs;

        RowsLiveData(LiveData<List<Habit>> h, LiveData<List<HabitLog>> l) {
            addSource(h, v -> { habits = v; recompute(); });
            addSource(l, v -> { logs = v; recompute(); });
        }

        public void recompute() {
            if (habits != null && logs != null) setValue(HabitRow.build(habits, logs, DateUtils.today()));
        }
    }

    public RowsLiveData observeRows() {
        return new RowsLiveData(db.habitDao().observeActive(), db.habitLogDao().observeAll());
    }

    public LiveData<Habit> observeHabit(long id) { return db.habitDao().observeById(id); }

    public LiveData<List<HabitLog>> observeLogs(long habitId) { return db.habitLogDao().observeForHabit(habitId); }

    /** Tick or untick a day. Safe to call from the main thread. */
    public void setDone(long habitId, long date, boolean done) {
        AppExecutors.DISK_IO.execute(() -> setDoneSync(habitId, date, done));
    }

    /** Background thread only (used by notification buttons too). */
    public void setDoneSync(long habitId, long date, boolean done) {
        if (done) {
            Habit h = db.habitDao().getById(habitId);
            HabitLog log = new HabitLog();
            log.habitId = habitId;
            log.date = date;
            log.status = "DONE";
            log.value = h != null ? h.targetValue : 0;
            db.habitLogDao().upsert(log);
        } else {
            db.habitLogDao().delete(habitId, date);
        }
        afterChange(habitId, date);
    }

    public void skip(long habitId, long date) {
        AppExecutors.DISK_IO.execute(() -> {
            HabitLog log = new HabitLog();
            log.habitId = habitId;
            log.date = date;
            log.status = "SKIPPED";
            db.habitLogDao().upsert(log);
            afterChange(habitId, date);
        });
    }

    private void afterChange(long habitId, long date) {
        if (date == DateUtils.today()) {
            NotificationScheduler.cancelNotification(app, Constants.KIND_HABIT, habitId);
        }
        NotificationScheduler.refreshNow(app);   // re-arms the next "Next up" alarm
    }

    public void save(Habit h) {
        AppExecutors.DISK_IO.execute(() -> {
            if (h.id == 0) {
                if (h.createdAt == 0) h.createdAt = System.currentTimeMillis();
                db.habitDao().insert(h);
            } else {
                db.habitDao().update(h);
            }
            NotificationScheduler.refreshNow(app);
        });
    }

    public void archive(long id) {
        AppExecutors.DISK_IO.execute(() -> {
            Habit h = db.habitDao().getById(id);
            if (h == null) return;
            h.archived = true;
            db.habitDao().update(h);
            NotificationScheduler.refreshNow(app);
        });
    }

    public void getHabit(long id, com.krittish.dailytracker.util.Callback<Habit> cb) {
        AppExecutors.DISK_IO.execute(() -> {
            Habit h = db.habitDao().getById(id);
            AppExecutors.main(() -> cb.on(h));
        });
    }
}
