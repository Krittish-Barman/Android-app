package com.krittish.dailytracker.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.util.AppExecutors;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        final Context app = context.getApplicationContext();
        if (!SettingsRepository.get(app).autoDetectSms()) return;

        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;
        final String sender = parts[0].getOriginatingAddress();
        final long time = parts[0].getTimestampMillis();
        StringBuilder sb = new StringBuilder();
        for (SmsMessage m : parts) sb.append(m.getMessageBody());      // long SMS arrive in parts
        final String body = sb.toString();

        final PendingResult result = goAsync();
        AppExecutors.DISK_IO.execute(() -> {
            try {
                SmsProcessor.process(app, sender, body, time > 0 ? time : System.currentTimeMillis(), true);
            } catch (Exception ignored) {
                // never crash a receiver
            } finally {
                result.finish();
            }
        });
    }
}
