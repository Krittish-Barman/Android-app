package com.krittish.dailytracker.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.krittish.dailytracker.data.db.AppDatabase;
import com.krittish.dailytracker.data.db.FocusSession;
import com.krittish.dailytracker.data.db.Subject;
import com.krittish.dailytracker.util.AppExecutors;

import java.util.List;

public class FocusRepository {
    private final AppDatabase db;

    public FocusRepository(Context context) {
        db = AppDatabase.get(context.getApplicationContext());
    }

    public LiveData<List<Subject>> observeSubjects() { return db.subjectDao().observeAll(); }

    public LiveData<List<FocusSession>> observeRecent() { return db.focusDao().observeRecent(); }

    public LiveData<Integer> observeMinutes(long fromMillis, long toMillis) {
        return db.focusDao().observeMinutes(fromMillis, toMillis);
    }

    public void addSubject(String name, int color) {
        AppExecutors.DISK_IO.execute(() -> {
            Subject s = new Subject();
            s.name = name;
            s.color = color;
            db.subjectDao().insert(s);
        });
    }
}
