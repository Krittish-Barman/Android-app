package com.krittish.dailytracker.ui.home;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.Task;
import com.krittish.dailytracker.databinding.FragmentHomeBinding;
import com.krittish.dailytracker.domain.HabitRow;
import com.krittish.dailytracker.domain.UpNextProvider;
import com.krittish.dailytracker.ui.MainActivity;
import com.krittish.dailytracker.ui.common.GenericAdapter;
import com.krittish.dailytracker.ui.habits.HabitItemBinder;
import com.krittish.dailytracker.ui.money.AddEntryBottomSheet;
import com.krittish.dailytracker.ui.tasks.TaskDialogs;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.DateUtils;
import com.krittish.dailytracker.util.MoneyFormat;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding b;
    private HomeViewModel vm;
    private UpNextProvider.UpNext upNext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentHomeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        b.tvGreeting.setText(DateUtils.greeting());
        b.tvDate.setText(DateUtils.formatDay(DateUtils.today()));

        GenericAdapter<HabitRow> habitAdapter = new GenericAdapter<>(R.layout.item_habit, r -> r.habit.id,
                (v, r) -> HabitItemBinder.bind(v, r,
                        (row, done) -> vm.setDone(row.habit.id, done),
                        row -> {
                            Bundle args = new Bundle();
                            args.putLong("habitId", row.habit.id);
                            NavHostFragment.findNavController(this).navigate(R.id.habitDetailFragment, args);
                        }));
        habitAdapter.attachTo(b.rvHabits);

        GenericAdapter<Task> taskAdapter = new GenericAdapter<>(R.layout.item_task, t -> t.id, this::bindTask);
        taskAdapter.attachTo(b.rvTasks);

        b.btnSettings.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.settingsFragment));
        b.btnAllTasks.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.tasksFragment));
        b.btnQuickExpense.setOnClickListener(v ->
                new AddEntryBottomSheet().show(getParentFragmentManager(), "add_entry"));
        b.btnQuickTask.setOnClickListener(v -> TaskDialogs.showAdd(requireContext(), vm.taskRepo()));
        b.btnQuickFocus.setOnClickListener(v -> ((MainActivity) requireActivity()).openTab(R.id.focusFragment));
        b.btnUpNextDone.setOnClickListener(v -> {
            if (upNext == null) return;
            if (Constants.KIND_HABIT.equals(upNext.kind)) vm.setDone(upNext.id, true);
            else markTaskDone(upNext.id);
        });

        vm.state().observe(getViewLifecycleOwner(), s -> {
            habitAdapter.submitList(s.dueHabits);
            taskAdapter.submitList(s.pinned);
            b.tvNoHabits.setVisibility(s.dueHabits.isEmpty() ? View.VISIBLE : View.GONE);
            b.tvNoTasks.setVisibility(s.pinned.isEmpty() ? View.VISIBLE : View.GONE);

            b.progressRing.setProgressCompat(s.percent, true);
            b.tvPercent.setText(s.percent + "%");
            b.tvProgressSub.setText(s.done + " of " + s.total + " habits");
            b.tvBestStreak.setText(s.bestStreak > 0 ? s.bestStreak + "-day best streak" : "Start a streak today");
            b.tvSpent.setText(MoneyFormat.format(s.spentToday));
            b.tvStudy.setText(DateUtils.formatMinutes(s.studyMin));
            b.tvTasksStat.setText(s.tasksDone + " / " + s.tasksTotal);

            upNext = s.upNext;
            if (upNext == null) {
                b.cardUpNext.setVisibility(View.GONE);
            } else {
                b.cardUpNext.setVisibility(View.VISIBLE);
                long mins = Math.max(0, (upNext.timeMillis - System.currentTimeMillis()) / 60_000);
                b.tvUpNextLabel.setText("UP NEXT · in " + (mins >= 60 ? (mins / 60) + " h " + (mins % 60) + " min" : mins + " min"));
                b.tvUpNext.setText(upNext.title + " · " + DateUtils.formatMillisTime(upNext.timeMillis));
            }
        });
    }

    private void markTaskDone(long id) {
        Task t = new Task();
        t.id = id;
        vm.taskRepo().setDone(t, true);
    }

    private void bindTask(View v, Task t) {
        TextView title = v.findViewById(R.id.tvTitle);
        title.setText(t.title);
        title.setPaintFlags(t.done ? title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        ((TextView) v.findViewById(R.id.tvMeta)).setText(t.dueAt > 0 ? "Due " + DateUtils.formatMillisTime(t.dueAt) : "No due time");
        v.findViewById(R.id.btnPin).setVisibility(View.GONE);
        MaterialCheckBox cb = v.findViewById(R.id.cbDone);
        cb.setChecked(t.done);
        cb.setOnClickListener(x -> vm.taskRepo().setDone(t, cb.isChecked()));
    }

    @Override
    public void onResume() {
        super.onResume();
        b.tvGreeting.setText(DateUtils.greeting());
        b.tvDate.setText(DateUtils.formatDay(DateUtils.today()));
        vm.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
