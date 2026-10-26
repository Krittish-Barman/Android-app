package com.krittish.dailytracker.ui.habits;

import android.content.res.ColorStateList;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.Habit;
import com.krittish.dailytracker.domain.HabitRow;
import com.krittish.dailytracker.util.DateUtils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Fills item_habit.xml. Used by the Habits tab and by Home. */
public final class HabitItemBinder {
    private HabitItemBinder() {}

    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    public static void bind(View v, HabitRow r, BiConsumer<HabitRow, Boolean> onToggle, Consumer<HabitRow> onOpen) {
        Habit h = r.habit;
        ((TextView) v.findViewById(R.id.tvName)).setText(h.name);
        ((TextView) v.findViewById(R.id.tvSub)).setText(subtitle(h));
        TextView streak = v.findViewById(R.id.tvStreak);
        if (r.streak > 0) {
            streak.setVisibility(View.VISIBLE);
            streak.setText(r.streak + (r.streak == 1 ? " day" : " days"));
        } else {
            streak.setVisibility(View.GONE);
        }
        v.findViewById(R.id.dot).setBackgroundTintList(ColorStateList.valueOf(h.color));

        MaterialCheckBox cb = v.findViewById(R.id.cbDone);
        cb.setChecked(r.doneToday);
        cb.setOnClickListener(x -> {
            boolean done = cb.isChecked();
            x.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);       // light vibration on tick
            onToggle.accept(r, done);
            Snackbar.make(v, done ? "Marked done" : "Unmarked", Snackbar.LENGTH_LONG)
                    .setAction("Undo", y -> onToggle.accept(r, !done)).show();
        });
        v.setOnClickListener(x -> onOpen.accept(r));
    }

    public static String subtitle(Habit h) {
        String schedule;
        if ("WEEKDAYS".equals(h.scheduleType)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 7; i++) {
                if ((h.weekdaysMask & (1 << i)) != 0) sb.append(sb.length() > 0 ? " " : "").append(DAYS[i]);
            }
            schedule = sb.length() == 0 ? "No days" : sb.toString();
        } else if ("TIMES_PER_WEEK".equals(h.scheduleType)) {
            schedule = h.timesPerWeek + "× per week";
        } else {
            schedule = "Daily";
        }
        StringBuilder out = new StringBuilder(schedule);
        if (h.targetValue > 0) {
            out.append(" · ").append(h.targetValue);
            if (h.targetUnit != null && !h.targetUnit.isEmpty()) out.append(' ').append(h.targetUnit);
        }
        if (h.reminderTime >= 0) out.append(" · ").append(DateUtils.formatTime(h.reminderTime));
        return out.toString();
    }
}
