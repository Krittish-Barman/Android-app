package com.krittish.dailytracker.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Dates are epoch days (long). Times are epoch milliseconds (long). */
public final class DateUtils {
    private DateUtils() {}

    private static ZoneId zone() { return ZoneId.systemDefault(); }

    public static long today() { return LocalDate.now().toEpochDay(); }

    public static long epochDayOf(long millis) {
        return Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay();
    }

    public static long startOfDayMillis(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).atStartOfDay(zone()).toInstant().toEpochMilli();
    }

    public static long millisAt(long epochDay, int minuteOfDay) {
        return LocalDate.ofEpochDay(epochDay).atTime(minuteOfDay / 60, minuteOfDay % 60)
                .atZone(zone()).toInstant().toEpochMilli();
    }

    public static long millisOf(int year, int month1to12, int day, int hour, int minute) {
        return LocalDateTime.of(year, month1to12, day, hour, minute).atZone(zone()).toInstant().toEpochMilli();
    }

    /** 0 = Monday ... 6 = Sunday */
    public static int dayOfWeek(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).getDayOfWeek().getValue() - 1;
    }

    public static long weekStart(long epochDay) { return epochDay - dayOfWeek(epochDay); }

    public static int minuteOfDay(long millis) {
        LocalTime t = Instant.ofEpochMilli(millis).atZone(zone()).toLocalTime();
        return t.getHour() * 60 + t.getMinute();
    }

    public static int hourOf(long millis) {
        return Instant.ofEpochMilli(millis).atZone(zone()).getHour();
    }

    // ---- months, keys look like "2026-10" ----
    public static String currentMonthKey() { return YearMonth.now().toString(); }

    public static String shiftMonth(String key, int delta) {
        return YearMonth.parse(key).plusMonths(delta).toString();
    }

    public static long monthStartMillis(String key) {
        return YearMonth.parse(key).atDay(1).atStartOfDay(zone()).toInstant().toEpochMilli();
    }

    /** Exclusive end (first millisecond of next month). */
    public static long monthEndMillis(String key) {
        return YearMonth.parse(key).plusMonths(1).atDay(1).atStartOfDay(zone()).toInstant().toEpochMilli();
    }

    public static String monthLabel(String key) {
        return YearMonth.parse(key).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
    }

    // ---- text ----
    public static String formatTime(int minuteOfDay) {
        return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
    }

    public static String formatMillisTime(long millis) { return formatTime(minuteOfDay(millis)); }

    /** "Tue, 20 Oct" */
    public static String formatDay(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH));
    }

    /** "20 Oct, 7:42 PM" */
    public static String formatDateTime(long millis) {
        return Instant.ofEpochMilli(millis).atZone(zone())
                .format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH));
    }

    /** "Today" / "Yesterday" / "20 Oct" */
    public static String dayLabel(long millis) {
        long d = epochDayOf(millis);
        long t = today();
        if (d == t) return "Today";
        if (d == t - 1) return "Yesterday";
        return LocalDate.ofEpochDay(d).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH));
    }

    public static String greeting() {
        int h = LocalTime.now().getHour();
        if (h < 12) return "Good morning";
        if (h < 17) return "Good afternoon";
        return "Good evening";
    }

    /** 85 -> "1h 25m", 40 -> "40m" */
    public static String formatMinutes(int minutes) {
        if (minutes < 60) return minutes + "m";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}
