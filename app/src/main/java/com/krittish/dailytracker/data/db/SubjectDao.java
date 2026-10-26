package com.krittish.dailytracker.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SubjectDao {
    @Query("SELECT * FROM subject ORDER BY id")
    LiveData<List<Subject>> observeAll();

    @Query("SELECT COUNT(*) FROM subject")
    int count();

    @Insert
    long insert(Subject s);
}
