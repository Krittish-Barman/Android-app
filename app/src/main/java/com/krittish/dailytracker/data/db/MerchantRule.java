package com.krittish.dailytracker.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "merchant_rule")
public class MerchantRule {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public String pattern = "";                // lower-case keyword
    public long categoryId;
    public int priority = 10;                           // user rules use 100 so they win
    public boolean isUserCreated;
}
