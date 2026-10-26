package com.krittish.dailytracker.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.databinding.FragmentStatsBinding;
import com.krittish.dailytracker.util.DateUtils;
import com.krittish.dailytracker.util.MoneyFormat;

import java.util.List;

public class StatsFragment extends Fragment {
    private FragmentStatsBinding b;
    private StatsViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentStatsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(this).get(StatsViewModel.class);
        b.toggleRange.addOnButtonCheckedListener((group, id, checked) -> {
            if (checked) vm.load(id == R.id.btnMonth);
        });
        b.toggleRange.check(R.id.btnWeek);

        vm.data().observe(getViewLifecycleOwner(), d -> {
            b.chart.setData(d.study, d.labels);
            b.tvStudyTotal.setText("Total: " + DateUtils.formatMinutes(d.totalMinutes));
            b.tvBestTime.setText(d.bestTime);
            fillRows(b.llHabits, d.habits, "No habits yet");
            fillRows(b.llSpending, d.spending, "No spending in this period");
            b.tvIncomeExpense.setText("Income " + MoneyFormat.format(d.incomePaise)
                    + "  ·  Expense " + MoneyFormat.format(d.expensePaise));
        });
    }

    private void fillRows(LinearLayout parent, List<StatsViewModel.Row> rows, String emptyText) {
        parent.removeAllViews();
        if (rows.isEmpty()) {
            TextView t = new TextView(requireContext());
            t.setText(emptyText);
            t.setTextAppearance(R.style.TextAppearance_DT_Caption);
            parent.addView(t);
            return;
        }
        for (StatsViewModel.Row r : rows) {
            View row = getLayoutInflater().inflate(R.layout.item_stat_row, parent, false);
            ((TextView) row.findViewById(R.id.tvLabel)).setText(r.label);
            ((TextView) row.findViewById(R.id.tvValue)).setText(r.valueText);
            ((LinearProgressIndicator) row.findViewById(R.id.bar)).setProgressCompat(r.percent, false);
            parent.addView(row);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.load(b.toggleRange.getCheckedButtonId() == R.id.btnMonth);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
