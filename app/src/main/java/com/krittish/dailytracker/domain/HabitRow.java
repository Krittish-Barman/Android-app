package com.krittish.dailytracker.domain;

import com.krittish.dailytracker.data.db.Habit;
import com.krittish.dailytracker.data.db.HabitLog;
import com.krittish.dailytracker.util.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A habit plus everything the screens need to draw it for "today". */
public class HabitRow {
    public Habit habit;
    public boolean doneToday;
    public boolean skippedToday;
    public boolean dueToday;
    public int doneThisWeek;
    public int streak;
    public int bestStreak;

    public static List<HabitRow> build(List<Habit> habits, List<HabitLog> logs, long today) {
        Map<Long, Map<Long, String>> byHabit = new HashMap<>();
        for (HabitLog l : logs) {
            Map<Long, String> m = byHabit.get(l.habitId);
            if (m == null) {
                m = new HashMap<>();
                byHabit.put(l.habitId, m);
            }
            m.put(l.date, l.status);
        }
        long weekStart = DateUtils.weekStart(today);
        List<HabitRow> out = new ArrayList<>();
        for (Habit h : habits) {
            Map<Long, String> m = byHabit.get(h.id);
            if (m == null) m = new HashMap<>();
            HabitRow r = new HabitRow();
            r.habit = h;
            String st = m.get(today);
            r.doneToday = "DONE".equals(st);
            r.skippedToday = "SKIPPED".equals(st);
            for (long d = weekStart; d <= today; d++) {
                if ("DONE".equals(m.get(d))) r.doneThisWeek++;
            }
            if ("TIMES_PER_WEEK".equals(h.scheduleType)) {
                r.dueToday = r.doneToday || r.doneThisWeek < h.timesPerWeek;
            } else {
                r.dueToday = StreakCalculator.isScheduled(h.scheduleType, h.weekdaysMask, today);
            }
            long start = DateUtils.epochDayOf(h.createdAt > 0 ? h.createdAt : System.currentTimeMillis());
            for (Long d : m.keySet()) {
                if (d < start) start = d;
            }
            StreakCalculator.Result res = StreakCalculator.compute(
                    h.scheduleType, h.weekdaysMask, h.timesPerWeek, start, m, today);
            r.streak = res.current;
            r.bestStreak = res.best;
            out.add(r);
        }
        return out;
    }
}
