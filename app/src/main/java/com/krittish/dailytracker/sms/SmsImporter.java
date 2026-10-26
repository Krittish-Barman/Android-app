package com.krittish.dailytracker.sms;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import com.krittish.dailytracker.util.AppExecutors;

/** One-time import of the last N days of old SMS. Everything goes to the PENDING list. */
public final class SmsImporter {
    private SmsImporter() {}

    public interface Progress {
        void onProgress(int done, int total);
        void onFinished(int created);
    }

    /** Needs READ_SMS. Runs on the background executor; callbacks arrive on the main thread. */
    public static void importLastDays(Context context, int days, Progress cb) {
        final Context app = context.getApplicationContext();
        AppExecutors.DISK_IO.execute(() -> {
            long since = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000;
            int created = 0;
            try (Cursor c = app.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI,
                    new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE},
                    Telephony.Sms.DATE + " >= ?", new String[]{String.valueOf(since)},
                    Telephony.Sms.DATE + " ASC")) {
                int total = c == null ? 0 : c.getCount();
                int done = 0;
                while (c != null && c.moveToNext()) {
                    if (SmsProcessor.process(app, c.getString(0), c.getString(1), c.getLong(2), false)) created++;
                    done++;
                    if (done % 20 == 0 || done == total) {
                        final int d = done, t = total;
                        AppExecutors.main(() -> cb.onProgress(d, t));
                    }
                }
            } catch (SecurityException e) {
                // READ_SMS not granted
            }
            final int result = created;
            AppExecutors.main(() -> cb.onFinished(result));
        });
    }
}
