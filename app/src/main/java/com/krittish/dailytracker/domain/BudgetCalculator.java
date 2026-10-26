package com.krittish.dailytracker.domain;

import com.krittish.dailytracker.util.MoneyFormat;

public final class BudgetCalculator {
    private BudgetCalculator() {}

    public static int percentUsed(long spentPaise, long limitPaise) {
        if (limitPaise <= 0) return 0;
        return (int) Math.min(999, spentPaise * 100 / limitPaise);
    }

    /** Never colour alone: always show text such as "70% used · ₹1,800 left". */
    public static String statusText(long spentPaise, long limitPaise) {
        if (limitPaise <= 0) return "No budget set";
        int pct = percentUsed(spentPaise, limitPaise);
        long left = limitPaise - spentPaise;
        if (left >= 0) return pct + "% used · " + MoneyFormat.format(left) + " left";
        return pct + "% used · over by " + MoneyFormat.format(-left);
    }
}
