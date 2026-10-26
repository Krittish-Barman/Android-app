package com.krittish.dailytracker.ui.habits;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.Habit;
import com.krittish.dailytracker.data.db.HabitLog;
import com.krittish.dailytracker.databinding.FragmentHabitDetailBinding;
import com.krittish.dailytracker.domain.StreakCalculator;
import com.krittish.dailytracker.util.DateUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HabitDetailFragment extends Fragment {
    private FragmentHabitDetailBinding b;
    private HabitsViewModel vm;
    private long habitId;
    private Habit habit;
    private Map<Long, String> logs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentHabitDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(this).get(HabitsViewModel.class);
        habitId = requireArguments().getLong("habitId");
        NavController nav = NavHostFragment.findNavController(this);

        b.btnBack.setOnClickListener(v -> nav.popBackStack());
        b.btnEdit.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("habitId", habitId);
            nav.navigate(R.id.addEditHabitFragment, args);
        });
        b.btnSkip.setOnClickListener(v -> vm.skip(habitId, DateUtils.today()));
        b.btnArchive.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Archive this habit?")
                .setMessage("It disappears from your lists. Your history is kept.")
                .setPositiveButton("Archive", (d, w) -> {
                    vm.archive(habitId);
                    nav.popBackStack();
                })
                .setNegativeButton("Cancel", null).show());

        b.heatmap.setOnDayClickListener(day -> {
            if (logs == null) return;
            boolean done = "DONE".equals(logs.get(day));
            vm.setDone(habitId, day, !done);
        });

        vm.observeHabit(habitId).observe(getViewLifecycleOwner(), h -> {
            habit = h;
            refresh();
        });
        vm.observeLogs(habitId).observe(getViewLifecycleOwner(), list -> {
            Map<Long, String> m = new HashMap<>();
            for (HabitLog l : (List<HabitLog>) list) m.put(l.date, l.status);
            logs = m;
            refresh();
        });
    }

    private void refresh() {
        if (b == null || habit == null || logs == null) return;
        b.tvTitle.setText(habit.name);
        b.tvSub.setText(HabitItemBinder.subtitle(habit));
        long start = DateUtils.epochDayOf(habit.createdAt > 0 ? habit.createdAt : System.currentTimeMillis());
        for (Long d : logs.keySet()) if (d < start) start = d;
        StreakCalculator.Result r = StreakCalculator.compute(
                habit.scheduleType, habit.weekdaysMask, habit.timesPerWeek, start, logs, DateUtils.today());
        String unit = "TIMES_PER_WEEK".equals(habit.scheduleType) ? " weeks" : " days";
        b.tvCurrent.setText(r.current + unit);
        b.tvBest.setText(r.best + unit);
        b.heatmap.setData(logs, habit.color);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
