package com.krittish.dailytracker.ui.habits;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.krittish.dailytracker.R;
import com.krittish.dailytracker.databinding.FragmentHabitsBinding;
import com.krittish.dailytracker.domain.HabitRow;
import com.krittish.dailytracker.ui.common.GenericAdapter;

public class HabitsFragment extends Fragment {
    private FragmentHabitsBinding b;
    private HabitsViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentHabitsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(this).get(HabitsViewModel.class);

        GenericAdapter<HabitRow> adapter = new GenericAdapter<>(R.layout.item_habit, r -> r.habit.id,
                (v, r) -> HabitItemBinder.bind(v, r,
                        (row, done) -> vm.setDoneToday(row.habit.id, done),
                        row -> open(R.id.habitDetailFragment, row.habit.id)));
        adapter.attachTo(b.rvHabits);

        View.OnClickListener add = v -> open(R.id.addEditHabitFragment, 0);
        b.btnAdd.setOnClickListener(add);
        b.btnEmptyAdd.setOnClickListener(add);

        vm.rows.observe(getViewLifecycleOwner(), rows -> {
            adapter.submitList(rows);
            boolean empty = rows == null || rows.isEmpty();
            b.emptyBox.setVisibility(empty ? View.VISIBLE : View.GONE);
            b.rvHabits.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.rows.recompute();            // picks up a new day
    }

    private void open(int destination, long habitId) {
        Bundle args = new Bundle();
        args.putLong("habitId", habitId);
        NavHostFragment.findNavController(this).navigate(destination, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
