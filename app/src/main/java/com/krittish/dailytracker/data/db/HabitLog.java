package com.krittish.dailytracker.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "habit_log",
        indices = {@Index(value = {"habitId", "date"}, unique = true)},
        foreignKeys = @ForeignKey(entity = Habit.class, parentColumns = "id",
                childColumns = "habitId", onDelete = ForeignKey.CASCADE))
public class HabitLog {
    @PrimaryKey(autoGenerate = true) public long id;
    public long habitId;
    public long date;                                   // epoch day
    public int value;
    @NonNull public String status = "DONE";             // DONE / SKIPPED
    public String note;
}
