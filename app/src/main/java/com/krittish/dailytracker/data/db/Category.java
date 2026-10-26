package com.krittish.dailytracker.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category")
public class Category {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public String name = "";
    @NonNull public String type = "EXPENSE";            // EXPENSE / INCOME
    public int color;
    public boolean isDefault;
}
