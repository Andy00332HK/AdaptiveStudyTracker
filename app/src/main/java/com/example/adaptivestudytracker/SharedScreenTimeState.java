package com.example.adaptivestudytracker;

import android.content.Context;
import android.os.SystemClock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Shared screen-time state used by Dashboard and Insights.
 * It applies one consistent update model: periodic UsageStats resync + smooth in-between increments.
 */
public final class SharedScreenTimeState {

    private static final long USAGE_RESYNC_INTERVAL_MS = 15000L;
    private static final int WEEK_DAYS = 7;

    private static final SharedScreenTimeState INSTANCE = new SharedScreenTimeState();

    private long displayedTotalScreenMs;
    private long displayedAppScreenMs;
    private long lastResyncElapsedMs;
    private long lastTickElapsedMs;
    private long lastSyncedDayStartMs = -1L;

    private LocalDate displayedWeekStart;
    private final List<Long> displayedWeekDailyMs = new ArrayList<>();

    private SharedScreenTimeState() {
    }

    public static SharedScreenTimeState getInstance() {
        return INSTANCE;
    }

    public synchronized void beginVisibleSession() {
        lastTickElapsedMs = SystemClock.elapsedRealtime();
        lastResyncElapsedMs = 0L;
    }

    public synchronized Snapshot update(Context context) {
        boolean hasUsageAccess = UsageStatsHelper.hasUsageAccess(context);
        if (!hasUsageAccess) {
            return snapshot(hasUsageAccess);
        }

        long nowElapsedMs = SystemClock.elapsedRealtime();
        if (lastTickElapsedMs == 0L) {
            lastTickElapsedMs = nowElapsedMs;
        }
        long deltaMs = Math.max(0L, nowElapsedMs - lastTickElapsedMs);
        lastTickElapsedMs = nowElapsedMs;

        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        long todayStartMs = getStartOfTodayMs();

        boolean shouldResync = lastResyncElapsedMs == 0L
                || (nowElapsedMs - lastResyncElapsedMs) >= USAGE_RESYNC_INTERVAL_MS
                || lastSyncedDayStartMs != todayStartMs
                || displayedWeekStart == null
                || !displayedWeekStart.equals(weekStart)
                || displayedWeekDailyMs.size() != WEEK_DAYS;

        if (shouldResync) {
            long usageTotalMs = UsageStatsHelper.getTodayTotalScreenTimeMs(context);
            long usageAppMs = UsageStatsHelper.getTodayAppScreenTimeMs(context);
            List<Long> usageWeekDailyMs = UsageStatsHelper.getCurrentWeekTotalScreenTimeMsMondayFirst(context);

            if (lastSyncedDayStartMs != todayStartMs) {
                displayedTotalScreenMs = usageTotalMs;
                displayedAppScreenMs = usageAppMs;
            } else {
                displayedTotalScreenMs = Math.max(displayedTotalScreenMs, usageTotalMs);
                displayedAppScreenMs = Math.max(displayedAppScreenMs, usageAppMs);
            }

            mergeWeekDaily(weekStart, usageWeekDailyMs);

            lastSyncedDayStartMs = todayStartMs;
            lastResyncElapsedMs = nowElapsedMs;
        } else {
            displayedTotalScreenMs += deltaMs;
            displayedAppScreenMs += deltaMs;
            incrementTodayWeekValue(weekStart, deltaMs);
        }

        return snapshot(hasUsageAccess);
    }

    private void mergeWeekDaily(LocalDate weekStart, List<Long> latestValues) {
        List<Long> normalized = normalizeWeekValues(latestValues);
        if (displayedWeekStart == null
                || !displayedWeekStart.equals(weekStart)
                || displayedWeekDailyMs.size() != WEEK_DAYS) {
            displayedWeekStart = weekStart;
            displayedWeekDailyMs.clear();
            displayedWeekDailyMs.addAll(normalized);
            return;
        }

        for (int i = 0; i < WEEK_DAYS; i++) {
            long merged = Math.max(displayedWeekDailyMs.get(i), normalized.get(i));
            displayedWeekDailyMs.set(i, merged);
        }
    }

    private void incrementTodayWeekValue(LocalDate weekStart, long deltaMs) {
        if (displayedWeekDailyMs.size() != WEEK_DAYS) {
            displayedWeekStart = weekStart;
            displayedWeekDailyMs.clear();
            for (int i = 0; i < WEEK_DAYS; i++) {
                displayedWeekDailyMs.add(0L);
            }
        }

        int todayIndex = (int) (LocalDate.now().toEpochDay() - weekStart.toEpochDay());
        if (todayIndex >= 0 && todayIndex < displayedWeekDailyMs.size()) {
            displayedWeekDailyMs.set(todayIndex, displayedWeekDailyMs.get(todayIndex) + deltaMs);
        }
    }

    private List<Long> normalizeWeekValues(List<Long> values) {
        List<Long> normalized = new ArrayList<>(WEEK_DAYS);
        for (int i = 0; i < WEEK_DAYS; i++) {
            long value = (values != null && i < values.size()) ? values.get(i) : 0L;
            normalized.add(Math.max(0L, value));
        }
        return normalized;
    }

    private Snapshot snapshot(boolean hasUsageAccess) {
        LocalDate weekStart = displayedWeekStart != null
                ? displayedWeekStart
                : LocalDate.now().with(DayOfWeek.MONDAY);
        List<Long> weekValues = normalizeWeekValues(displayedWeekDailyMs);
        return new Snapshot(hasUsageAccess, displayedTotalScreenMs, displayedAppScreenMs, weekStart, weekValues);
    }

    private long getStartOfTodayMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static final class Snapshot {
        public final boolean hasUsageAccess;
        public final long totalScreenMs;
        public final long appScreenMs;
        public final LocalDate weekStart;
        public final List<Long> weekScreenDailyMs;

        Snapshot(boolean hasUsageAccess, long totalScreenMs, long appScreenMs, LocalDate weekStart, List<Long> weekScreenDailyMs) {
            this.hasUsageAccess = hasUsageAccess;
            this.totalScreenMs = totalScreenMs;
            this.appScreenMs = appScreenMs;
            this.weekStart = weekStart;
            this.weekScreenDailyMs = weekScreenDailyMs;
        }
    }
}

