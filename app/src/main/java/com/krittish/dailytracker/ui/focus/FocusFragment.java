package com.krittish.dailytracker.ui.focus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.FocusSession;
import com.krittish.dailytracker.data.db.Subject;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.databinding.FragmentFocusBinding;
import com.krittish.dailytracker.focus.FocusTimerManager;
import com.krittish.dailytracker.ui.common.Dialogs;
import com.krittish.dailytracker.ui.common.GenericAdapter;
import com.krittish.dailytracker.util.AppExecutors;
import com.krittish.dailytracker.util.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FocusFragment extends Fragment {
    private FragmentFocusBinding b;
    private FocusViewModel vm;
    private FocusTimerManager mgr;
    private SettingsRepository settings;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateUi();
            handler.postDelayed(this, 500);
        }
    };

    private List<Subject> subjects = new ArrayList<>();
    private long subjectId = 0;
    private String subjectName = "Focus";
    private boolean wasActive;
    private boolean userStopped;
    private int goal = 120;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentFocusBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(this).get(FocusViewModel.class);
        mgr = FocusTimerManager.get(requireContext());
        settings = SettingsRepository.get(requireContext());

        GenericAdapter<FocusSession> adapter = new GenericAdapter<>(R.layout.item_session, s -> s.id, this::bindSession);
        adapter.attachTo(b.rvSessions);
        vm.recent.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            b.tvNoSessions.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.subjects.observe(getViewLifecycleOwner(), list -> {
            subjects = list;
            buildSubjectChips();
        });
        vm.todayMinutes.observe(getViewLifecycleOwner(), m -> showGoal(m == null ? 0 : m));

        // presets
        int f = settings.focusMinutes(), br = settings.breakMinutes();
        b.togglePreset.check(f == 25 && br == 5 ? R.id.btnP25 : f == 50 && br == 10 ? R.id.btnP50 : R.id.btnPCustom);
        b.togglePreset.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            if (id == R.id.btnP25) settings.setPreset(25, 5);
            else if (id == R.id.btnP50) settings.setPreset(50, 10);
            else askCustomPreset();
            updateUi();
        });

        b.btnStart.setOnClickListener(v -> {
            Dialogs.askExactAlarmIfNeeded(requireContext());
            userStopped = false;
            mgr.start(FocusTimerManager.FOCUS, subjectId, subjectName, settings.focusMinutes(), settings.breakMinutes());
            updateUi();
        });
        b.btnPause.setOnClickListener(v -> {
            if (mgr.isPaused()) mgr.resume(); else mgr.pause();
            updateUi();
        });
        b.btnStop.setOnClickListener(v -> {
            userStopped = true;
            AppExecutors.DISK_IO.execute(mgr::stop);
            b.tvTimer.postDelayed(this::updateUi, 150);
        });
    }

    private void askCustomPreset() {
        Dialogs.promptNumber(requireContext(), "Focus minutes", "e.g. 40", settings.focusMinutes(), focus ->
                Dialogs.promptNumber(requireContext(), "Break minutes", "e.g. 8", settings.breakMinutes(), brk -> {
                    settings.setPreset((int) Math.max(1, Math.min(300, focus)), (int) Math.max(0, Math.min(60, brk)));
                    updateUi();
                }));
    }

    private void buildSubjectChips() {
        b.cgSubject.removeAllViews();
        for (Subject s : subjects) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, b.cgSubject, false);
            chip.setText(s.name);
            chip.setId(View.generateViewId());
            chip.setOnClickListener(v -> {
                subjectId = chip.isChecked() ? s.id : 0;
                subjectName = chip.isChecked() ? s.name : "Focus";
                updateUi();
            });
            b.cgSubject.addView(chip);
            if (s.id == subjectId) chip.setChecked(true);
        }
        Chip plus = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, b.cgSubject, false);
        plus.setText("+");
        plus.setCheckable(false);
        plus.setOnClickListener(v -> Dialogs.promptText(requireContext(), "New subject", "e.g. Maths", "",
                InputType.TYPE_CLASS_TEXT, name -> {
                    if (!name.isEmpty()) vm.addSubject(name);
                }));
        b.cgSubject.addView(plus);
    }

    private void bindSession(View v, FocusSession s) {
        ((TextView) v.findViewById(R.id.tvTitle)).setText(
                (s.subjectName == null ? "Focus" : s.subjectName) + " · " + s.focusedMinutes + " min");
        ((TextView) v.findViewById(R.id.tvMeta)).setText(
                DateUtils.dayLabel(s.startTime) + ", " + DateUtils.formatMillisTime(s.startTime));
        ((TextView) v.findViewById(R.id.tvStatus)).setText("COMPLETED".equals(s.status) ? "done" : "interrupted");
    }

    private void showGoal(int minutes) {
        goal = Math.max(1, settings.focusGoal());
        b.tvGoal.setText("Today: " + minutes + " / " + goal + " min");
        b.goalBar.setProgressCompat(Math.min(100, minutes * 100 / goal), true);
    }

    private void updateUi() {
        if (b == null) return;
        boolean active = mgr.isActive();
        boolean paused = mgr.isPaused();
        if (active && !paused && mgr.remainingMs() <= 0) {
            AppExecutors.DISK_IO.execute(mgr::completeIfDue);       // writes to the database: background thread
        }
        if (wasActive && !active && !userStopped && FocusTimerManager.FOCUS.equals(mgr.mode())) {
            Snackbar.make(b.getRoot(), "Session done. Nice work!", Snackbar.LENGTH_LONG)
                    .setAction("Start break", v -> mgr.startBreakAfterFocus()).show();
        }
        wasActive = active;

        long rem = active ? mgr.remainingMs() : settings.focusMinutes() * 60_000L;
        long secs = (rem + 999) / 1000;
        b.tvTimer.setText(String.format(Locale.ENGLISH, "%02d:%02d", secs / 60, secs % 60));
        if (active) {
            boolean brk = FocusTimerManager.BREAK.equals(mgr.mode());
            b.tvTimerLabel.setText((brk ? "Break" : mgr.subjectName()) + " · " + (mgr.plannedMs() / 60_000) + " min"
                    + (paused ? " · paused" : ""));
        } else {
            b.tvTimerLabel.setText(subjectName + " · " + settings.focusMinutes() + " min");
        }
        b.btnStart.setVisibility(active ? View.GONE : View.VISIBLE);
        b.btnPause.setVisibility(active ? View.VISIBLE : View.GONE);
        b.btnStop.setVisibility(active ? View.VISIBLE : View.GONE);
        b.btnPause.setText(paused ? "Resume" : "Pause");
        b.togglePreset.setEnabled(!active);
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.bindDay();
        wasActive = mgr.isActive();
        handler.post(tick);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
