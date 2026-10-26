package com.krittish.dailytracker.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_session", indices = {@Index(value = {"startTime"})})
public class FocusSession {
    @PrimaryKey(autoGenerate = true) public long id;
    public long subjectId;
    public String subjectName;
    public int plannedMinutes;
    public int focusedMinutes;
    public long startTime;
    public long endTime;
    @NonNull public String status = "COMPLETED";        // COMPLETED / INTERRUPTED
}
