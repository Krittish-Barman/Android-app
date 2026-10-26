package com.krittish.dailytracker.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/** All simple settings live in SharedPreferences, behind this one class. */
public class SettingsRepository {
    private static SettingsRepository instance;
    private final SharedPreferences p;

    public static synchronized SettingsRepository get(Context c) {
        if (instance == null) instance = new SettingsRepository(c.getApplicationContext());
        return instance;
    }

    private SettingsRepository(Context c) {
        p = c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    // ---- onboarding / look ----
    public boolean onboardingDone() { return p.getBoolean("onboarding_done", false); }
    public void setOnboardingDone(boolean v) { p.edit().putBoolean("onboarding_done", v).apply(); }
    public String theme() { return p.getString("theme", "system"); }          // system / light / dark
    public void setTheme(String v) { p.edit().putString("theme", v).apply(); }

    // ---- notifications ----
    public boolean notifOn(String type) { return p.getBoolean("n_" + type, true); }
    public void setNotifOn(String type, boolean v) { p.edit().putBoolean("n_" + type, v).apply(); }
    public int leadMinutes() { return p.getInt("lead_min", 15); }
    public void setLeadMinutes(int v) { p.edit().putInt("lead_min", v).apply(); }
    public int morningTime() { return p.getInt("morning_time", 8 * 60); }
    public void setMorningTime(int v) { p.edit().putInt("morning_time", v).apply(); }
    public int eveningTime() { return p.getInt("evening_time", 21 * 60); }
    public void setEveningTime(int v) { p.edit().putInt("evening_time", v).apply(); }
    public boolean quietOn() { return p.getBoolean("quiet_on", true); }
    public void setQuietOn(boolean v) { p.edit().putBoolean("quiet_on", v).apply(); }
    public int quietStart() { return p.getInt("quiet_start", 23 * 60); }
    public void setQuietStart(int v) { p.edit().putInt("quiet_start", v).apply(); }
    public int quietEnd() { return p.getInt("quiet_end", 7 * 60); }
    public void setQuietEnd(int v) { p.edit().putInt("quiet_end", v).apply(); }
    public boolean upNextCard() { return p.getBoolean("up_next_card", false); }
    public void setUpNextCard(boolean v) { p.edit().putBoolean("up_next_card", v).apply(); }

    // ---- focus ----
    public int focusGoal() { return p.getInt("focus_goal", 120); }
    public void setFocusGoal(int v) { p.edit().putInt("focus_goal", v).apply(); }
    public int focusMinutes() { return p.getInt("focus_min", 25); }
    public int breakMinutes() { return p.getInt("break_min", 5); }
    public void setPreset(int focus, int brk) { p.edit().putInt("focus_min", focus).putInt("break_min", brk).apply(); }
    public boolean interruptedRule() { return p.getBoolean("interrupted_rule", false); }
    public void setInterruptedRule(boolean v) { p.edit().putBoolean("interrupted_rule", v).apply(); }

    // ---- money ----
    public long monthlyBudgetPaise() { return p.getLong("budget_paise", 600000L); }
    public void setMonthlyBudgetPaise(long v) { p.edit().putLong("budget_paise", v).apply(); }
    public boolean autoDetectSms() { return p.getBoolean("auto_sms", false); }
    public void setAutoDetectSms(boolean v) { p.edit().putBoolean("auto_sms", v).apply(); }
    /** Comma separated sender ids, e.g. "HDFCBK, SBIUPI". Empty = read nothing. */
    public String smsSenders() { return p.getString("sms_senders", ""); }
    public void setSmsSenders(String v) { p.edit().putString("sms_senders", v).apply(); }

    // ---- privacy ----
    public boolean lockOn() { return p.getBoolean("lock_on", false); }
    public void setLockOn(boolean v) { p.edit().putBoolean("lock_on", v).apply(); }

    // ---- one-time flags (budget alerts etc.) ----
    public boolean flag(String key) { return p.getBoolean("flag_" + key, false); }
    public void setFlag(String key) { p.edit().putBoolean("flag_" + key, true).apply(); }
}
