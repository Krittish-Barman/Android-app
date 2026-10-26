package com.krittish.dailytracker.notifications;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.ui.MainActivity;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.DateUtils;

public final class NotificationHelper {
    private NotificationHelper() {}

    /** Call once in Application.onCreate(). Channels can only be changed by the user later. */
    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(Constants.CH_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(Constants.CH_FOCUS, "Focus timer", NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(Constants.CH_INSIGHTS, "Insights", NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(new NotificationChannel(Constants.CH_MONEY, "Money", NotificationManager.IMPORTANCE_DEFAULT));
    }

    public static boolean canNotify(Context ctx) {
        return Build.VERSION.SDK_INT < 33
                || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean inQuietHours(Context ctx) {
        SettingsRepository s = SettingsRepository.get(ctx);
        if (!s.quietOn()) return false;
        int now = DateUtils.minuteOfDay(System.currentTimeMillis());
        int start = s.quietStart();
        int end = s.quietEnd();
        return start <= end ? (now >= start && now < end) : (now >= start || now < end);
    }

    /** Tap on a notification opens MainActivity and jumps to a screen (see MainActivity.handleIntent). */
    public static PendingIntent openApp(Context ctx, int requestCode, String open) {
        Intent i = new Intent(ctx, MainActivity.class)
                .putExtra(Constants.EXTRA_OPEN, open)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(ctx, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static NotificationCompat.Builder builder(Context ctx, String channel, String title, String text) {
        return new NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true);
    }

    /** Shows the notification unless permission is missing or quiet hours apply. */
    @SuppressLint("MissingPermission")
    public static void notify(Context ctx, int id, NotificationCompat.Builder b, boolean respectQuietHours) {
        if (!canNotify(ctx)) return;
        if (respectQuietHours && inQuietHours(ctx)) return;
        Notification n = b.build();
        NotificationManagerCompat.from(ctx).notify(id, n);
    }
}
