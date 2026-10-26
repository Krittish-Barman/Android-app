package com.krittish.dailytracker.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HabitLogDao {
    @Query("SELECT * FROM habit_log WHERE habitId = :habitId ORDER BY date")
    LiveData<List<HabitLog>> observeForHabit(long habitId);

    @Query("SELECT * FROM habit_log")
    LiveData<List<HabitLog>> observeAll();

    @Query("SELECT * FROM habit_log")
    List<HabitLog> allNow();

    @Query("SELECT * FROM habit_log WHERE habitId = :habitId AND date = :date")
    HabitLog get(long habitId, long date);

    @Query("SELECT * FROM habit_log WHERE date >= :from AND date <= :to")
    List<HabitLog> between(long from, long to);

    @Query("SELECT COUNT(*) FROM habit_log WHERE status = 'DONE' AND date >= :from AND date <= :to")
    int doneCountBetween(long from, long to);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(HabitLog log);

    @Query("DELETE FROM habit_log WHERE habitId = :habitId AND date = :date")
    void delete(long habitId, long date);
}
