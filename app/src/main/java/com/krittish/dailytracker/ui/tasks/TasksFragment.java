package com.krittish.dailytracker.ui.tasks;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.Task;
import com.krittish.dailytracker.databinding.FragmentTasksBinding;
import com.krittish.dailytracker.ui.common.GenericAdapter;
import com.krittish.dailytracker.ui.common.SwipeToDelete;
import com.krittish.dailytracker.util.DateUtils;

public class TasksFragment extends Fragment {
    private FragmentTasksBinding b;
    private TasksViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentTasksBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(this).get(TasksViewModel.class);
        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        GenericAdapter<Task> adapter = new GenericAdapter<>(R.layout.item_task, t -> t.id, this::bindRow);
        adapter.attachTo(b.rvTasks);
        SwipeToDelete.attach(b.rvTasks, pos -> {
            Task t = adapter.itemAt(pos);
            vm.delete(t);
            Snackbar.make(b.getRoot(), "Task deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo", v -> vm.restore(t)).show();
        });
        b.fabAdd.setOnClickListener(v -> TaskDialogs.showAdd(requireContext(), vm.repo()));

        vm.tasks.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            b.rvTasks.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void bindRow(View v, Task t) {
        TextView title = v.findViewById(R.id.tvTitle);
        title.setText(t.title);
        title.setPaintFlags(t.done ? title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

        String prio = t.priority == 1 ? "High" : t.priority == 3 ? "Low" : "Medium";
        String due = t.dueAt > 0 ? " · " + DateUtils.dayLabel(t.dueAt) + ", " + DateUtils.formatMillisTime(t.dueAt) : "";
        ((TextView) v.findViewById(R.id.tvMeta)).setText(prio + due);

        MaterialCheckBox cb = v.findViewById(R.id.cbDone);
        cb.setChecked(t.done);
        cb.setOnClickListener(x -> vm.setDone(t, cb.isChecked()));

        TextView pin = v.findViewById(R.id.btnPin);
        boolean pinned = t.pinnedDate == DateUtils.today();
        pin.setText(pinned ? "★" : "☆");
        pin.setOnClickListener(x -> vm.togglePin(t, () ->
                Toast.makeText(requireContext(), "Only 3 tasks can be pinned for today", Toast.LENGTH_SHORT).show()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
