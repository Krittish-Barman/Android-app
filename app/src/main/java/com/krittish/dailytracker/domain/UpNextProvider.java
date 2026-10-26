package com.krittish.dailytracker.domain;

import com.krittish.dailytracker.data.db.Task;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.DateUtils;

import java.util.List;

/** Finds the nearest timed habit or task today that is still ahead of us. */
public final class UpNextProvider {
    private UpNextProvider() {}

    public static class UpNext {
        public String kind;      // Constants.KIND_HABIT / KIND_TASK
        public long id;
        public String title;
        public long timeMillis;
    }

    public static UpNext compute(List<HabitRow> rows, List<Task> tasks, long now) {
        long today = DateUtils.today();
        UpNext best = null;
        for (HabitRow r : rows) {
            if (!r.dueToday || r.doneToday || r.skippedToday || r.habit.reminderTime < 0) continue;
            long t = DateUtils.millisAt(today, r.habit.reminderTime);
            if (t < now) continue;
            if (best == null || t < best.timeMillis) best = make(Constants.KIND_HABIT, r.habit.id, r.habit.name, t);
        }
        for (Task k : tasks) {
            if (k.done || k.dueAt <= 0 || k.dueAt < now) continue;
            if (DateUtils.epochDayOf(k.dueAt) != today) continue;
            if (best == null || k.dueAt < best.timeMillis) best = make(Constants.KIND_TASK, k.id, k.title, k.dueAt);
        }
        return best;
    }

    private static UpNext make(String kind, long id, String title, long t) {
        UpNext n = new UpNext();
        n.kind = kind;
        n.id = id;
        n.title = title;
        n.timeMillis = t;
        return n;
    }
}
