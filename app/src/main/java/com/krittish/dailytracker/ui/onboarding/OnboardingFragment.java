package com.krittish.dailytracker.ui.onboarding;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.Habit;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.data.repo.HabitRepository;
import com.krittish.dailytracker.databinding.FragmentOnboardingBinding;
import com.krittish.dailytracker.util.MoneyFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** First launch: 3 short steps. Pick habits, set a budget, allow notifications. */
public class OnboardingFragment extends Fragment {

    private static class Suggestion {
        final String name, schedule, unit;
        final int mask, target, reminder, color;
        final boolean checked;

        Suggestion(String name, String schedule, int mask, int target, String unit, int reminder, int color, boolean checked) {
            this.name = name;
            this.schedule = schedule;
            this.mask = mask;
            this.target = target;
            this.unit = unit;
            this.reminder = reminder;
            this.color = color;
            this.checked = checked;
        }
    }

    private static final Suggestion[] SUGGESTIONS = {
            new Suggestion("Gym", "WEEKDAYS", 1 | 4 | 16, 0, null, 18 * 60, 0xFF0F766E, true),
            new Suggestion("English speaking", "DAILY", 0, 20, "min", -1, 0xFF6366F1, true),
            new Suggestion("DSA practice", "DAILY", 0, 1, "problem", -1, 0xFFF97316, true),
            new Suggestion("Sleep before 12:30 AM", "DAILY", 0, 0, null, 23 * 60 + 30, 0xFF8B5CF6, true),
            new Suggestion("Read 10 pages", "DAILY", 0, 10, "pages", -1, 0xFF16A34A, false),
            new Suggestion("Drink 3 L water", "DAILY", 0, 3, "L", -1, 0xFF0EA5E9, false)
    };

    private FragmentOnboardingBinding b;
    private int step = 0;
    private final List<MaterialCheckBox> checks = new ArrayList<>();

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    private final ActivityResultLauncher<String[]> smsPermission =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (Map<String, Boolean> r) -> {
                boolean ok = Boolean.TRUE.equals(r.get(Manifest.permission.RECEIVE_SMS))
                        && Boolean.TRUE.equals(r.get(Manifest.permission.READ_SMS));
                SettingsRepository.get(requireContext()).setAutoDetectSms(ok);
                if (b != null) b.swAutoDetect.setChecked(ok);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentOnboardingBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        for (Suggestion s : SUGGESTIONS) {
            MaterialCheckBox cb = new MaterialCheckBox(requireContext());
            cb.setText(s.name);
            cb.setChecked(s.checked);
            b.llHabitChoices.addView(cb);
            checks.add(cb);
        }
        b.btnAllowNotif.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        });
        b.swAutoDetect.setOnCheckedChangeListener((btn, on) -> {
            if (on && !SettingsRepository.get(requireContext()).autoDetectSms()) {
                btn.setChecked(false);
                smsPermission.launch(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS});
            } else if (!on) {
                SettingsRepository.get(requireContext()).setAutoDetectSms(false);
            }
        });
        b.btnBack.setOnClickListener(v -> {
            if (step > 0) { step--; showStep(); }
        });
        b.btnNext.setOnClickListener(v -> {
            if (step < 2) { step++; showStep(); }
            else finish();
        });
        showStep();
    }

    private void showStep() {
        b.stepHabits.setVisibility(step == 0 ? View.VISIBLE : View.GONE);
        b.stepBudget.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        b.stepPermissions.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        b.tvStep.setText("Step " + (step + 1) + " of 3");
        b.btnBack.setVisibility(step == 0 ? View.INVISIBLE : View.VISIBLE);
        b.btnNext.setText(step == 2 ? "Finish" : "Next");
        if (step == 0) {
            b.tvHeading.setText("Pick your habits");
            b.tvBody.setText("A short list is easier to keep than a long one. You can change everything later.");
        } else if (step == 1) {
            b.tvHeading.setText("Set a monthly budget");
            b.tvBody.setText("You will get an alert at 80% and 100%.");
        } else {
            b.tvHeading.setText("Stay on track");
            b.tvBody.setText("Notifications tell you what is next, right on time.");
        }
    }

    private void finish() {
        SettingsRepository s = SettingsRepository.get(requireContext());
        HabitRepository repo = new HabitRepository(requireContext());
        for (int i = 0; i < SUGGESTIONS.length; i++) {
            if (!checks.get(i).isChecked()) continue;
            Suggestion sg = SUGGESTIONS[i];
            Habit h = new Habit();
            h.name = sg.name;
            h.scheduleType = sg.schedule;
            h.weekdaysMask = sg.mask;
            h.targetValue = sg.target;
            h.targetUnit = sg.unit;
            h.reminderTime = sg.reminder;
            h.color = sg.color;
            repo.save(h);
        }
        long paise = MoneyFormat.parseToPaise(String.valueOf(b.etBudget.getText()));
        if (paise > 0) s.setMonthlyBudgetPaise(paise);
        s.setOnboardingDone(true);
        NavHostFragment.findNavController(this).navigate(R.id.action_onboarding_to_home);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
