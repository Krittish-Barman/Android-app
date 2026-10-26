package com.krittish.dailytracker;

import static org.junit.Assert.assertEquals;

import com.krittish.dailytracker.domain.StreakCalculator;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class StreakCalculatorTest {

    @Test
    public void dailyStreakCountsDoneDays_todayPendingIsNotAMiss() {
        Map<Long, String> m = new HashMap<>();
        for (long d = 95; d <= 99; d++) m.put(d, "DONE");
        StreakCalculator.Result r = StreakCalculator.compute("DAILY", 0, 3, 90, m, 100);
        assertEquals(5, r.current);
        assertEquals(5, r.best);
    }

    @Test
    public void missedDayResetsStreak_butBestIsKept() {
        Map<Long, String> m = new HashMap<>();
        for (long d = 90; d <= 94; d++) m.put(d, "DONE");   // 5 in a row
        m.put(96L, "DONE");
        m.put(97L, "DONE");                                  // 95 missed
        StreakCalculator.Result r = StreakCalculator.compute("DAILY", 0, 3, 90, m, 97);
        assertEquals(2, r.current);
        assertEquals(5, r.best);
    }

    @Test
    public void skippedDayKeepsStreakWithoutAddingToIt() {
        Map<Long, String> m = new HashMap<>();
        m.put(98L, "DONE");
        m.put(99L, "SKIPPED");
        m.put(100L, "DONE");
        assertEquals(2, StreakCalculator.compute("DAILY", 0, 3, 98, m, 100).current);
    }

    @Test
    public void weekdaysScheduleIgnoresOffDays() {
        // epoch day 4 = Monday. Mon/Wed/Fri mask = bits 0, 2, 4.
        Map<Long, String> m = new HashMap<>();
        m.put(4L, "DONE");
        m.put(6L, "DONE");
        m.put(8L, "DONE");
        m.put(11L, "DONE");
        assertEquals(4, StreakCalculator.compute("WEEKDAYS", 0b10101, 3, 4, m, 13).current);
    }

    @Test
    public void timesPerWeekCountsWeeksThatMetTheTarget() {
        Map<Long, String> m = new HashMap<>();
        m.put(4L, "DONE");
        m.put(5L, "DONE");
        m.put(11L, "DONE");
        m.put(12L, "DONE");
        assertEquals(2, StreakCalculator.compute("TIMES_PER_WEEK", 0, 2, 4, m, 19).current);
        assertEquals(0, StreakCalculator.compute("TIMES_PER_WEEK", 0, 3, 4, m, 19).current);
    }
}
