package com.krittish.dailytracker.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        Habit.class, HabitLog.class, Category.class, MoneyEntry.class,
        MerchantRule.class, Subject.class, FocusSession.class, Task.class
}, version = 1, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    public abstract HabitDao habitDao();
    public abstract HabitLogDao habitLogDao();
    public abstract CategoryDao categoryDao();
    public abstract MoneyDao moneyDao();
    public abstract MerchantRuleDao merchantRuleDao();
    public abstract SubjectDao subjectDao();
    public abstract FocusDao focusDao();
    public abstract TaskDao taskDao();

    private static volatile AppDatabase instance;

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "daily_tracker.db").build();
                }
            }
        }
        return instance;
    }
}
