package com.krittish.dailytracker.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.db.Task;
import com.krittish.dailytracker.notifications.NotificationScheduler;
import com.krittish.dailytracker.util.AppExecutors;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.DateUtils;

import java.util.List;

public class TaskRepository {
    private final Context app;
    private final AppDatabase db;

    public TaskRepository(Context context) {
        this.app = context.getApplicationContext();
        this.db = AppDatabase.get(app);
    }

    public LiveData<List<Task>> observeAll() { return db.taskDao().observeAll(); }

    public void add(Task t, boolean pinToday, Runnable onPinLimit) {
        AppExecutors.DISK_IO.execute(() -> {
            long today = DateUtils.today();
            if (pinToday) {
                if (db.taskDao().pinnedCountNow(today) < 3) t.pinnedDate = today;
                else if (onPinLimit != null) AppExecutors.main(onPinLimit);
            }
            db.taskDao().insert(t);
            NotificationScheduler.refreshNow(app);
        });
    }

    public void setDone(Task t, boolean done) {
        AppExecutors.DISK_IO.execute(() -> setDoneSync(t.id, done));
    }

    /** Background thread only (used by notification buttons too). */
    public void setDoneSync(long id, boolean done) {
        Task t = db.taskDao().getById(id);
        if (t == null) return;
        t.done = done;
        t.doneAt = done ? System.currentTimeMillis() : 0;
        db.taskDao().update(t);
        if (done) NotificationScheduler.cancelNotification(app, Constants.KIND_TASK, id);
        NotificationScheduler.refreshNow(app);
    }

    /** Pins up to 3 tasks for today. */
    public void togglePin(Task t, Runnable onLimit) {
        AppExecutors.DISK_IO.execute(() -> {
            long today = DateUtils.today();
            Task cur = db.taskDao().getById(t.id);
            if (cur == null) return;
            if (cur.pinnedDate == today) {
                cur.pinnedDate = -1;
            } else {
                if (db.taskDao().pinnedCountNow(today) >= 3) {
                    if (onLimit != null) AppExecutors.main(onLimit);
                    return;
                }
                cur.pinnedDate = today;
            }
            db.taskDao().update(cur);
        });
    }

    public void delete(Task t) {
        AppExecutors.DISK_IO.execute(() -> {
            db.taskDao().delete(t);
            NotificationScheduler.cancelNotification(app, Constants.KIND_TASK, t.id);
            NotificationScheduler.refreshNow(app);
        });
    }

    /** Undo for swipe-to-delete: same id goes back in. */
    public void restore(Task t) {
        AppExecutors.DISK_IO.execute(() -> {
            db.taskDao().insert(t);
            NotificationScheduler.refreshNow(app);
        });
    }
}
