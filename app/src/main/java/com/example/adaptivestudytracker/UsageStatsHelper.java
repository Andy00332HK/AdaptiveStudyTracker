package com.example.adaptivestudytracker;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Reads screen time data from the system UsageStatsManager API.
 * Requires the user to grant Usage Access via Settings.
 */
public class UsageStatsHelper {

    /** Returns true if the user has granted Usage Access to this app. */
    public static boolean hasUsageAccess(Context context) {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.getPackageName()
            );
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    /** Intent to open the system Usage Access settings page. */
    public static Intent getUsageAccessSettingsIntent() {
        return new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
    }

    public static long getTodayTotalScreenTimeMs(Context context) {
        return getTotalScreenTimeMsForRange(context, getStartOfTodayMs(), System.currentTimeMillis());
    }

    /** Foreground time for this specific app today. */
    public static long getTodayAppScreenTimeMs(Context context) {
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                getStartOfTodayMs(),
                System.currentTimeMillis()
        );
        if (stats != null) {
            for (UsageStats s : stats) {
                if (context.getPackageName().equals(s.getPackageName())) {
                    return s.getTotalTimeInForeground();
                }
            }
        }
        return 0;
    }

    public static long getTotalScreenTimeMsForRange(Context context, long startMs, long endMs) {
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startMs,
                endMs
        );
        long total = 0;
        if (stats != null) {
            for (UsageStats s : stats) {
                total += s.getTotalTimeInForeground();
            }
        }
        return total;
    }

    public static List<Long> getLastNDaysTotalScreenTimeMs(Context context, int days) {
        List<Long> values = new ArrayList<>();
        if (days <= 0) {
            return values;
        }
        LocalDate startDate = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            long start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (date.equals(LocalDate.now())) {
                end = System.currentTimeMillis();
            }
            values.add(getTotalScreenTimeMsForRange(context, start, end));
        }
        return values;
    }

    public static List<Long> getCurrentWeekTotalScreenTimeMsMondayFirst(Context context) {
        List<Long> values = new ArrayList<>();
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            long start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (date.equals(LocalDate.now())) {
                end = System.currentTimeMillis();
            }
            values.add(getTotalScreenTimeMsForRange(context, start, end));
        }
        return values;
    }

    private static long getStartOfTodayMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}