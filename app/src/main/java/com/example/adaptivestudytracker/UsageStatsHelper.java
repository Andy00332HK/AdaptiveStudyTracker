package com.example.adaptivestudytracker;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import java.util.Calendar;
import java.util.List;

/**
 * Reads today's screen time data from the system UsageStatsManager API.
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

    /**
     * Sum of all apps foreground time today.
     * UsageStatsManager foreground time is the best available proxy for
     * total screen-on time without a system privilege.
     */
    public static long getTodayTotalScreenTimeMs(Context context) {
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                getStartOfTodayMs(),
                System.currentTimeMillis()
        );
        long total = 0;
        if (stats != null) {
            for (UsageStats s : stats) {
                total += s.getTotalTimeInForeground();
            }
        }
        return total;
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

    private static long getStartOfTodayMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}

