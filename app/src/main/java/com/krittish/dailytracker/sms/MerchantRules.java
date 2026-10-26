package com.krittish.dailytracker.sms;

import com.krittish.dailytracker.data.db.MerchantRule;

import java.util.List;
import java.util.Locale;

/** keyword -> category. Rules arrive sorted by priority, so the user's own rules (priority 100) win. */
public final class MerchantRules {
    private MerchantRules() {}

    /** @return category id, or 0 when no rule matches */
    public static long categorize(List<MerchantRule> rules, String merchant, String rawText) {
        String hay = ((merchant == null ? "" : merchant) + " " + (rawText == null ? "" : rawText)).toLowerCase(Locale.ROOT);
        for (MerchantRule r : rules) {
            if (!r.pattern.isEmpty() && hay.contains(r.pattern)) return r.categoryId;
        }
        return 0;
    }
}
