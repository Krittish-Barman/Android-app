package com.krittish.dailytracker.data.db;

/** Default categories, merchant rules and focus subjects. Call on a background thread. */
public final class SeedData {
    private SeedData() {}

    public static void run(AppDatabase db) {
        if (db.categoryDao().count() == 0) {
            String[] expense = {"Food", "Travel", "Recharge & bills", "Study", "Shopping", "Health", "Entertainment", "Other"};
            int[] colors = {0xFFF97316, 0xFF0EA5E9, 0xFF8B5CF6, 0xFF14B8A6, 0xFFEC4899, 0xFFEF4444, 0xFFEAB308, 0xFF64748B};
            for (int i = 0; i < expense.length; i++) addCategory(db, expense[i], "EXPENSE", colors[i]);
            String[] income = {"Pocket money", "Project payment", "Other"};
            int[] incomeColors = {0xFF16A34A, 0xFF0F766E, 0xFF64748B};
            for (int i = 0; i < income.length; i++) addCategory(db, income[i], "INCOME", incomeColors[i]);
        }
        if (db.merchantRuleDao().count() == 0) {
            rules(db, "Food", "zomato", "swiggy", "dominos", "mcdonald", "kfc", "restaurant", "biryani");
            rules(db, "Travel", "irctc", "redbus", "uber", "rapido", "petrol", "fastag");
            rules(db, "Recharge & bills", "jio", "airtel", "vodafone", "recharge", "electricity", "dth");
            rules(db, "Shopping", "amazon", "flipkart", "myntra", "meesho", "ajio");
            rules(db, "Entertainment", "netflix", "spotify", "hotstar", "bookmyshow");
        }
        if (db.subjectDao().count() == 0) {
            addSubject(db, "DSA", 0xFF0F766E);
            addSubject(db, "Java", 0xFFF97316);
            addSubject(db, "English", 0xFF8B5CF6);
        }
    }

    private static void addCategory(AppDatabase db, String name, String type, int color) {
        Category c = new Category();
        c.name = name;
        c.type = type;
        c.color = color;
        c.isDefault = true;
        db.categoryDao().insert(c);
    }

    private static void rules(AppDatabase db, String categoryName, String... keywords) {
        Category c = db.categoryDao().find(categoryName, "EXPENSE");
        if (c == null) return;
        for (String k : keywords) {
            MerchantRule r = new MerchantRule();
            r.pattern = k;
            r.categoryId = c.id;
            r.priority = 10;
            db.merchantRuleDao().insert(r);
        }
    }

    private static void addSubject(AppDatabase db, String name, int color) {
        Subject s = new Subject();
        s.name = name;
        s.color = color;
        db.subjectDao().insert(s);
    }
}
