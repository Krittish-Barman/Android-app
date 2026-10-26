package com.krittish.dailytracker.ui.focus;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.krittish.dailytracker.data.db.FocusSession;
import com.krittish.dailytracker.data.db.Subject;
import com.krittish.dailytracker.data.repo.FocusRepository;
import com.krittish.dailytracker.util.DateUtils;

import java.util.List;

public class FocusViewModel extends AndroidViewModel {
    private final FocusRepository repo;
    public final LiveData<List<Subject>> subjects;
    public final LiveData<List<FocusSession>> recent;
    public final MediatorLiveData<Integer> todayMinutes = new MediatorLiveData<>();
    private LiveData<Integer> src;
    private long boundDay = -1;

    public FocusViewModel(@NonNull Application app) {
        super(app);
        repo = new FocusRepository(app);
        subjects = repo.observeSubjects();
        recent = repo.observeRecent();
        bindDay();
    }

    /** Call in onResume so a new day starts from 0. */
    public void bindDay() {
        long day = DateUtils.today();
        if (day == boundDay) return;
        boundDay = day;
        if (src != null) todayMinutes.removeSource(src);
        src = repo.observeMinutes(DateUtils.startOfDayMillis(day), DateUtils.startOfDayMillis(day + 1));
        todayMinutes.addSource(src, todayMinutes::setValue);
    }

    public void addSubject(String name) { repo.addSubject(name, 0xFF0F766E); }
}
