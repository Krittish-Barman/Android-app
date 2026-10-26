package com.krittish.dailytracker.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.domain.HabitRow;
import com.krittish.dailytracker.domain.UpNextProvider;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.DateUtils;
import com.krittish.dailytracker.util.MoneyFormat;

import java.util.List;

/** Morning plan, evening check, streak at risk, weekly summary. Runs on a WorkManager thread. */
public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String type = getInputData().getString("type");
        if (type == null) return Result.success();
        Context c = getApplicationContext();
        AppDatabase db = AppDatabase.get(c);
        SettingsRepository s = SettingsRepository.get(c);
        long today = DateUtils.today();
        List<HabitRow> rows = HabitRow.build(db.habitDao().activeNow(), db.habitLogDao().allNow(), today);

        switch (type) {
            case "MORNING": {
                db.taskDao().rollOver(today);                 // unfinished pinned tasks move to today
                NotificationScheduler.refreshNow(c);          // also re-arms alarms every day
                if (!s.notifOn(Constants.NT_MORNING)) break;
                int due = 0;
                for (HabitRow r : rows) if (r.dueToday) due++;
                int tasks = db.taskDao().pinnedOpenCountNow(today);
                if (due == 0 && tasks == 0) break;
                UpNextProvider.UpNext n = UpNextProvider.compute(rows, db.taskDao().allNow(), System.currentTimeMillis());
                String text = "Today: " + due + " habits, " + tasks + " tasks."
                        + (n != null ? " First: " + n.title + " at " + DateUtils.formatMillisTime(n.timeMillis) : "");
                NotificationHelper.notify(c, Constants.NOTIF_MORNING,
                        NotificationHelper.builder(c, Constants.CH_INSIGHTS, "Today's plan", text)
                                .setContentIntent(NotificationHelper.openApp(c, 60, Constants.OPEN_HOME)), true);
                break;
            }
            case "EVENING": {
                if (!s.notifOn(Constants.NT_EVENING)) break;
                int left = 0;
                for (HabitRow r : rows) if (r.dueToday && !r.doneToday && !r.skippedToday) left++;
                String text = (left == 0 ? "All habits done." : left + " habits left.") + " Log today's expenses?";
                NotificationHelper.notify(c, Constants.NOTIF_EVENING,
                        NotificationHelper.builder(c, Constants.CH_INSIGHTS, "Evening check", text)
                                .setContentIntent(NotificationHelper.openApp(c, 61, Constants.OPEN_HOME))
                                .addAction(0, "Add expense", NotificationHelper.openApp(c, 62, Constants.OPEN_ADD_EXPENSE))
                                .addAction(0, "Open habits", NotificationHelper.openApp(c, 63, Constants.OPEN_HABITS)), true);
                break;
            }
            case "STREAK": {
                if (!s.notifOn(Constants.NT_STREAK)) break;
                HabitRow best = null;
                for (HabitRow r : rows) {
                    if (r.dueToday && !r.doneToday && !r.skippedToday && r.streak >= 3
                            && (best == null || r.streak > best.streak)) best = r;
                }
                if (best == null) break;
                NotificationHelper.notify(c, Constants.NOTIF_STREAK,
                        NotificationHelper.builder(c, Constants.CH_REMINDERS, "Streak at risk",
                                "2 hours left to keep your " + best.streak + "-day streak: " + best.habit.name)
                                .setContentIntent(NotificationHelper.openApp(c, 64, Constants.OPEN_HABITS))
                                .addAction(0, "Done", NotificationScheduler.donePending(c, Constants.KIND_HABIT, best.habit.id)), true);
                break;
            }
            case "WEEKLY": {
                if (!s.notifOn(Constants.NT_WEEKLY)) break;
                long start = DateUtils.weekStart(today);
                long from = DateUtils.startOfDayMillis(start);
                long to = DateUtils.startOfDayMillis(start + 7);
                int study = db.focusDao().minutesBetweenNow(from, to);
                int ticks = db.habitLogDao().doneCountBetween(start, start + 6);
                long spent = db.moneyDao().expenseSumNow(from, to);
                String text = "This week: " + DateUtils.formatMinutes(study) + " study, " + ticks
                        + " habit ticks, " + MoneyFormat.format(spent) + " spent";
                NotificationHelper.notify(c, Constants.NOTIF_WEEKLY,
                        NotificationHelper.builder(c, Constants.CH_INSIGHTS, "Weekly summary", text)
                                .setContentIntent(NotificationHelper.openApp(c, 65, Constants.OPEN_STATS)), true);
                break;
            }
            default:
                break;
        }
        return Result.success();
    }
}
