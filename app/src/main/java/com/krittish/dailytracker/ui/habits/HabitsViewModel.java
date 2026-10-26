package com.krittish.dailytracker.ui.habits;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.krittish.dailytracker.data.db.Habit;
import com.krittish.dailytracker.data.db.HabitLog;
import com.krittish.dailytracker.data.repo.HabitRepository;
import com.krittish.dailytracker.util.Callback;
import com.krittish.dailytracker.util.DateUtils;

import java.util.List;

public class HabitsViewModel extends AndroidViewModel {
    private final HabitRepository repo;
    public final HabitRepository.RowsLiveData rows;

    public HabitsViewModel(@NonNull Application app) {
        super(app);
        repo = new HabitRepository(app);
        rows = repo.observeRows();
    }

    public void setDoneToday(long habitId, boolean done) { repo.setDone(habitId, DateUtils.today(), done); }
    public void setDone(long habitId, long day, boolean done) { repo.setDone(habitId, day, done); }
    public void skip(long habitId, long day) { repo.skip(habitId, day); }
    public void save(Habit h) { repo.save(h); }
    public void archive(long id) { repo.archive(id); }
    public LiveData<Habit> observeHabit(long id) { return repo.observeHabit(id); }
    public LiveData<List<HabitLog>> observeLogs(long id) { return repo.observeLogs(id); }
    public void load(long id, Callback<Habit> cb) { repo.getHabit(id, cb); }
}
