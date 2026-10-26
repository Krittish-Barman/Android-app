package com.krittish.dailytracker.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface HabitDao {
    @Query("SELECT * FROM habit WHERE archived = 0 ORDER BY id")
    LiveData<List<Habit>> observeActive();

    @Query("SELECT * FROM habit WHERE archived = 0 ORDER BY id")
    List<Habit> activeNow();

    @Query("SELECT * FROM habit ORDER BY id")
    List<Habit> allNow();

    @Query("SELECT * FROM habit WHERE id = :id")
    Habit getById(long id);

    @Query("SELECT * FROM habit WHERE id = :id")
    LiveData<Habit> observeById(long id);

    @Insert
    long insert(Habit h);

    @Update
    void update(Habit h);
}
