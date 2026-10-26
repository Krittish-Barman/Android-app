package com.krittish.dailytracker.ui.tasks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.Task;
import com.krittish.dailytracker.data.repo.TaskRepository;
import com.krittish.dailytracker.ui.common.Dialogs;
import com.krittish.dailytracker.util.DateUtils;

public final class TaskDialogs {
    private TaskDialogs() {}

    public static void showAdd(Context ctx, TaskRepository repo) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_task, null);
        TextInputEditText etTitle = v.findViewById(R.id.etTitle);
        ChipGroup cgPriority = v.findViewById(R.id.cgPriority);
        MaterialButton btnDue = v.findViewById(R.id.btnDue);
        MaterialCheckBox cbPin = v.findViewById(R.id.cbPin);
        final long[] due = {0};

        btnDue.setOnClickListener(x -> Dialogs.pickDateTime(ctx, System.currentTimeMillis() + 3_600_000L, ms -> {
            due[0] = ms;
            btnDue.setText("Due " + DateUtils.formatDateTime(ms));
        }));

        new MaterialAlertDialogBuilder(ctx).setTitle("New task").setView(v)
                .setPositiveButton("Add", (d, w) -> {
                    String title = String.valueOf(etTitle.getText()).trim();
                    if (title.isEmpty()) return;
                    Task t = new Task();
                    t.title = title;
                    t.dueAt = due[0];
                    int checked = cgPriority.getCheckedChipId();
                    t.priority = checked == R.id.chipHigh ? 1 : checked == R.id.chipLow ? 3 : 2;
                    repo.add(t, cbPin.isChecked(), () ->
                            Toast.makeText(ctx, "You already have 3 pinned tasks today", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null).show();
    }
}
