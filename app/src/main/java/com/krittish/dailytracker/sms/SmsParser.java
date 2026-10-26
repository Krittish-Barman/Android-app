package com.krittish.dailytracker.sms;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure Java SMS parser (no Android classes) - easy to unit-test.
 * These regexes are a STARTING POINT. Real bank SMS differ: collect 30+ real messages
 * (hide names/numbers), add them to SmsParserTest and tune until 90%+ are correct.
 */
public final class SmsParser {
    private SmsParser() {}

    public static final class Parsed {
        public String type;            // "EXPENSE" or "INCOME" (null when ignored)
        public long amountPaise;
        public String merchant = "";
        public String refNo;
        public String account;
        public boolean needsReview;    // both debit and credit words were found
        public String ignoreReason;    // null = a valid transaction
    }

    private static final Pattern AMOUNT = Pattern.compile(
            "(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)");
    private static final Pattern DEBIT = Pattern.compile(
            "(?i)\\b(debited|spent|paid|sent|withdrawn|purchase)\\b");
    private static final Pattern CREDIT = Pattern.compile(
            "(?i)\\b(credited|received|deposited|refund)\\b");
    private static final Pattern REF = Pattern.compile("\\b\\d{12}\\b");
    private static final Pattern ACCOUNT = Pattern.compile(
            "(?i)(?:a/c|acct?|account|card)\\s*(?:no\\.?)?\\s*[xX*]*\\s*(\\d{3,4})");
    private static final Pattern OTP = Pattern.compile(
            "(?i)\\botp\\b|one[- ]time password|do not share|don't share|never share");
    private static final Pattern FAILED = Pattern.compile(
            "(?i)\\b(failed|declined|unsuccessful|not processed|reversed)\\b");
    private static final Pattern PROMO = Pattern.compile(
            "(?i)\\b(offer|loan|apply now|pre-approved|cashback|discount|congratulations|win)\\b");
    private static final Pattern INFO_UPI = Pattern.compile("(?i)info:\\s*(\\S+)");
    private static final Pattern TO_AT = Pattern.compile(
            "(?i)\\b(?:to|at|by)\\s+(?:vpa\\s+)?([A-Za-z][A-Za-z0-9@._& -]{1,30})");
    private static final Pattern CUT = Pattern.compile(
            "(?i)\\s+(?:on|ref|via|from|using|upi|txn|avl|bal|for|dt|date|a/c|acct|if|call)\\b.*$");

    public static Parsed parse(String text) {
        Parsed p = new Parsed();
        String t = text == null ? "" : text.trim();

        if (OTP.matcher(t).find()) return ignore(p, "OTP");
        if (FAILED.matcher(t).find()) return ignore(p, "FAILED");

        boolean debit = DEBIT.matcher(t).find();
        boolean credit = CREDIT.matcher(t).find();
        if (!debit && !credit) return ignore(p, PROMO.matcher(t).find() ? "PROMO" : "NO_DIRECTION");
        if (PROMO.matcher(t).find() && !hasStrongWord(t)) return ignore(p, "PROMO");

        long paise = toPaise(t);
        if (paise <= 0) return ignore(p, "NO_AMOUNT");
        p.amountPaise = paise;

        if (debit && credit) {
            p.needsReview = true;
            Matcher d = DEBIT.matcher(t);
            Matcher c = CREDIT.matcher(t);
            d.find();
            c.find();
            p.type = d.start() <= c.start() ? "EXPENSE" : "INCOME";
        } else {
            p.type = debit ? "EXPENSE" : "INCOME";
        }

        Matcher ref = REF.matcher(t);
        if (ref.find()) p.refNo = ref.group();
        Matcher acc = ACCOUNT.matcher(t);
        if (acc.find()) p.account = acc.group(1);
        p.merchant = merchant(t, "EXPENSE".equals(p.type));
        return p;
    }

    private static boolean hasStrongWord(String t) {
        return Pattern.compile("(?i)\\b(debited|credited)\\b").matcher(t).find();
    }

    private static Parsed ignore(Parsed p, String reason) {
        p.ignoreReason = reason;
        return p;
    }

    /** Returns the amount in paise, or -1 if the SMS has no amount. Skips "Avl Bal" amounts. */
    public static long toPaise(String smsText) {
        Matcher m = AMOUNT.matcher(smsText);
        while (m.find()) {
            String before = smsText.substring(Math.max(0, m.start() - 22), m.start()).toLowerCase(Locale.ROOT);
            if (before.contains("bal") || before.contains("avl") || before.contains("available")
                    || before.contains("limit")) {
                continue;
            }
            BigDecimal rupees = new BigDecimal(m.group(1).replace(",", ""));
            return rupees.movePointRight(2).longValueExact();   // "180.50" -> 18050
        }
        return -1;
    }

    static String merchant(String t, boolean expense) {
        String name = null;
        Matcher info = INFO_UPI.matcher(t);
        if (info.find()) {
            for (String part : info.group(1).split("/")) {
                String s = part.trim();
                if (s.isEmpty() || s.equalsIgnoreCase("UPI") || s.matches("\\d+")) continue;
                name = s;
                break;
            }
        }
        if (name == null) {
            Matcher m = TO_AT.matcher(t);
            while (m.find()) {
                String cand = CUT.matcher(m.group(1)).replaceAll("").trim();
                if (cand.isEmpty() || cand.toLowerCase(Locale.ROOT).startsWith("upi")) continue;
                name = cand;
                break;
            }
        }
        if (name == null) return "";
        int at = name.indexOf('@');
        if (at > 0) name = name.substring(0, at);
        name = name.replaceAll("[^A-Za-z0-9&._ -]", "").replaceAll("[.\\-_ ]+$", "").trim();
        return name.matches("\\d*") ? "" : name;
    }
}
