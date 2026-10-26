package com.krittish.dailytracker.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Money is always stored as whole paise in a long (Rs 180.50 = 18050). */
public final class MoneyFormat {
    private MoneyFormat() {}

    /** 18050 -> "₹180.50", 15000000 -> "₹1,50,000" */
    public static String format(long paise) {
        boolean neg = paise < 0;
        long abs = Math.abs(paise);
        long rupees = abs / 100;
        long rest = abs % 100;
        String s = "₹" + group(rupees) + (rest == 0 ? "" : String.format(Locale.ENGLISH, ".%02d", rest));
        return neg ? "−" + s : s;
    }

    /** EXPENSE -> "−₹180", INCOME -> "+₹1,500", TRANSFER -> "₹2,000" */
    public static String signed(long paise, String type) {
        if ("EXPENSE".equals(type)) return "−" + format(paise);
        if ("INCOME".equals(type)) return "+" + format(paise);
        return format(paise);
    }

    /** Indian grouping: 1234567 -> "12,34,567" */
    static String group(long n) {
        String s = Long.toString(n);
        if (s.length() <= 3) return s;
        String last3 = s.substring(s.length() - 3);
        String head = s.substring(0, s.length() - 3);
        StringBuilder sb = new StringBuilder();
        for (int i = head.length(); i > 0; i -= 2) {
            int start = Math.max(0, i - 2);
            sb.insert(0, head.substring(start, i));
            if (start > 0) sb.insert(0, ",");
        }
        return sb + "," + last3;
    }

    /** "180.5" -> 18050. Returns -1 when the text is not a number. */
    public static long parseToPaise(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return -1;
            return new BigDecimal(text.trim().replace(",", ""))
                    .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return -1;
        }
    }
}
