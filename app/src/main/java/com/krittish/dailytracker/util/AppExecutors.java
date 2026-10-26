package com.krittish.dailytracker.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One place for background work. Room refuses to run on the main thread. */
public final class AppExecutors {
    public static final ExecutorService DISK_IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {}

    public static void main(Runnable r) {
        MAIN.post(r);
    }
}
