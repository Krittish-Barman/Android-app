package com.krittish.dailytracker.ui.tasks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.krittish.dailytracker.data.db.Task;
import com.krittish.dailytracker.data.repo.TaskRepository;

import java.util.List;

public class TasksViewModel extends AndroidViewModel {
    private final TaskRepository repo;
    public final LiveData<List<Task>> tasks;

    public TasksViewModel(@NonNull Application app) {
        super(app);
        repo = new TaskRepository(app);
        tasks = repo.observeAll();
    }

    public TaskRepository repo() { return repo; }
    public void setDone(Task t, boolean done) { repo.setDone(t, done); }
    public void togglePin(Task t, Runnable onLimit) { repo.togglePin(t, onLimit); }
    public void delete(Task t) { repo.delete(t); }
    public void restore(Task t) { repo.restore(t); }
}
