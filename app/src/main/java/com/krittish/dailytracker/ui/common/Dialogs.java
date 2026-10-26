package com.krittish.dailytracker.ui.common;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.krittish.dailytracker.notifications.NotificationScheduler;
import com.krittish.dailytracker.util.Callback;
import com.krittish.dailytracker.util.DateUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Small helpers so screens stay short. */
public final class Dialogs {
    private Dialogs() {}

    private static boolean askedExactAlarm;

    public static void promptText(Context ctx, String title, String hint, String initial, int inputType, Callback<String> cb) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setInputType(inputType);
        if (initial != null) et.setText(initial);
        et.setSelection(et.getText().length());
        FrameLayout box = new FrameLayout(ctx);
        int pad = (int) (20 * ctx.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(et, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new MaterialAlertDialogBuilder(ctx).setTitle(title).setView(box)
                .setPositiveButton("OK", (d, w) -> cb.on(et.getText().toString().trim()))
                .setNegativeButton("Cancel", null).show();
    }

    public static void promptNumber(Context ctx, String title, String hint, long initial, Callback<Long> cb) {
        promptText(ctx, title, hint, initial > 0 ? String.valueOf(initial) : "", InputType.TYPE_CLASS_NUMBER, s -> {
            try { cb.on(Long.parseLong(s)); } catch (NumberFormatException ignored) { /* ignore bad input */ }
        });
    }

    public static void pickTime(Context ctx, int initialMinute, Callback<Integer> cb) {
        int m = initialMinute < 0 ? 9 * 60 : initialMinute;
        new TimePickerDialog(ctx, (view, h, min) -> cb.on(h * 60 + min), m / 60, m % 60, false).show();
    }

    /** Date, then time. Returns epoch millis. */
    public static void pickDateTime(Context ctx, long initialMillis, Callback<Long> cb) {
        LocalDate d = LocalDate.ofEpochDay(DateUtils.epochDayOf(initialMillis));
        new DatePickerDialog(ctx, (v, y, mo, day) -> {
            int m = DateUtils.minuteOfDay(initialMillis);
            new TimePickerDialog(ctx, (tv, h, min) -> {
                long ms = LocalDateTime.of(y, mo + 1, day, h, min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                cb.on(ms);
            }, m / 60, m % 60, false).show();
        }, d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
    }

    public static void info(Context ctx, String title, String message) {
        new MaterialAlertDialogBuilder(ctx).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }

    /** Android 12+: exact alarms may be off. Ask once per app run, never block the user. */
    public static void askExactAlarmIfNeeded(Context ctx) {
        if (askedExactAlarm || NotificationScheduler.canScheduleExact(ctx)) return;
        askedExactAlarm = true;
        new MaterialAlertDialogBuilder(ctx).setTitle("Allow exact alarms?")
                .setMessage("For reminders and the focus timer to arrive exactly on time, allow \"Alarms & reminders\" for this app.")
                .setPositiveButton("Open settings", (d, w) -> {
                    if (Build.VERSION.SDK_INT >= 31) {
                        ctx.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + ctx.getPackageName())));
                    }
                })
                .setNegativeButton("Not now", null).show();
    }
}
