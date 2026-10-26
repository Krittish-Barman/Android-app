package com.krittish.dailytracker.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY id")
    LiveData<List<Category>> observeAll();

    @Query("SELECT * FROM category ORDER BY id")
    List<Category> allNow();

    @Query("SELECT * FROM category WHERE name = :name AND type = :type LIMIT 1")
    Category find(String name, String type);

    @Query("SELECT COUNT(*) FROM category")
    int count();

    @Insert
    long insert(Category c);
}
