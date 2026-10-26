package com.krittish.dailytracker.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.krittish.dailytracker.data.db.Task;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.data.repo.FocusRepository;
import com.krittish.dailytracker.data.repo.HabitRepository;
import com.krittish.dailytracker.data.repo.MoneyRepository;
import com.krittish.dailytracker.data.repo.TaskRepository;
import com.krittish.dailytracker.domain.HabitRow;
import com.krittish.dailytracker.domain.UpNextProvider;
import com.krittish.dailytracker.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/** Combines habits, tasks, money and focus into one "how am I doing today?" state. */
public class HomeViewModel extends AndroidViewModel {

    public static class State {
        public List<HabitRow> dueHabits = new ArrayList<>();
        public int done;
        public int total;
        public int percent;
        public int bestStreak;
        public long spentToday;
        public int studyMin;
        public int goalMin;
        public int tasksDone;
        public int tasksTotal;
        public List<Task> pinned = new ArrayList<>();
        public UpNextProvider.UpNext upNext;
    }

    private final HabitRepository habits;
    private final TaskRepository tasks;
    private final MoneyRepository money;
    private final FocusRepository focus;
    private final SettingsRepository settings;

    public final HabitRepository.RowsLiveData rows;
    private final MediatorLiveData<State> state = new MediatorLiveData<>();

    private List<HabitRow> lastRows;
    private List<Task> lastTasks;
    private long spent;
    private int study;
    private LiveData<Long> spentSrc;
    private LiveData<Integer> studySrc;
    private long boundDay = -1;

    public HomeViewModel(@NonNull Application app) {
        super(app);
        habits = new HabitRepository(app);
        tasks = new TaskRepository(app);
        money = new MoneyRepository(app);
        focus = new FocusRepository(app);
        settings = SettingsRepository.get(app);

        rows = habits.observeRows();
        state.addSource(rows, r -> { lastRows = r; recompute(); });
        state.addSource(tasks.observeAll(), t -> { lastTasks = t; recompute(); });
        bindDay();
    }

    public LiveData<State> state() { return state; }
    public TaskRepository taskRepo() { return tasks; }

    public void setDone(long habitId, boolean done) { habits.setDone(habitId, DateUtils.today(), done); }

    /** Call in onResume: picks up a new day. */
    public void onResume() {
        bindDay();
        rows.recompute();
        recompute();
    }

    private void bindDay() {
        long day = DateUtils.today();
        if (day == boundDay) return;
        boundDay = day;
        if (spentSrc != null) state.removeSource(spentSrc);
        if (studySrc != null) state.removeSource(studySrc);
        long from = DateUtils.startOfDayMillis(day);
        long to = DateUtils.startOfDayMillis(day + 1);
        spentSrc = money.observeExpenseSum(from, to);
        studySrc = focus.observeMinutes(from, to);
        state.addSource(spentSrc, v -> { spent = v == null ? 0 : v; recompute(); });
        state.addSource(studySrc, v -> { study = v == null ? 0 : v; recompute(); });
    }

    private void recompute() {
        if (lastRows == null || lastTasks == null) return;
        long today = DateUtils.today();
        State s = new State();
        for (HabitRow r : lastRows) {
            if (!r.dueToday) continue;
            s.dueHabits.add(r);
            s.total++;
            if (r.doneToday) s.done++;
        }
        for (HabitRow r : lastRows) s.bestStreak = Math.max(s.bestStreak, r.bestStreak);
        s.percent = s.total == 0 ? 0 : s.done * 100 / s.total;
        s.spentToday = spent;
        s.studyMin = study;
        s.goalMin = settings.focusGoal();
        for (Task t : lastTasks) {
            if (t.pinnedDate == today) {
                s.pinned.add(t);
                s.tasksTotal++;
                if (t.done) s.tasksDone++;
            }
        }
        s.upNext = UpNextProvider.compute(lastRows, lastTasks, System.currentTimeMillis());
        state.setValue(s);
    }
}
