package com.krittish.dailytracker.ui.money;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.db.Category;
import com.krittish.dailytracker.data.db.MoneyEntry;
import com.krittish.dailytracker.databinding.SheetAddEntryBinding;
import com.krittish.dailytracker.ui.common.Dialogs;
import com.krittish.dailytracker.util.DateUtils;
import com.krittish.dailytracker.util.MoneyFormat;

import java.util.ArrayList;
import java.util.List;

/** Add (or edit) an expense / income / transfer without leaving the list. Big amount + number pad. */
public class AddEntryBottomSheet extends BottomSheetDialogFragment {

    public static AddEntryBottomSheet edit(long entryId) {
        AddEntryBottomSheet s = new AddEntryBottomSheet();
        Bundle a = new Bundle();
        a.putLong("entryId", entryId);
        s.setArguments(a);
        return s;
    }

    private SheetAddEntryBinding b;
    private MoneyViewModel vm;
    private final StringBuilder amount = new StringBuilder();
    private String type = "EXPENSE";
    private long categoryId = 0;
    private long originalCategoryId = 0;
    private long timestamp = System.currentTimeMillis();
    private MoneyEntry editing;
    private List<Category> categories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = SheetAddEntryBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(requireActivity()).get(MoneyViewModel.class);

        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫"};
        for (String k : keys) {
            MaterialButton key = (MaterialButton) getLayoutInflater().inflate(R.layout.view_key, b.pad, false);
            key.setText(k);
            key.setOnClickListener(v -> onKey(k));
            b.pad.addView(key);
        }

        b.toggleType.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            type = id == R.id.btnIncome ? "INCOME" : id == R.id.btnTransfer ? "TRANSFER" : "EXPENSE";
            categoryId = 0;
            b.btnSave.setText("Save " + type.toLowerCase());
            rebuildChips();
        });
        b.toggleType.check(R.id.btnExpense);

        vm.categories.observe(getViewLifecycleOwner(), list -> {
            categories = list;
            rebuildChips();
        });

        b.btnWhen.setOnClickListener(v -> Dialogs.pickDateTime(requireContext(), timestamp, ms -> {
            timestamp = ms;
            showWhen();
        }));
        b.btnSave.setOnClickListener(v -> save());

        long id = getArguments() == null ? 0 : getArguments().getLong("entryId", 0);
        if (id != 0) {
            vm.getEntry(id, e -> {
                if (e == null || b == null) return;
                editing = e;
                timestamp = e.timestamp;
                categoryId = e.categoryId;
                originalCategoryId = e.categoryId;
                amount.setLength(0);
                String plain = MoneyFormat.format(e.amountPaise).replace("₹", "").replace(",", "");
                amount.append(plain);
                b.toggleType.check("INCOME".equals(e.type) ? R.id.btnIncome : "TRANSFER".equals(e.type) ? R.id.btnTransfer : R.id.btnExpense);
                categoryId = e.categoryId;           // the toggle listener resets it, so set again
                b.etNote.setText(useMerchant() ? e.merchant : e.note);
                rebuildChips();
                showAmount();
                showWhen();
            });
        }
        showAmount();
        showWhen();
    }

    private boolean useMerchant() { return editing != null && "SMS".equals(editing.source); }

    private void onKey(String k) {
        if ("⌫".equals(k)) {
            if (amount.length() > 0) amount.setLength(amount.length() - 1);
        } else if (".".equals(k)) {
            if (amount.indexOf(".") < 0) {
                if (amount.length() == 0) amount.append("0");
                amount.append(".");
            }
        } else {
            int dot = amount.indexOf(".");
            if (dot >= 0 && amount.length() - dot > 2) return;      // max 2 decimals
            if (amount.length() >= 9) return;
            if (amount.length() == 1 && amount.charAt(0) == '0') amount.setLength(0);
            amount.append(k);
        }
        showAmount();
    }

    private void showAmount() {
        if (b == null) return;
        b.tvAmount.setText("₹ " + (amount.length() == 0 ? "0" : amount.toString()));
    }

    private void showWhen() {
        if (b == null) return;
        b.btnWhen.setText(DateUtils.dayLabel(timestamp) + " · " + DateUtils.formatMillisTime(timestamp));
    }

    private void rebuildChips() {
        if (b == null) return;
        b.cgCategory.removeAllViews();
        b.chipsScroll.setVisibility("TRANSFER".equals(type) ? View.GONE : View.VISIBLE);
        if ("TRANSFER".equals(type)) return;
        for (Category c : categories) {
            if (!c.type.equals(type)) continue;
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, b.cgCategory, false);
            chip.setText(c.name);
            chip.setId(View.generateViewId());
            chip.setTag(c.id);
            chip.setChipStrokeColor(ColorStateList.valueOf(c.color));
            chip.setOnClickListener(v -> categoryId = chip.isChecked() ? c.id : 0);
            b.cgCategory.addView(chip);
            if (c.id == categoryId) chip.setChecked(true);
        }
        Chip plus = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, b.cgCategory, false);
        plus.setText("+ New");
        plus.setCheckable(false);
        plus.setOnClickListener(v -> Dialogs.promptText(requireContext(), "New category", "Name", "",
                InputType.TYPE_CLASS_TEXT, name -> {
                    if (!name.isEmpty()) vm.addCategory(name, type, 0xFF0F766E);
                }));
        b.cgCategory.addView(plus);
    }

    private void save() {
        long paise = MoneyFormat.parseToPaise(amount.toString());
        if (paise <= 0) {
            Toast.makeText(requireContext(), "Enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }
        MoneyEntry e = editing != null ? editing : new MoneyEntry();
        String text = String.valueOf(b.etNote.getText()).trim();
        e.amountPaise = paise;
        e.type = type;
        e.categoryId = "TRANSFER".equals(type) ? 0 : categoryId;
        e.timestamp = timestamp;
        if (useMerchant()) e.merchant = text; else e.note = text;
        if (editing == null) {
            e.source = "MANUAL";
        }
        e.status = "CONFIRMED";          // saving a pending SMS entry confirms it
        e.rawText = null;

        boolean askRule = useMerchant() && e.merchant != null && !e.merchant.isEmpty()
                && e.categoryId != 0 && e.categoryId != originalCategoryId;
        if (askRule) {
            final String merchant = e.merchant;
            final long cat = e.categoryId;
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Always use this category for \"" + merchant + "\"?")
                    .setPositiveButton("Always", (d, w) -> {
                        vm.addRule(merchant, cat);
                        commit(e);
                    })
                    .setNegativeButton("Just once", (d, w) -> commit(e))
                    .show();
        } else {
            commit(e);
        }
    }

    private void commit(MoneyEntry e) {
        vm.save(e);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
