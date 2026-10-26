package com.krittish.dailytracker;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.db.SeedData;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.focus.FocusTimerManager;
import com.krittish.dailytracker.notifications.NotificationHelper;
import com.krittish.dailytracker.notifications.NotificationScheduler;
import com.krittish.dailytracker.util.AppExecutors;

public class DailyTrackerApp extends Application {

    private static boolean unlocked;
    private static long backgroundAt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable interruptRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        SettingsRepository s = SettingsRepository.get(this);
        AppCompatDelegate.setDefaultNightMode(nightMode(s.theme()));
        NotificationHelper.createChannels(this);

        AppExecutors.DISK_IO.execute(() -> {
            SeedData.run(AppDatabase.get(this));
            NotificationScheduler.refreshNow(this);
            NotificationScheduler.scheduleWork(this, false);
        });

        // Knows when the whole app goes to the background: used by the lock and by the "interrupted" rule.
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                if (interruptRunnable != null) {
                    handler.removeCallbacks(interruptRunnable);
                    interruptRunnable = null;
                }
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                backgroundAt = System.currentTimeMillis();
                FocusTimerManager fm = FocusTimerManager.get(DailyTrackerApp.this);
                boolean rule = SettingsRepository.get(DailyTrackerApp.this).interruptedRule();
                if (rule && fm.isActive() && !fm.isPaused() && FocusTimerManager.FOCUS.equals(fm.mode())) {
                    final long focusedAtLeave = fm.plannedMs() - fm.remainingMs();
                    interruptRunnable = () -> AppExecutors.DISK_IO.execute(
                            () -> FocusTimerManager.get(DailyTrackerApp.this).interruptAt(focusedAtLeave));
                    handler.postDelayed(interruptRunnable, 10_000);       // more than 10 seconds away
                }
            }
        });
    }

    public static int nightMode(String theme) {
        if ("light".equals(theme)) return AppCompatDelegate.MODE_NIGHT_NO;
        if ("dark".equals(theme)) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    // ---- fingerprint lock: ask at launch and after 1 minute in the background ----
    public static boolean needsLock(Context c) {
        if (!SettingsRepository.get(c).lockOn()) return false;
        return !unlocked || (backgroundAt > 0 && System.currentTimeMillis() - backgroundAt > 60_000);
    }

    public static void markUnlocked() {
        unlocked = true;
        backgroundAt = 0;
    }
}
