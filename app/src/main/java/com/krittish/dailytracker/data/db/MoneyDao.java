package com.krittish.dailytracker.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MoneyDao {
    @Query("SELECT * FROM money_entry WHERE status = 'CONFIRMED' AND timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    LiveData<List<MoneyEntry>> observeConfirmed(long from, long to);

    @Query("SELECT * FROM money_entry WHERE status = 'PENDING' ORDER BY timestamp DESC")
    LiveData<List<MoneyEntry>> observePending();

    @Query("SELECT COUNT(*) FROM money_entry WHERE status = 'PENDING'")
    LiveData<Integer> observePendingCount();

    @Query("SELECT * FROM money_entry WHERE status = 'PENDING' ORDER BY timestamp DESC")
    List<MoneyEntry> pendingNow();

    @Query("SELECT COUNT(*) FROM money_entry WHERE status = 'PENDING'")
    int pendingCountNow();

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM money_entry WHERE status = 'CONFIRMED' AND type = 'EXPENSE' AND timestamp >= :from AND timestamp < :to")
    LiveData<Long> observeExpenseSum(long from, long to);

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM money_entry WHERE status = 'CONFIRMED' AND type = 'EXPENSE' AND timestamp >= :from AND timestamp < :to")
    long expenseSumNow(long from, long to);

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM money_entry WHERE status = 'CONFIRMED' AND type = 'INCOME' AND timestamp >= :from AND timestamp < :to")
    long incomeSumNow(long from, long to);

    @Query("SELECT categoryId AS categoryId, SUM(amountPaise) AS total FROM money_entry WHERE status = 'CONFIRMED' AND type = 'EXPENSE' AND timestamp >= :from AND timestamp < :to GROUP BY categoryId ORDER BY total DESC")
    List<CategoryTotal> expenseByCategoryNow(long from, long to);

    @Query("SELECT * FROM money_entry WHERE id = :id")
    MoneyEntry getById(long id);

    /** Returns -1 when refNo / dedupeHash already exists (= duplicate SMS). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(MoneyEntry e);

    @Update
    void update(MoneyEntry e);

    @Delete
    void delete(MoneyEntry e);

    @Query("UPDATE money_entry SET status = :status, rawText = NULL WHERE id = :id")
    void setStatus(long id, String status);

    @Query("UPDATE money_entry SET categoryId = :categoryId WHERE id = :id")
    void setCategory(long id, long categoryId);

    @Query("UPDATE money_entry SET type = 'TRANSFER', status = 'CONFIRMED', categoryId = 0, rawText = NULL WHERE id = :id")
    void markTransfer(long id);
}
