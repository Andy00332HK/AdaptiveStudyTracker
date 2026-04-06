package com.example.adaptivestudytracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tracks focus time for today only.
 * When the date changes the counter is automatically reset.
 * Screen time (total device + app) is now read via UsageStatsHelper.
 */
public class ScreenTimeTracker {
    private static final String PREFS_NAME = "usage_metrics";
    private static final String KEY_FOCUS_SECONDS = "focus_seconds";
    private static final String KEY_FOCUS_DATE    = "focus_date";

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final SharedPreferences sharedPreferences;

    public ScreenTimeTracker(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Returns today's date as a string, e.g. "2026-04-06". */
    private String today() {
        return DATE_FORMAT.format(new Date());
    }

    /**
     * Returns the number of focus seconds recorded today.
     * Returns 0 if the stored date is not today (stale data from a previous day).
     */
    public long getTotalFocusSeconds() {
        String storedDate = sharedPreferences.getString(KEY_FOCUS_DATE, "");
        if (!today().equals(storedDate)) {
            return 0L;
        }
        return sharedPreferences.getLong(KEY_FOCUS_SECONDS, 0L);
    }

    /**
     * Adds {@code deltaSeconds} to today's focus counter.
     * Resets to zero first if the stored date is not today.
     */
    public void incrementFocusSeconds(long deltaSeconds) {
        String storedDate = sharedPreferences.getString(KEY_FOCUS_DATE, "");
        long current = today().equals(storedDate)
                ? sharedPreferences.getLong(KEY_FOCUS_SECONDS, 0L)
                : 0L;
        sharedPreferences.edit()
                .putLong(KEY_FOCUS_SECONDS, current + deltaSeconds)
                .putString(KEY_FOCUS_DATE, today())
                .apply();
    }
}
