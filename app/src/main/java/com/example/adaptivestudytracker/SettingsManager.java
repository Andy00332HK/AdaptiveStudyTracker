package com.example.adaptivestudytracker;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {

    private static final String PREFS = "app_settings";

    private static final String KEY_USAGE_LIMIT_MIN = "usage_limit_minutes";
    private static final String KEY_SLEEP_START_HOUR = "sleep_start_hour";
    private static final String KEY_SLEEP_START_MIN = "sleep_start_min";
    private static final String KEY_SLEEP_END_HOUR = "sleep_end_hour";
    private static final String KEY_SLEEP_END_MIN = "sleep_end_min";
    private static final String KEY_REMINDERS_ENABLED = "reminders_enabled";
    private static final String KEY_USAGE_WARNINGS = "usage_warnings_enabled";
    private static final String KEY_USAGE_TRACKING = "usage_tracking_enabled";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /* ---------- daily max phone usage (minutes) ---------- */
    public int  getUsageLimitMinutes() { return prefs.getInt(KEY_USAGE_LIMIT_MIN, 120); }
    public void setUsageLimitMinutes(int minutes) { prefs.edit().putInt(KEY_USAGE_LIMIT_MIN, minutes).apply(); }

    /* ---------- sleep time ---------- */
    public int  getSleepStartHour() { return prefs.getInt(KEY_SLEEP_START_HOUR, 23); }
    public int  getSleepStartMinute() { return prefs.getInt(KEY_SLEEP_START_MIN, 0); }
    public void setSleepStart(int h, int m) {
        prefs.edit().putInt(KEY_SLEEP_START_HOUR, h)
                .putInt(KEY_SLEEP_START_MIN, m).apply();
    }

    /* ---------- wakeup time ---------- */
    public int  getSleepEndHour() { return prefs.getInt(KEY_SLEEP_END_HOUR, 7); }
    public int  getSleepEndMinute() { return prefs.getInt(KEY_SLEEP_END_MIN, 0); }
    public void setSleepEnd(int h, int m) {
        prefs.edit().putInt(KEY_SLEEP_END_HOUR, h)
                .putInt(KEY_SLEEP_END_MIN, m).apply();
    }

    /* ---------- switch ---------- */
    public boolean isRemindersEnabled() { return prefs.getBoolean(KEY_REMINDERS_ENABLED, true); }
    public void    setRemindersEnabled(boolean b) { prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, b).apply(); }

    public boolean isUsageWarningsEnabled() { return prefs.getBoolean(KEY_USAGE_WARNINGS, true); }
    public void    setUsageWarningsEnabled(boolean b) { prefs.edit().putBoolean(KEY_USAGE_WARNINGS, b).apply(); }

    public boolean isUsageTrackingEnabled() { return prefs.getBoolean(KEY_USAGE_TRACKING, false); }
    public void    setUsageTrackingEnabled(boolean b) { prefs.edit().putBoolean(KEY_USAGE_TRACKING, b).apply(); }
}