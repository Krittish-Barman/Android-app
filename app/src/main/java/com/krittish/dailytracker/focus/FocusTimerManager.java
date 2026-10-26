package com.krittish.dailytracker.focus;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.db.FocusSession;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.notifications.AlarmReceiver;
import com.krittish.dailytracker.notifications.NotificationHelper;
import com.krittish.dailytracker.notifications.NotificationScheduler;
import com.krittish.dailytracker.util.Constants;

/**
 * Timer that stays correct with the screen off or the app closed:
 * we save the END TIME, set an alarm for it, and show an ongoing notification with a countdown
 * chronometer. The real remaining time always comes from the saved end time.
 *
 * Threading: start/pause/resume only touch SharedPreferences and are fine on the main thread.
 * stop(), interruptAt(), completeIfDue() write to the database, so call them on AppExecutors.DISK_IO.
 */
public final class FocusTimerManager {
    public static final String FOCUS = "FOCUS";
    public static final String BREAK = "BREAK";

    private static FocusTimerManager instance;
    private final Context ctx;
    private final SharedPreferences p;

    public static synchronized FocusTimerManager get(Context c) {
        if (instance == null) instance = new FocusTimerManager(c.getApplicationContext());
        return instance;
    }

    private FocusTimerManager(Context c) {
        ctx = c;
        p = c.getSharedPreferences("focus_timer", Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- state
    public boolean isActive() { return p.getBoolean("active", false); }
    public boolean isPaused() { return p.getBoolean("paused", false); }
    public String mode() { return p.getString("mode", FOCUS); }
    public long plannedMs() { return p.getLong("planned", 0); }
    public String subjectName() { return p.getString("subjectName", "Focus"); }

    public long remainingMs() {
        if (!isActive()) return 0;
        if (isPaused()) return p.getLong("remaining", 0);
        return Math.max(0, p.getLong("endAt", 0) - System.currentTimeMillis());
    }

    // ---------------------------------------------------------------- controls
    public synchronized void start(String mode, long subjectId, String subjectName, int minutes, int breakMinutes) {
        long now = System.currentTimeMillis();
        long planned = minutes * 60_000L;
        SharedPreferences.Editor e = p.edit()
                .putBoolean("active", true).putBoolean("paused", false)
                .putString("mode", mode).putLong("subjectId", subjectId).putString("subjectName", subjectName)
                .putLong("planned", planned).putLong("startAt", now).putLong("endAt", now + planned)
                .putLong("remaining", planned).putInt("breakMin", breakMinutes);
        if (FOCUS.equals(mode)) {
            e.putLong("lastSubjectId", subjectId).putString("lastSubjectName", subjectName).putInt("lastBreakMin", breakMinutes);
        }
        e.apply();
        armAlarm(now + planned);
        showOngoing(now + planned, false);
    }

    public synchronized void pause() {
        if (!isActive() || isPaused()) return;
        long rem = remainingMs();
        p.edit().putBoolean("paused", true).putLong("remaining", rem).apply();
        NotificationScheduler.cancelAlarmIntent(ctx, alarmPending(PendingIntent.FLAG_NO_CREATE));
        showOngoing(0, true);
    }

    public synchronized void resume() {
        if (!isActive() || !isPaused()) return;
        long end = System.currentTimeMillis() + p.getLong("remaining", 0);
        p.edit().putBoolean("paused", false).putLong("endAt", end).apply();
        armAlarm(end);
        showOngoing(end, false);
    }

    /** Stop early. The focused minutes are saved as an INTERRUPTED session. Background thread. */
    public synchronized void stop() {
        if (!isActive()) return;
        long focused = FOCUS.equals(mode()) ? plannedMs() - remainingMs() : 0;
        finishState();
        if (focused >= 60_000) saveSession("INTERRUPTED", focused);
    }

    /** "Leave the app for more than 10 seconds = interrupted". Background thread. */
    public synchronized void interruptAt(long focusedMsAtLeave) {
        if (!isActive() || isPaused() || !FOCUS.equals(mode())) return;
        finishState();
        if (focusedMsAtLeave >= 60_000) saveSession("INTERRUPTED", focusedMsAtLeave);
        NotificationHelper.notify(ctx, Constants.NOTIF_FOCUS_DONE,
                NotificationHelper.builder(ctx, Constants.CH_REMINDERS, "Session interrupted",
                        "You left the app. " + (focusedMsAtLeave / 60_000) + " min of focus were saved.")
                        .setContentIntent(NotificationHelper.openApp(ctx, 70, Constants.OPEN_FOCUS)), false);
    }

    /** Called by the alarm and by the screen when the countdown reaches 0. Safe to call many times. */
    public synchronized boolean completeIfDue() {
        if (!isActive() || isPaused()) return false;
        if (System.currentTimeMillis() < p.getLong("endAt", 0) - 1500) return false;
        String mode = mode();
        long planned = plannedMs();
        String subject = subjectName();
        int breakMin = p.getInt("breakMin", 5);
        finishState();

        if (FOCUS.equals(mode)) {
            saveSession("COMPLETED", planned);
            if (SettingsRepository.get(ctx).notifOn(Constants.NT_FOCUS)) {
                NotificationCompat.Builder b = NotificationHelper.builder(ctx, Constants.CH_REMINDERS, "Focus session done",
                        "Take a " + breakMin + " min break. Next: " + subject)
                        .setContentIntent(NotificationHelper.openApp(ctx, 71, Constants.OPEN_FOCUS));
                if (breakMin > 0) {
                    Intent i = new Intent(ctx, AlarmReceiver.class).setAction(Constants.ACTION_START_BREAK);
                    b.addAction(0, "Start break", PendingIntent.getBroadcast(ctx, Constants.REQ_FOCUS_BREAK, i,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                }
                NotificationHelper.notify(ctx, Constants.NOTIF_FOCUS_DONE, b, false);
            }
        } else {
            NotificationHelper.notify(ctx, Constants.NOTIF_FOCUS_DONE,
                    NotificationHelper.builder(ctx, Constants.CH_REMINDERS, "Break over", "Ready for the next session?")
                            .setContentIntent(NotificationHelper.openApp(ctx, 72, Constants.OPEN_FOCUS)), false);
        }
        return true;
    }

    /** "Start break" button on the "Focus session done" notification. */
    public synchronized void startBreakAfterFocus() {
        int breakMin = p.getInt("lastBreakMin", 5);
        if (breakMin <= 0 || isActive()) return;
        start(BREAK, p.getLong("lastSubjectId", 0), p.getString("lastSubjectName", "Focus"), breakMin, breakMin);
    }

    /** After a phone restart: put the alarm and notification back. Background thread. */
    public synchronized void rearm() {
        if (!isActive() || isPaused()) return;
        long end = p.getLong("endAt", 0);
        if (end <= System.currentTimeMillis()) {
            completeIfDue();
        } else {
            armAlarm(end);
            showOngoing(end, false);
        }
    }

    // ---------------------------------------------------------------- internals
    private void finishState() {
        NotificationScheduler.cancelAlarmIntent(ctx, alarmPending(PendingIntent.FLAG_NO_CREATE));
        NotificationManagerCompat.from(ctx).cancel(Constants.NOTIF_FOCUS_ONGOING);
        p.edit().putBoolean("active", false).putBoolean("paused", false).apply();
    }

    private void saveSession(String status, long focusedMs) {
        FocusSession s = new FocusSession();
        s.subjectId = p.getLong("subjectId", 0);
        s.subjectName = subjectName();
        s.plannedMinutes = (int) (plannedMs() / 60_000);
        s.focusedMinutes = (int) (focusedMs / 60_000);
        s.startTime = p.getLong("startAt", System.currentTimeMillis());
        s.endTime = System.currentTimeMillis();
        s.status = status;
        AppDatabase.get(ctx).focusDao().insert(s);
    }

    private PendingIntent alarmPending(int flag) {
        Intent i = new Intent(ctx, AlarmReceiver.class).setAction(Constants.ACTION_FOCUS_END);
        return PendingIntent.getBroadcast(ctx, Constants.REQ_FOCUS_END, i, flag | PendingIntent.FLAG_IMMUTABLE);
    }

    private void armAlarm(long endAt) {
        NotificationScheduler.setAlarm(ctx, endAt, alarmPending(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void showOngoing(long endAt, boolean paused) {
        String title = (BREAK.equals(mode()) ? "Break" : "Focus · " + subjectName());
        NotificationCompat.Builder b = NotificationHelper.builder(ctx, Constants.CH_FOCUS, title,
                paused ? "Paused" : "Stay on it")
                .setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true)
                .setContentIntent(NotificationHelper.openApp(ctx, 73, Constants.OPEN_FOCUS));
        if (!paused) {
            b.setUsesChronometer(true).setChronometerCountDown(true).setWhen(endAt).setShowWhen(true);
        } else {
            b.setContentText("Paused · " + (p.getLong("remaining", 0) / 60_000) + " min left");
        }
        NotificationHelper.notify(ctx, Constants.NOTIF_FOCUS_ONGOING, b, false);
    }
}
