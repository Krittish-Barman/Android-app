package com.krittish.dailytracker.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "task")
public class Task {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public String title = "";
    public String note;
    public long dueAt;                                  // epoch millis, 0 = no due time
    public int priority = 2;                            // 1 high, 2 medium, 3 low
    public boolean done;
    public long doneAt;
    public long pinnedDate = -1;                        // epoch day it is pinned as "top 3", -1 = not pinned
    public String repeat;
}
