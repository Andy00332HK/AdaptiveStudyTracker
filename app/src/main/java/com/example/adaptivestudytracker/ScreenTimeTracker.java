package com.example.adaptivestudytracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks focus time for today and keeps a lightweight per-day history for insights.
 */
public class ScreenTimeTracker {
    private static final String PREFS_NAME = "usage_metrics";
    private static final String KEY_FOCUS_SECONDS = "focus_seconds";
    private static final String KEY_FOCUS_DATE = "focus_date";
    private static final String KEY_FOCUS_HISTORY_PREFIX = "focus_day_";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SharedPreferences sharedPreferences;

    public ScreenTimeTracker(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private String today() {
        return LocalDate.now().format(DATE_FORMAT);
    }

    private String keyForDate(LocalDate date) {
        return KEY_FOCUS_HISTORY_PREFIX + date.format(DATE_FORMAT);
    }

    public long getTotalFocusSeconds() {
        String storedDate = sharedPreferences.getString(KEY_FOCUS_DATE, "");
        if (!today().equals(storedDate)) {
            return 0L;
        }
        return sharedPreferences.getLong(KEY_FOCUS_SECONDS, 0L);
    }

    public void incrementFocusSeconds(long deltaSeconds) {
        if (deltaSeconds <= 0) {
            return;
        }
        String todayString = today();
        String storedDate = sharedPreferences.getString(KEY_FOCUS_DATE, "");
        long current = todayString.equals(storedDate)
                ? sharedPreferences.getLong(KEY_FOCUS_SECONDS, 0L)
                : 0L;

        long updated = current + deltaSeconds;
        LocalDate localDate = LocalDate.parse(todayString, DATE_FORMAT);
        long dayTotal = sharedPreferences.getLong(keyForDate(localDate), 0L) + deltaSeconds;

        sharedPreferences.edit()
                .putLong(KEY_FOCUS_SECONDS, updated)
                .putString(KEY_FOCUS_DATE, todayString)
                .putLong(keyForDate(localDate), dayTotal)
                .apply();
    }

    public long getFocusSecondsForDate(LocalDate date) {
        if (date == null) {
            return 0L;
        }
        if (date.equals(LocalDate.now())) {
            return Math.max(sharedPreferences.getLong(keyForDate(date), 0L), getTotalFocusSeconds());
        }
        return sharedPreferences.getLong(keyForDate(date), 0L);
    }

    public List<Long> getLastNDaysFocusSeconds(int days) {
        List<Long> results = new ArrayList<>();
        if (days <= 0) {
            return results;
        }
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            results.add(getFocusSecondsForDate(start.plusDays(i)));
        }
        return results;
    }

    public List<Long> getCurrentWeekFocusSecondsMondayFirst() {
        List<Long> values = new ArrayList<>();
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            values.add(getFocusSecondsForDate(monday.plusDays(i)));
        }
        return values;
    }
}
