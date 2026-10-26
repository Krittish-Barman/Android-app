package com.krittish.dailytracker.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MerchantRuleDao {
    @Query("SELECT * FROM merchant_rule ORDER BY priority DESC, id DESC")
    List<MerchantRule> allNow();

    @Query("SELECT COUNT(*) FROM merchant_rule")
    int count();

    @Insert
    void insert(MerchantRule r);
}
