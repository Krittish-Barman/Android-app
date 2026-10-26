package com.krittish.dailytracker.ui.money;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import android.widget.TextView;

import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.Category;
import com.krittish.dailytracker.data.db.MoneyEntry;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.databinding.FragmentMoneyBinding;
import com.krittish.dailytracker.domain.BudgetCalculator;
import com.krittish.dailytracker.ui.common.GenericAdapter;
import com.krittish.dailytracker.ui.common.SwipeToDelete;
import com.krittish.dailytracker.util.DateUtils;
import com.krittish.dailytracker.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoneyFragment extends Fragment {
    private FragmentMoneyBinding b;
    private MoneyViewModel vm;
    private GenericAdapter<MoneyEntry> adapter;

    private final Map<Long, Category> categoryById = new HashMap<>();
    private List<MoneyEntry> all = new ArrayList<>();
    private long filterCategory = -1;          // -1 = all
    private boolean filterIncome;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentMoneyBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(requireActivity()).get(MoneyViewModel.class);

        adapter = new GenericAdapter<>(R.layout.item_money_entry, e -> e.id, this::bindRow);
        adapter.attachTo(b.rvEntries);
        SwipeToDelete.attach(b.rvEntries, pos -> {
            MoneyEntry e = adapter.itemAt(pos);
            vm.delete(e);
            Snackbar.make(b.getRoot(), "Entry deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo", v -> vm.restore(e)).show();
        });

        b.fabAdd.setOnClickListener(v -> new AddEntryBottomSheet().show(getParentFragmentManager(), "add_entry"));
        b.btnReview.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.reviewFragment));
        b.btnPrevMonth.setOnClickListener(v -> vm.shiftMonth(-1));
        b.btnNextMonth.setOnClickListener(v -> vm.shiftMonth(1));
        b.cgFilter.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Object tag = group.findViewById(ids.get(0)).getTag();
            filterIncome = "INCOME".equals(tag);
            filterCategory = tag instanceof Long ? (Long) tag : -1;
            applyFilter();
        });

        vm.categories.observe(getViewLifecycleOwner(), list -> {
            categoryById.clear();
            for (Category c : list) categoryById.put(c.id, c);
            buildChips(list);
            adapter.notifyDataSetChanged();
        });
        vm.entries.observe(getViewLifecycleOwner(), list -> {
            all = list;
            applyFilter();
        });
        vm.spent.observe(getViewLifecycleOwner(), s -> showBudget(s == null ? 0 : s));
        vm.pendingCount.observe(getViewLifecycleOwner(), n -> {
            b.btnReview.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
            b.btnReview.setText("Review " + n);
        });
        showMonth();
    }

    private void showMonth() {
        b.tvMonth.setText(DateUtils.monthLabel(vm.monthKey()));
    }

    private void buildChips(List<Category> cats) {
        long keepCat = filterCategory;
        boolean keepIncome = filterIncome;
        b.cgFilter.removeAllViews();
        Chip allChip = addChip("All", -1L);
        for (Category c : cats) {
            if ("EXPENSE".equals(c.type)) addChip(c.name, c.id);
        }
        addChip("Income", "INCOME");
        // restore selection
        for (int i = 0; i < b.cgFilter.getChildCount(); i++) {
            View chip = b.cgFilter.getChildAt(i);
            Object tag = chip.getTag();
            boolean match = keepIncome ? "INCOME".equals(tag) : (tag instanceof Long && (Long) tag == keepCat);
            if (match) {
                b.cgFilter.check(chip.getId());
                return;
            }
        }
        b.cgFilter.check(allChip.getId());
    }

    private Chip addChip(String text, Object tag) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, b.cgFilter, false);
        chip.setText(text);
        chip.setTag(tag);
        chip.setId(View.generateViewId());
        b.cgFilter.addView(chip);
        return chip;
    }

    private void applyFilter() {
        List<MoneyEntry> out = new ArrayList<>();
        for (MoneyEntry e : all) {
            if (filterIncome) {
                if (!"INCOME".equals(e.type)) continue;
            } else if (filterCategory != -1 && e.categoryId != filterCategory) {
                continue;
            }
            out.add(e);
        }
        adapter.submitList(out);
        b.tvEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
        b.rvEntries.setVisibility(out.isEmpty() ? View.GONE : View.VISIBLE);
        showMonth();
    }

    private void showBudget(long spent) {
        long limit = SettingsRepository.get(requireContext()).monthlyBudgetPaise();
        b.tvBudgetNumbers.setText(MoneyFormat.format(spent) + " / " + MoneyFormat.format(limit));
        int pct = BudgetCalculator.percentUsed(spent, limit);
        b.budgetBar.setProgressCompat(Math.min(100, pct), true);
        b.tvBudgetStatus.setText(BudgetCalculator.statusText(spent, limit));
        int color = pct >= 100 ? R.color.dt_red : pct >= 80 ? R.color.dt_amber_text : R.color.dt_on_container;
        b.tvBudgetStatus.setTextColor(ContextCompat.getColor(requireContext(), color));
    }

    private void bindRow(View v, MoneyEntry e) {
        Category c = categoryById.get(e.categoryId);
        String cat = "TRANSFER".equals(e.type) ? "Transfer" : c != null ? c.name : "No category";
        String source = "SMS".equals(e.source) ? "UPI auto" : "Manual";
        ((TextView) v.findViewById(R.id.tvTitle)).setText(e.title());
        ((TextView) v.findViewById(R.id.tvMeta)).setText(cat + " · " + source + " · " + DateUtils.dayLabel(e.timestamp)
                + ", " + DateUtils.formatMillisTime(e.timestamp));
        TextView amount = v.findViewById(R.id.tvAmount);
        amount.setText(MoneyFormat.signed(e.amountPaise, e.type));
        int color = "INCOME".equals(e.type) ? R.color.dt_green : "EXPENSE".equals(e.type) ? R.color.dt_red : R.color.dt_muted;
        amount.setTextColor(ContextCompat.getColor(v.getContext(), color));
        v.setOnClickListener(x -> AddEntryBottomSheet.edit(e.id).show(getParentFragmentManager(), "edit_entry"));
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.shiftMonth(0);    // refresh bindings (new month / new budget)
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
