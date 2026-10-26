package com.krittish.dailytracker.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "subject")
public class Subject {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public String name = "";
    public int color;
}
