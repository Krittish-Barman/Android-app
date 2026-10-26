package com.krittish.dailytracker.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "habit")
public class Habit {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public String name = "";
    public int color;                                   // ARGB int
    @NonNull public String scheduleType = "DAILY";      // DAILY / WEEKDAYS / TIMES_PER_WEEK
    public int weekdaysMask;                            // bit 0 = Monday ... bit 6 = Sunday
    public int timesPerWeek = 3;
    public int targetValue;                             // 0 = simple check, else e.g. 20 (minutes)
    public String targetUnit;
    public int reminderTime = -1;                       // minutes after midnight, -1 = none
    public boolean archived;
    public long createdAt;
}
