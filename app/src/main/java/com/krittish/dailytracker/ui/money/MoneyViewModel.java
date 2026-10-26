package com.krittish.dailytracker.ui.money;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.krittish.dailytracker.data.db.Category;
import com.krittish.dailytracker.data.db.MoneyEntry;
import com.krittish.dailytracker.data.repo.MoneyRepository;
import com.krittish.dailytracker.util.Callback;
import com.krittish.dailytracker.util.DateUtils;

import java.util.List;

/** Shared by the Money tab, the Add sheet and the Review screen (activity scope). */
public class MoneyViewModel extends AndroidViewModel {
    private final MoneyRepository repo;

    public final MediatorLiveData<List<MoneyEntry>> entries = new MediatorLiveData<>();
    public final MediatorLiveData<Long> spent = new MediatorLiveData<>();
    public final LiveData<List<Category>> categories;
    public final LiveData<List<MoneyEntry>> pending;
    public final LiveData<Integer> pendingCount;

    private String monthKey = DateUtils.currentMonthKey();
    private LiveData<List<MoneyEntry>> entriesSrc;
    private LiveData<Long> spentSrc;

    public MoneyViewModel(@NonNull Application app) {
        super(app);
        repo = new MoneyRepository(app);
        categories = repo.observeCategories();
        pending = repo.observePending();
        pendingCount = repo.observePendingCount();
        bind();
    }

    public String monthKey() { return monthKey; }

    public void shiftMonth(int delta) {
        monthKey = DateUtils.shiftMonth(monthKey, delta);
        bind();
    }

    /** Switch the month: remove the old LiveData sources and add the new ones. */
    private void bind() {
        if (entriesSrc != null) entries.removeSource(entriesSrc);
        if (spentSrc != null) spent.removeSource(spentSrc);
        entriesSrc = repo.observeMonth(monthKey);
        spentSrc = repo.observeExpenseSum(DateUtils.monthStartMillis(monthKey), DateUtils.monthEndMillis(monthKey));
        entries.addSource(entriesSrc, entries::setValue);
        spent.addSource(spentSrc, spent::setValue);
    }

    public void save(MoneyEntry e) { repo.save(e); }
    public void delete(MoneyEntry e) { repo.delete(e); }
    public void restore(MoneyEntry e) { repo.restore(e); }
    public void getEntry(long id, Callback<MoneyEntry> cb) { repo.getEntry(id, cb); }
    public void confirm(long id) { repo.confirm(id); }
    public void ignore(long id) { repo.ignore(id); }
    public void setCategory(long id, long categoryId) { repo.setCategory(id, categoryId); }
    public void markTransfer(long a, long b) { repo.markTransfer(a, b); }
    public void confirmAllWithRules(Callback<Integer> cb) { repo.confirmAllWithRules(cb); }
    public void addCategory(String name, String type, int color) { repo.addCategory(name, type, color); }
    public void addRule(String merchant, long categoryId) { repo.addRule(merchant, categoryId); }
}
