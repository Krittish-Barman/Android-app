package com.krittish.dailytracker.ui.habits;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.Habit;
import com.krittish.dailytracker.databinding.FragmentAddEditHabitBinding;
import com.krittish.dailytracker.ui.common.Dialogs;
import com.krittish.dailytracker.util.DateUtils;

public class AddEditHabitFragment extends Fragment {
    private static final int[] COLORS = {0xFF0F766E, 0xFFF97316, 0xFF6366F1, 0xFFEC4899, 0xFF16A34A, 0xFF8B5CF6};
    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private FragmentAddEditHabitBinding b;
    private HabitsViewModel vm;
    private Habit habit = new Habit();
    private int color = COLORS[0];
    private int reminder = -1;
    private String schedule = "DAILY";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentAddEditHabitBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(this).get(HabitsViewModel.class);
        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        // colour chips
        for (int c : COLORS) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, b.cgColor, false);
            chip.setText("    ");
            chip.setChipBackgroundColor(ColorStateList.valueOf(c));
            chip.setCheckedIconTint(ColorStateList.valueOf(Color.WHITE));
            chip.setId(View.generateViewId());
            chip.setTag(c);
            b.cgColor.addView(chip);
        }
        b.cgColor.setOnCheckedStateChangeListener((group, ids) -> {
            if (!ids.isEmpty()) color = (int) group.findViewById(ids.get(0)).getTag();
        });
        selectColor(color);

        // weekday chips
        for (String d : DAYS) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, b.cgDays, false);
            chip.setText(d);
            b.cgDays.addView(chip);
        }

        b.toggleSchedule.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            schedule = id == R.id.btnWeekdays ? "WEEKDAYS" : id == R.id.btnWeekly ? "TIMES_PER_WEEK" : "DAILY";
            updateScheduleViews();
        });
        b.toggleSchedule.check(R.id.btnDaily);

        b.btnReminder.setOnClickListener(v -> Dialogs.pickTime(requireContext(), reminder, m -> {
            reminder = m;
            showReminder();
        }));
        b.btnClearReminder.setOnClickListener(v -> {
            reminder = -1;
            showReminder();
        });
        b.btnSave.setOnClickListener(v -> save());

        long id = requireArguments().getLong("habitId", 0);
        if (id != 0) {
            b.tvScreenTitle.setText("Edit habit");
            vm.load(id, h -> {
                if (h == null || b == null) return;
                habit = h;
                b.etName.setText(h.name);
                color = h.color;
                selectColor(color);
                reminder = h.reminderTime;
                showReminder();
                if (h.targetValue > 0) b.etTarget.setText(String.valueOf(h.targetValue));
                if (h.targetUnit != null) b.etUnit.setText(h.targetUnit);
                b.etTimes.setText(String.valueOf(h.timesPerWeek));
                for (int i = 0; i < 7; i++) ((Chip) b.cgDays.getChildAt(i)).setChecked((h.weekdaysMask & (1 << i)) != 0);
                b.toggleSchedule.check("WEEKDAYS".equals(h.scheduleType) ? R.id.btnWeekdays
                        : "TIMES_PER_WEEK".equals(h.scheduleType) ? R.id.btnWeekly : R.id.btnDaily);
            });
        } else {
            b.etTimes.setText("3");
        }
        showReminder();
    }

    private void selectColor(int c) {
        for (int i = 0; i < b.cgColor.getChildCount(); i++) {
            View chip = b.cgColor.getChildAt(i);
            if (((int) chip.getTag()) == c) {
                b.cgColor.check(chip.getId());
                return;
            }
        }
    }

    private void updateScheduleViews() {
        b.cgDays.setVisibility("WEEKDAYS".equals(schedule) ? View.VISIBLE : View.GONE);
        b.tilTimes.setVisibility("TIMES_PER_WEEK".equals(schedule) ? View.VISIBLE : View.GONE);
    }

    private void showReminder() {
        if (b == null) return;
        b.btnReminder.setText(reminder < 0 ? "No reminder" : "Remind at " + DateUtils.formatTime(reminder));
    }

    private void save() {
        String name = String.valueOf(b.etName.getText()).trim();
        if (name.isEmpty()) {
            b.etName.setError("Give the habit a name");
            return;
        }
        int mask = 0;
        for (int i = 0; i < 7; i++) if (((Chip) b.cgDays.getChildAt(i)).isChecked()) mask |= (1 << i);
        if ("WEEKDAYS".equals(schedule) && mask == 0) {
            Toast.makeText(requireContext(), "Pick at least one day", Toast.LENGTH_SHORT).show();
            return;
        }
        int times = 3;
        try { times = Integer.parseInt(String.valueOf(b.etTimes.getText()).trim()); } catch (NumberFormatException ignored) { /* default */ }
        if (times < 1 || times > 7) times = 3;
        int target = 0;
        try { target = Integer.parseInt(String.valueOf(b.etTarget.getText()).trim()); } catch (NumberFormatException ignored) { /* none */ }

        habit.name = name;
        habit.color = color;
        habit.scheduleType = schedule;
        habit.weekdaysMask = mask;
        habit.timesPerWeek = times;
        habit.targetValue = target;
        String unit = String.valueOf(b.etUnit.getText()).trim();
        habit.targetUnit = target > 0 && !unit.isEmpty() ? unit : null;
        habit.reminderTime = reminder;
        vm.save(habit);
        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
