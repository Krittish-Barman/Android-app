package com.krittish.dailytracker.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FocusDao {
    @Insert
    void insert(FocusSession s);

    @Query("SELECT * FROM focus_session ORDER BY startTime DESC LIMIT 40")
    LiveData<List<FocusSession>> observeRecent();

    @Query("SELECT COALESCE(SUM(focusedMinutes), 0) FROM focus_session WHERE startTime >= :from AND startTime < :to")
    LiveData<Integer> observeMinutes(long from, long to);

    @Query("SELECT COALESCE(SUM(focusedMinutes), 0) FROM focus_session WHERE startTime >= :from AND startTime < :to")
    int minutesBetweenNow(long from, long to);

    @Query("SELECT * FROM focus_session WHERE startTime >= :from AND startTime < :to")
    List<FocusSession> between(long from, long to);
}
