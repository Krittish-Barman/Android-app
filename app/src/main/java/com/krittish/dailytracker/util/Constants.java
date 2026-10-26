package com.krittish.dailytracker.util;

public final class Constants {
    private Constants() {}

    // Notification channels
    public static final String CH_REMINDERS = "reminders";
    public static final String CH_FOCUS = "focus_timer";
    public static final String CH_INSIGHTS = "insights";
    public static final String CH_MONEY = "money";

    // Notification types (each has its own on/off switch in Settings)
    public static final String NT_NEXT_UP = "nextup";
    public static final String NT_MORNING = "morning";
    public static final String NT_EVENING = "evening";
    public static final String NT_STREAK = "streak";
    public static final String NT_BUDGET = "budget";
    public static final String NT_WEEKLY = "weekly";
    public static final String NT_FOCUS = "focus";
    public static final String NT_DETECTED = "detected";

    // Intent actions
    public static final String ACTION_NEXT_UP = "com.krittish.dailytracker.NEXT_UP";
    public static final String ACTION_DONE = "com.krittish.dailytracker.DONE";
    public static final String ACTION_SNOOZE = "com.krittish.dailytracker.SNOOZE";
    public static final String ACTION_FOCUS_END = "com.krittish.dailytracker.FOCUS_END";
    public static final String ACTION_START_BREAK = "com.krittish.dailytracker.START_BREAK";
    public static final String ACTION_CONFIRM_ENTRY = "com.krittish.dailytracker.CONFIRM_ENTRY";

    // Intent extras
    public static final String EXTRA_KIND = "kind";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_EVENT_TIME = "event_time";
    public static final String EXTRA_OPEN = "open";

    public static final String KIND_HABIT = "habit";
    public static final String KIND_TASK = "task";

    // Where a notification tap should open the app
    public static final String OPEN_HOME = "home";
    public static final String OPEN_HABITS = "habits";
    public static final String OPEN_MONEY = "money";
    public static final String OPEN_FOCUS = "focus";
    public static final String OPEN_STATS = "stats";
    public static final String OPEN_REVIEW = "review";
    public static final String OPEN_ADD_EXPENSE = "add_expense";
    public static final String OPEN_TASKS = "tasks";

    // Fixed notification ids
    public static final int NOTIF_FOCUS_ONGOING = 10;
    public static final int NOTIF_FOCUS_DONE = 11;
    public static final int NOTIF_MORNING = 12;
    public static final int NOTIF_EVENING = 13;
    public static final int NOTIF_WEEKLY = 14;
    public static final int NOTIF_STREAK = 15;
    public static final int NOTIF_BUDGET = 16;
    public static final int NOTIF_DETECTED = 17;
    public static final int NOTIF_UP_NEXT_CARD = 18;
    public static final int NOTIF_HABIT_BASE = 1_000_000;
    public static final int NOTIF_TASK_BASE = 2_000_000;

    public static final int REQ_FOCUS_END = 1;
    public static final int REQ_FOCUS_BREAK = 2;
    public static final int REQ_CONFIRM = 3;
}
