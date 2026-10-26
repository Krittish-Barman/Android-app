package com.krittish.dailytracker.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.db.Habit;
import com.krittish.dailytracker.data.db.HabitLog;
import com.krittish.dailytracker.data.db.Task;
import com.krittish.dailytracker.data.repo.HabitRepository;
import com.krittish.dailytracker.data.repo.MoneyRepository;
import com.krittish.dailytracker.data.repo.TaskRepository;
import com.krittish.dailytracker.focus.FocusTimerManager;
import com.krittish.dailytracker.util.AppExecutors;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.DateUtils;

import androidx.core.app.NotificationManagerCompat;

/** Receives alarms and notification buttons. DB work runs on the background executor. */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) return;
        final Context app = context.getApplicationContext();
        final PendingResult result = goAsync();
        AppExecutors.DISK_IO.execute(() -> {
            try {
                handle(app, intent, action);
            } catch (Exception ignored) {
                // never crash a receiver
            } finally {
                result.finish();
            }
        });
    }

    private void handle(Context ctx, Intent in, String action) {
        String kind = in.getStringExtra(Constants.EXTRA_KIND);
        long id = in.getLongExtra(Constants.EXTRA_ID, 0);
        long eventTime = in.getLongExtra(Constants.EXTRA_EVENT_TIME, 0);
        String title = in.getStringExtra(Constants.EXTRA_TITLE);
        AppDatabase db = AppDatabase.get(ctx);

        switch (action) {
            case Constants.ACTION_NEXT_UP:
                if (Constants.KIND_HABIT.equals(kind)) {
                    Habit h = db.habitDao().getById(id);
                    if (h == null || h.archived) return;
                    HabitLog log = db.habitLogDao().get(id, DateUtils.today());
                    NotificationScheduler.scheduleHabit(ctx, db, h);          // arm the next occurrence
                    if (log != null) return;                                  // already done today
                    NotificationScheduler.showNextUp(ctx, kind, id, h.name, eventTime);
                } else {
                    Task t = db.taskDao().getById(id);
                    if (t == null || t.done) return;
                    NotificationScheduler.showNextUp(ctx, kind, id, t.title, t.dueAt > 0 ? t.dueAt : eventTime);
                }
                break;

            case Constants.ACTION_DONE:
                if (Constants.KIND_HABIT.equals(kind)) {
                    new HabitRepository(ctx).setDoneSync(id, DateUtils.today(), true);
                } else {
                    new TaskRepository(ctx).setDoneSync(id, true);
                }
                NotificationManagerCompat.from(ctx).cancel(NotificationScheduler.notifId(kind, id));
                break;

            case Constants.ACTION_SNOOZE:
                NotificationManagerCompat.from(ctx).cancel(NotificationScheduler.notifId(kind, id));
                NotificationScheduler.snooze(ctx, kind, id, title, eventTime, 10);
                break;

            case Constants.ACTION_FOCUS_END:
                FocusTimerManager.get(ctx).completeIfDue();
                break;

            case Constants.ACTION_START_BREAK:
                NotificationManagerCompat.from(ctx).cancel(Constants.NOTIF_FOCUS_DONE);
                FocusTimerManager.get(ctx).startBreakAfterFocus();
                break;

            case Constants.ACTION_CONFIRM_ENTRY:
                new MoneyRepository(ctx).confirmSync(id);
                NotificationManagerCompat.from(ctx).cancel(Constants.NOTIF_DETECTED);
                break;

            default:
                break;
        }
    }
}
