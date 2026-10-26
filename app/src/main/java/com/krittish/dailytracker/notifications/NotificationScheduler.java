package com.krittish.dailytracker.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.db.Habit;
import com.krittish.dailytracker.data.db.HabitLog;
import com.krittish.dailytracker.data.db.Task;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.domain.HabitRow;
import com.krittish.dailytracker.domain.StreakCalculator;
import com.krittish.dailytracker.domain.UpNextProvider;
import com.krittish.dailytracker.util.AppExecutors;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.DateUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The ONE class that owns all alarms and background work.
 * - AlarmManager (exact): "Next up" before habits/tasks, "Focus done".
 * - WorkManager (daily/weekly): morning plan, evening check, streak at risk, weekly summary.
 */
public final class NotificationScheduler {
    private NotificationScheduler() {}

    /** Unique PendingIntent request code per item. variant: 0 alarm, 1 Done button, 2 Snooze button, 3 snoozed alarm. */
    public static int req(String kind, long id, int variant) {
        int base = Constants.KIND_HABIT.equals(kind) ? 10_000_000 : 20_000_000;
        return base + ((int) id) * 4 + variant;
    }

    public static int notifId(String kind, long id) {
        return (Constants.KIND_HABIT.equals(kind) ? Constants.NOTIF_HABIT_BASE : Constants.NOTIF_TASK_BASE) + (int) id;
    }

    // ------------------------------------------------------------------ refresh

    /** Safe from the main thread. */
    public static void refreshAsync(Context ctx) {
        Context app = ctx.getApplicationContext();
        AppExecutors.DISK_IO.execute(() -> refreshNow(app));
    }

    /** Re-arms every alarm and the "up next" card. Background thread only. */
    public static void refreshNow(Context ctx) {
        AppDatabase db = AppDatabase.get(ctx);
        for (Habit h : db.habitDao().allNow()) {
            if (h.archived || h.reminderTime < 0) cancelAlarm(ctx, Constants.KIND_HABIT, h.id);
            else scheduleHabit(ctx, db, h);
        }
        long now = System.currentTimeMillis();
        for (Task t : db.taskDao().allNow()) {
            if (!t.done && t.dueAt > now) scheduleTask(ctx, t);
            else cancelAlarm(ctx, Constants.KIND_TASK, t.id);
        }
        updateUpNextCard(ctx, db);
    }

    // ------------------------------------------------------------------ alarms

    public static void scheduleHabit(Context ctx, AppDatabase db, Habit h) {
        if (h.archived || h.reminderTime < 0) return;
        int lead = SettingsRepository.get(ctx).leadMinutes();
        long now = System.currentTimeMillis();
        long today = DateUtils.today();
        for (int i = 0; i < 8; i++) {
            long day = today + i;
            if (!StreakCalculator.isScheduled(h.scheduleType, h.weekdaysMask, day)) continue;
            if (i == 0) {
                HabitLog l = db.habitLogDao().get(h.id, today);
                if (l != null) continue;                      // already done or skipped today
            }
            long event = DateUtils.millisAt(day, h.reminderTime);
            long fire = event - lead * 60_000L;
            if (fire > now + 1000) {
                setAlarm(ctx, fire, alarmIntent(ctx, Constants.ACTION_NEXT_UP, Constants.KIND_HABIT, h.id, h.name, event, 0));
                return;
            }
        }
    }

    public static void scheduleTask(Context ctx, Task t) {
        int lead = SettingsRepository.get(ctx).leadMinutes();
        long fire = t.dueAt - lead * 60_000L;
        if (fire <= System.currentTimeMillis() + 1000) fire = t.dueAt;
        if (fire > System.currentTimeMillis() + 1000) {
            setAlarm(ctx, fire, alarmIntent(ctx, Constants.ACTION_NEXT_UP, Constants.KIND_TASK, t.id, t.title, t.dueAt, 0));
        }
    }

    public static void snooze(Context ctx, String kind, long id, String title, long eventTime, int minutes) {
        long at = System.currentTimeMillis() + minutes * 60_000L;
        setAlarm(ctx, at, alarmIntent(ctx, Constants.ACTION_NEXT_UP, kind, id, title, eventTime, 3));
    }

    private static PendingIntent alarmIntent(Context ctx, String action, String kind, long id,
                                             String title, long eventTime, int variant) {
        Intent i = new Intent(ctx, AlarmReceiver.class).setAction(action)
                .putExtra(Constants.EXTRA_KIND, kind)
                .putExtra(Constants.EXTRA_ID, id)
                .putExtra(Constants.EXTRA_TITLE, title)
                .putExtra(Constants.EXTRA_EVENT_TIME, eventTime);
        return PendingIntent.getBroadcast(ctx, req(kind, id, variant), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void cancelAlarm(Context ctx, String kind, long id) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, AlarmReceiver.class).setAction(Constants.ACTION_NEXT_UP);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, req(kind, id, 0), i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    public static void cancelNotification(Context ctx, String kind, long id) {
        NotificationManagerCompat.from(ctx).cancel(notifId(kind, id));
    }

    public static boolean canScheduleExact(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        return am != null && (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
    }

    /** Exact alarm when allowed, otherwise an inexact one (a few minutes late is fine). */
    public static void setAlarm(Context ctx, long atMillis, PendingIntent pi) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            if (canScheduleExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi);
        }
    }

    public static void cancelAlarmIntent(Context ctx, PendingIntent pi) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null && pi != null) am.cancel(pi);
    }

    // ------------------------------------------------------------------ notifications

    public static PendingIntent donePending(Context ctx, String kind, long id) {
        Intent i = new Intent(ctx, AlarmReceiver.class).setAction(Constants.ACTION_DONE)
                .putExtra(Constants.EXTRA_KIND, kind).putExtra(Constants.EXTRA_ID, id);
        return PendingIntent.getBroadcast(ctx, req(kind, id, 1), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** "Next: Gym at 6:00 PM (in 15 min)" with Done and Snooze 10 min buttons. */
    public static void showNextUp(Context ctx, String kind, long id, String title, long eventTime) {
        if (!SettingsRepository.get(ctx).notifOn(Constants.NT_NEXT_UP)) return;
        long mins = Math.max(0, (eventTime - System.currentTimeMillis() + 30_000) / 60_000);
        String when = mins <= 0 ? "now" : "in " + mins + " min";
        String text = "Next: " + title + " at " + DateUtils.formatMillisTime(eventTime) + " (" + when + ")";

        Intent snooze = new Intent(ctx, AlarmReceiver.class).setAction(Constants.ACTION_SNOOZE)
                .putExtra(Constants.EXTRA_KIND, kind).putExtra(Constants.EXTRA_ID, id)
                .putExtra(Constants.EXTRA_TITLE, title).putExtra(Constants.EXTRA_EVENT_TIME, eventTime);
        PendingIntent snoozePi = PendingIntent.getBroadcast(ctx, req(kind, id, 2), snooze,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String open = Constants.KIND_HABIT.equals(kind) ? Constants.OPEN_HABITS : Constants.OPEN_TASKS;
        NotificationCompat.Builder b = NotificationHelper.builder(ctx, Constants.CH_REMINDERS, "Daily Tracker", text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(NotificationHelper.openApp(ctx, 50, open))
                .addAction(0, "Done", donePending(ctx, kind, id))
                .addAction(0, "Snooze 10 min", snoozePi);
        NotificationHelper.notify(ctx, notifId(kind, id), b, true);
    }

    /** Optional always-visible "Up next" notification (Settings switch). */
    public static void updateUpNextCard(Context ctx, AppDatabase db) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(ctx);
        if (!SettingsRepository.get(ctx).upNextCard()) {
            nm.cancel(Constants.NOTIF_UP_NEXT_CARD);
            return;
        }
        long now = System.currentTimeMillis();
        List<HabitRow> rows = HabitRow.build(db.habitDao().activeNow(), db.habitLogDao().allNow(), DateUtils.today());
        UpNextProvider.UpNext n = UpNextProvider.compute(rows, db.taskDao().allNow(), now);
        if (n == null) {
            nm.cancel(Constants.NOTIF_UP_NEXT_CARD);
            return;
        }
        NotificationCompat.Builder b = NotificationHelper.builder(ctx, Constants.CH_FOCUS, "Up next",
                n.title + " · " + DateUtils.formatMillisTime(n.timeMillis))
                .setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true)
                .setContentIntent(NotificationHelper.openApp(ctx, 51, Constants.OPEN_HOME))
                .addAction(0, "Done", donePending(ctx, n.kind, n.id));
        NotificationHelper.notify(ctx, Constants.NOTIF_UP_NEXT_CARD, b, false);
    }

    // ------------------------------------------------------------------ daily / weekly work

    /** replace = true when the user changed a time in Settings. */
    public static void scheduleWork(Context ctx, boolean replace) {
        SettingsRepository s = SettingsRepository.get(ctx);
        enqueue(ctx, "MORNING", s.morningTime(), 1, -1, replace);
        enqueue(ctx, "EVENING", s.eveningTime(), 1, -1, replace);
        enqueue(ctx, "STREAK", 22 * 60, 1, -1, replace);          // ~2 hours before the day ends
        enqueue(ctx, "WEEKLY", 20 * 60, 7, 6, replace);           // Sunday 8:00 PM
    }

    private static void enqueue(Context ctx, String type, int minuteOfDay, int periodDays, int dayOfWeek, boolean replace) {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(ReminderWorker.class, periodDays, TimeUnit.DAYS)
                .setInitialDelay(delayUntil(minuteOfDay, dayOfWeek), TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder().putString("type", type).build())
                .build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("dt_" + type,
                replace ? ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE : ExistingPeriodicWorkPolicy.KEEP, req);
    }

    /** Milliseconds until the next given time (optionally on a given weekday, 0 = Monday ... 6 = Sunday). */
    static long delayUntil(int minuteOfDay, int dayOfWeek) {
        long now = System.currentTimeMillis();
        long today = DateUtils.today();
        for (int i = 0; i < 8; i++) {
            long day = today + i;
            if (dayOfWeek >= 0 && DateUtils.dayOfWeek(day) != dayOfWeek) continue;
            long t = DateUtils.millisAt(day, minuteOfDay);
            if (t > now) return t - now;
        }
        return 0;
    }
}
