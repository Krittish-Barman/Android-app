package com.krittish.dailytracker.sms;

import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;

import androidx.core.app.NotificationCompat;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.db.MoneyEntry;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.notifications.AlarmReceiver;
import com.krittish.dailytracker.notifications.NotificationHelper;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.MoneyFormat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * SMS pipeline: sender whitelist -> filter OTP/offers -> parse -> duplicate check -> categorize -> save PENDING.
 * Everything runs on the phone. Call on a background thread.
 */
public final class SmsProcessor {
    private SmsProcessor() {}

    /** @return true when a new PENDING entry was created */
    public static boolean process(Context ctx, String sender, String body, long timestamp, boolean notify) {
        SettingsRepository s = SettingsRepository.get(ctx);
        if (!senderAllowed(s.smsSenders(), sender)) return false;      // personal chats are never touched

        SmsParser.Parsed p = SmsParser.parse(body);
        if (p.ignoreReason != null) return false;

        AppDatabase db = AppDatabase.get(ctx);
        MoneyEntry e = new MoneyEntry();
        e.amountPaise = p.amountPaise;
        e.type = p.type;
        e.merchant = p.merchant;
        e.timestamp = timestamp;
        e.source = "SMS";
        e.status = "PENDING";
        e.accountTag = p.account;
        e.refNo = p.refNo;
        e.dedupeHash = p.refNo != null ? null : hash(p, timestamp);
        e.rawText = body;                                                // deleted after you confirm / ignore
        e.categoryId = "EXPENSE".equals(p.type)
                ? MerchantRules.categorize(db.merchantRuleDao().allNow(), p.merchant, body) : 0;

        long id = db.moneyDao().insertIgnore(e);
        if (id == -1) return false;                                      // duplicate (same ref no. / same hash)
        if (notify && s.notifOn(Constants.NT_DETECTED)) showDetected(ctx, db, id, e);
        return true;
    }

    static boolean senderAllowed(String allowList, String sender) {
        if (sender == null || allowList == null) return false;
        String s = sender.toUpperCase(Locale.ROOT);
        for (String token : allowList.split(",")) {
            String t = token.trim().toUpperCase(Locale.ROOT);
            if (!t.isEmpty() && s.contains(t)) return true;
        }
        return false;
    }

    /** amount + type + account + time (to the minute) + merchant */
    static String hash(SmsParser.Parsed p, long ts) {
        String raw = p.amountPaise + "|" + p.type + "|" + p.account + "|" + (ts / 60_000) + "|"
                + p.merchant.toLowerCase(Locale.ROOT);
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format(Locale.ROOT, "%02x", d[i]));
            return sb.toString();
        } catch (Exception ex) {
            return raw;
        }
    }

    private static void showDetected(Context ctx, AppDatabase db, long id, MoneyEntry e) {
        int pending = db.moneyDao().pendingCountNow();
        NotificationCompat.Builder b;
        if (pending > 1) {                                               // several at once are grouped
            b = NotificationHelper.builder(ctx, Constants.CH_MONEY, pending + " expenses detected", "Tap to review them")
                    .setContentIntent(NotificationHelper.openApp(ctx, 80, Constants.OPEN_REVIEW));
        } else {
            String who = e.merchant == null || e.merchant.isEmpty() ? "your account" : e.merchant;
            String verb = "INCOME".equals(e.type) ? "Received " : "Detected ";
            String prep = "INCOME".equals(e.type) ? " from " : " at ";
            Intent confirm = new Intent(ctx, AlarmReceiver.class).setAction(Constants.ACTION_CONFIRM_ENTRY)
                    .putExtra(Constants.EXTRA_ID, id);
            b = NotificationHelper.builder(ctx, Constants.CH_MONEY, verb + MoneyFormat.format(e.amountPaise) + prep + who,
                    "Tap to review")
                    .setContentIntent(NotificationHelper.openApp(ctx, 80, Constants.OPEN_REVIEW))
                    .addAction(0, "Confirm", PendingIntent.getBroadcast(ctx, Constants.REQ_CONFIRM, confirm,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                    .addAction(0, "Edit", NotificationHelper.openApp(ctx, 81, Constants.OPEN_REVIEW));
        }
        NotificationHelper.notify(ctx, Constants.NOTIF_DETECTED, b, true);
    }
}
