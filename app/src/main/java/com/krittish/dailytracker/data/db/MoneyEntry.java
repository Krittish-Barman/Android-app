package com.krittish.dailytracker.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "money_entry",
        indices = {
                @Index(value = {"refNo"}, unique = true),
                @Index(value = {"dedupeHash"}, unique = true),
                @Index(value = {"timestamp"})
        })
public class MoneyEntry {
    @PrimaryKey(autoGenerate = true) public long id;
    public long amountPaise;
    @NonNull public String type = "EXPENSE";            // EXPENSE / INCOME / TRANSFER
    public long categoryId;                             // 0 = none
    public String merchant;
    public String note;
    public long timestamp;                              // epoch millis
    @NonNull public String source = "MANUAL";           // MANUAL / SMS
    @NonNull public String status = "CONFIRMED";        // CONFIRMED / PENDING / IGNORED
    public String accountTag;                           // last digits, e.g. "1234"
    public String refNo;                                // unique when present
    public String dedupeHash;                           // used when there is no refNo
    public String rawText;                              // only kept while PENDING

    public String title() {
        if (merchant != null && !merchant.trim().isEmpty()) return merchant;
        if (note != null && !note.trim().isEmpty()) return note;
        return "Unnamed";
    }
}
