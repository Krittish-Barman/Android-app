package com.krittish.dailytracker.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM task ORDER BY done, CASE WHEN dueAt = 0 THEN 1 ELSE 0 END, dueAt, priority")
    LiveData<List<Task>> observeAll();

    @Query("SELECT * FROM task")
    List<Task> allNow();

    @Query("SELECT * FROM task WHERE id = :id")
    Task getById(long id);

    @Query("SELECT COUNT(*) FROM task WHERE pinnedDate = :day")
    int pinnedCountNow(long day);

    @Query("SELECT COUNT(*) FROM task WHERE pinnedDate = :day AND done = 0")
    int pinnedOpenCountNow(long day);

    /** Unfinished tasks pinned on an earlier day move to today. */
    @Query("UPDATE task SET pinnedDate = :today WHERE pinnedDate >= 0 AND pinnedDate < :today AND done = 0")
    void rollOver(long today);

    @Insert
    long insert(Task t);

    @Update
    void update(Task t);

    @Delete
    void delete(Task t);
}
