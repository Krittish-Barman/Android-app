package com.krittish.dailytracker.ui.money;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.Category;
import com.krittish.dailytracker.data.db.MoneyEntry;
import com.krittish.dailytracker.databinding.FragmentReviewBinding;
import com.krittish.dailytracker.ui.common.GenericAdapter;
import com.krittish.dailytracker.util.DateUtils;
import com.krittish.dailytracker.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Entries found in SMS wait here: Confirm, Edit or Ignore. */
public class ReviewFragment extends Fragment {

    /** A pending entry, plus a possible "other side" if it looks like a transfer between own accounts. */
    private static class Row {
        MoneyEntry entry;
        MoneyEntry pair;
    }

    private FragmentReviewBinding b;
    private MoneyViewModel vm;
    private GenericAdapter<Row> adapter;
    private final Map<Long, Category> categoryById = new HashMap<>();
    private List<Category> categories = new ArrayList<>();
    private final Set<Long> keepAsExpense = new HashSet<>();
    private List<MoneyEntry> lastPending = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentReviewBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(requireActivity()).get(MoneyViewModel.class);
        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        adapter = new GenericAdapter<>(R.layout.item_pending_entry, r -> r.entry.id, this::bindRow);
        adapter.attachTo(b.rvPending);

        b.btnConfirmAll.setOnClickListener(v -> vm.confirmAllWithRules(n ->
                Toast.makeText(requireContext(), n + " confirmed", Toast.LENGTH_SHORT).show()));

        vm.categories.observe(getViewLifecycleOwner(), list -> {
            categories = list;
            categoryById.clear();
            for (Category c : list) categoryById.put(c.id, c);
            adapter.notifyDataSetChanged();
        });
        vm.pending.observe(getViewLifecycleOwner(), list -> {
            lastPending = list;
            showRows();
        });
    }

    private void showRows() {
        List<Row> rows = new ArrayList<>();
        for (MoneyEntry e : lastPending) {
            Row r = new Row();
            r.entry = e;
            if (!keepAsExpense.contains(e.id)) r.pair = findPair(e);
            rows.add(r);
        }
        adapter.submitList(rows);
        boolean empty = rows.isEmpty();
        b.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        b.rvPending.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.btnConfirmAll.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.tvTitle.setText("Review (" + rows.size() + ")");
    }

    /** Debit + credit of the same amount within 5 minutes on different accounts = maybe a transfer. */
    private MoneyEntry findPair(MoneyEntry e) {
        for (MoneyEntry o : lastPending) {
            if (o.id != e.id && o.amountPaise == e.amountPaise && !o.type.equals(e.type)
                    && Math.abs(o.timestamp - e.timestamp) <= 5 * 60_000L
                    && !Objects.equals(o.accountTag, e.accountTag)) {
                return o;
            }
        }
        return null;
    }

    private void bindRow(View v, Row r) {
        MoneyEntry e = r.entry;
        ((TextView) v.findViewById(R.id.tvTitle)).setText(e.title());
        TextView amount = v.findViewById(R.id.tvAmount);
        amount.setText(MoneyFormat.signed(e.amountPaise, e.type));
        amount.setTextColor(ContextCompat.getColor(v.getContext(),
                "INCOME".equals(e.type) ? R.color.dt_green : R.color.dt_red));
        String acct = e.accountTag != null ? " · A/c XX" + e.accountTag : "";
        ((TextView) v.findViewById(R.id.tvMeta)).setText("UPI" + acct + " · " + DateUtils.formatDateTime(e.timestamp));

        MaterialButton cat = v.findViewById(R.id.btnCategory);
        Category c = categoryById.get(e.categoryId);
        cat.setText(c != null ? c.name + " ▾" : "Pick category ▾");
        cat.setOnClickListener(x -> pickCategory(e));

        View transferBox = v.findViewById(R.id.transferBox);
        if (r.pair != null) {
            transferBox.setVisibility(View.VISIBLE);
            ((TextView) v.findViewById(R.id.tvTransfer)).setText("Possible transfer " + MoneyFormat.format(e.amountPaise)
                    + ": debit and credit within 5 min on different accounts.");
            v.findViewById(R.id.btnMarkTransfer).setOnClickListener(x -> vm.markTransfer(e.id, r.pair.id));
            v.findViewById(R.id.btnKeep).setOnClickListener(x -> {
                keepAsExpense.add(e.id);
                keepAsExpense.add(r.pair.id);
                showRows();
            });
        } else {
            transferBox.setVisibility(View.GONE);
        }

        v.findViewById(R.id.btnConfirm).setOnClickListener(x -> vm.confirm(e.id));
        v.findViewById(R.id.btnEdit).setOnClickListener(x ->
                AddEntryBottomSheet.edit(e.id).show(getParentFragmentManager(), "edit_entry"));
        v.findViewById(R.id.btnIgnore).setOnClickListener(x -> vm.ignore(e.id));
    }

    private void pickCategory(MoneyEntry e) {
        List<Category> options = new ArrayList<>();
        for (Category c : categories) if (c.type.equals(e.type)) options.add(c);
        String[] names = new String[options.size()];
        for (int i = 0; i < names.length; i++) names[i] = options.get(i).name;
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Category").setItems(names, (d, which) -> {
            Category chosen = options.get(which);
            vm.setCategory(e.id, chosen.id);
            if (e.merchant != null && !e.merchant.isEmpty() && chosen.id != e.categoryId) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setMessage("Always use \"" + chosen.name + "\" for \"" + e.merchant + "\"?")
                        .setPositiveButton("Always", (dd, w) -> vm.addRule(e.merchant, chosen.id))
                        .setNegativeButton("Just once", null).show();
            }
        }).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
