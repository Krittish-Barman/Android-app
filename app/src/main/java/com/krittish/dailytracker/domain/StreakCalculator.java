package com.krittish.dailytracker.domain;

import java.util.Map;

/**
 * Pure Java (no Android classes) so it can be unit-tested on your computer.
 *
 * Rules (from the spec):
 *  - A done scheduled day adds 1 to the streak.
 *  - A skipped day keeps the streak but adds nothing.
 *  - A missed scheduled day resets the streak to 0 (today is never "missed" until the day is over).
 *  - TIMES_PER_WEEK: the streak counts weeks (Mon-Sun) that met the target.
 * Streaks are calculated from the logs every time, so editing a past day is always consistent.
 */
public final class StreakCalculator {
    private StreakCalculator() {}

    public static final class Result {
        public final int current;
        public final int best;

        public Result(int current, int best) {
            this.current = current;
            this.best = best;
        }
    }

    /** Monday = 0 ... Sunday = 6. 1 Jan 1970 (epoch day 0) was a Thursday. */
    static int dayOfWeek(long epochDay) {
        return (int) Math.floorMod(epochDay + 3, 7L);
    }

    public static boolean isScheduled(String scheduleType, int weekdaysMask, long epochDay) {
        if ("WEEKDAYS".equals(scheduleType)) {
            return (weekdaysMask & (1 << dayOfWeek(epochDay))) != 0;
        }
        return true; // DAILY and TIMES_PER_WEEK
    }

    /**
     * @param statusByDay epoch day -> "DONE" or "SKIPPED"
     * @param startDay    first day to look at (habit creation day or earliest log)
     */
    public static Result compute(String scheduleType, int weekdaysMask, int timesPerWeek,
                                 long startDay, Map<Long, String> statusByDay, long today) {
        int run = 0;
        int best = 0;

        if ("TIMES_PER_WEEK".equals(scheduleType)) {
            long firstWeek = startDay - dayOfWeek(startDay);
            long thisWeek = today - dayOfWeek(today);
            int target = Math.max(1, timesPerWeek);
            for (long w = firstWeek; w <= thisWeek; w += 7) {
                int done = 0;
                for (long d = w; d < w + 7; d++) {
                    if ("DONE".equals(statusByDay.get(d))) done++;
                }
                if (done >= target) {
                    run++;
                    best = Math.max(best, run);
                } else if (w != thisWeek) {
                    run = 0;            // a finished week that missed the target
                }                       // the current week is still in progress
            }
            return new Result(run, best);
        }

        for (long d = startDay; d <= today; d++) {
            if (!isScheduled(scheduleType, weekdaysMask, d)) continue;
            String status = statusByDay.get(d);
            if ("DONE".equals(status)) {
                run++;
                best = Math.max(best, run);
            } else if ("SKIPPED".equals(status)) {
                // keeps the streak, adds nothing
            } else if (d != today) {
                run = 0;                // missed a scheduled day
            }                           // today not done yet: not missed until the day ends
        }
        return new Result(run, best);
    }
}
