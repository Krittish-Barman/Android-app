package com.krittish.dailytracker.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.krittish.dailytracker.focus.FocusTimerManager;
import com.krittish.dailytracker.util.AppExecutors;

/** After a restart, a time change or an app update, set all alarms again. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        final PendingResult result = goAsync();
        AppExecutors.DISK_IO.execute(() -> {
            try {
                NotificationHelper.createChannels(app);
                NotificationScheduler.refreshNow(app);
                NotificationScheduler.scheduleWork(app, false);
                FocusTimerManager.get(app).rearm();
            } catch (Exception ignored) {
                // never crash a receiver
            } finally {
                result.finish();
            }
        });
    }
}
