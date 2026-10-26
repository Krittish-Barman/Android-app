package com.krittish.dailytracker.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.db.Category;
import com.krittish.dailytracker.data.db.MerchantRule;
import com.krittish.dailytracker.data.db.MoneyEntry;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.notifications.NotificationHelper;
import com.krittish.dailytracker.util.AppExecutors;
import com.krittish.dailytracker.util.Callback;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.DateUtils;
import com.krittish.dailytracker.util.MoneyFormat;

import java.util.List;

public class MoneyRepository {
    private final Context app;
    private final AppDatabase db;

    public MoneyRepository(Context context) {
        this.app = context.getApplicationContext();
        this.db = AppDatabase.get(app);
    }

    // ---------- observe ----------
    public LiveData<List<MoneyEntry>> observeMonth(String monthKey) {
        return db.moneyDao().observeConfirmed(DateUtils.monthStartMillis(monthKey), DateUtils.monthEndMillis(monthKey));
    }

    public LiveData<Long> observeExpenseSum(long fromMillis, long toMillis) {
        return db.moneyDao().observeExpenseSum(fromMillis, toMillis);
    }

    public LiveData<List<MoneyEntry>> observePending() { return db.moneyDao().observePending(); }

    public LiveData<Integer> observePendingCount() { return db.moneyDao().observePendingCount(); }

    public LiveData<List<Category>> observeCategories() { return db.categoryDao().observeAll(); }

    public void getEntry(long id, Callback<MoneyEntry> cb) {
        AppExecutors.DISK_IO.execute(() -> {
            MoneyEntry e = db.moneyDao().getById(id);
            AppExecutors.main(() -> cb.on(e));
        });
    }

    // ---------- write ----------
    public void save(MoneyEntry e) {
        AppExecutors.DISK_IO.execute(() -> {
            if (e.id == 0) db.moneyDao().insertIgnore(e);
            else db.moneyDao().update(e);
            checkBudget();
        });
    }

    public void delete(MoneyEntry e) {
        AppExecutors.DISK_IO.execute(() -> db.moneyDao().delete(e));
    }

    /** Undo for swipe-to-delete: same id goes back in. */
    public void restore(MoneyEntry e) {
        AppExecutors.DISK_IO.execute(() -> db.moneyDao().insertIgnore(e));
    }

    public void confirm(long id) {
        AppExecutors.DISK_IO.execute(() -> confirmSync(id));
    }

    /** Background thread only (notification button uses it). */
    public void confirmSync(long id) {
        db.moneyDao().setStatus(id, "CONFIRMED");
        checkBudget();
    }

    public void ignore(long id) {
        AppExecutors.DISK_IO.execute(() -> db.moneyDao().setStatus(id, "IGNORED"));
    }

    public void setCategory(long id, long categoryId) {
        AppExecutors.DISK_IO.execute(() -> db.moneyDao().setCategory(id, categoryId));
    }

    public void markTransfer(long a, long b) {
        AppExecutors.DISK_IO.execute(() -> {
            db.moneyDao().markTransfer(a);
            db.moneyDao().markTransfer(b);
        });
    }

    /** Confirms every pending entry whose category was found by a rule. Returns the count on the main thread. */
    public void confirmAllWithRules(Callback<Integer> cb) {
        AppExecutors.DISK_IO.execute(() -> {
            int n = 0;
            for (MoneyEntry e : db.moneyDao().pendingNow()) {
                if (e.categoryId != 0) {
                    db.moneyDao().setStatus(e.id, "CONFIRMED");
                    n++;
                }
            }
            checkBudget();
            int count = n;
            AppExecutors.main(() -> cb.on(count));
        });
    }

    public void addCategory(String name, String type, int color) {
        AppExecutors.DISK_IO.execute(() -> {
            Category c = new Category();
            c.name = name;
            c.type = type;
            c.color = color;
            db.categoryDao().insert(c);
        });
    }

    /** "Always use this category for this merchant". User rules have priority 100 so they win. */
    public void addRule(String merchant, long categoryId) {
        AppExecutors.DISK_IO.execute(() -> {
            MerchantRule r = new MerchantRule();
            r.pattern = merchant.trim().toLowerCase();
            r.categoryId = categoryId;
            r.priority = 100;
            r.isUserCreated = true;
            db.merchantRuleDao().insert(r);
        });
    }

    // ---------- budget alert (80% and 100%, once per month each) ----------
    /** Background thread only. */
    public void checkBudget() {
        SettingsRepository s = SettingsRepository.get(app);
        if (!s.notifOn(Constants.NT_BUDGET)) return;
        long limit = s.monthlyBudgetPaise();
        if (limit <= 0) return;
        String key = DateUtils.currentMonthKey();
        long spent = db.moneyDao().expenseSumNow(DateUtils.monthStartMillis(key), DateUtils.monthEndMillis(key));
        int pct = (int) (spent * 100 / limit);
        String monthName = DateUtils.monthLabel(key).split(" ")[0];
        if (pct >= 100 && !s.flag("a100_" + key)) {
            s.setFlag("a100_" + key);
            s.setFlag("a80_" + key);
            alert("Budget used up", "You have used 100% of your " + monthName + " budget ("
                    + MoneyFormat.format(spent) + " of " + MoneyFormat.format(limit) + ").");
        } else if (pct >= 80 && pct < 100 && !s.flag("a80_" + key)) {
            s.setFlag("a80_" + key);
            alert("Budget alert", "You have used 80% of your " + monthName + " budget.");
        }
    }

    private void alert(String title, String text) {
        NotificationHelper.notify(app, Constants.NOTIF_BUDGET,
                NotificationHelper.builder(app, Constants.CH_MONEY, title, text)
                        .setContentIntent(NotificationHelper.openApp(app, 41, Constants.OPEN_MONEY)), true);
    }
}
