package com.example.adaptivestudytracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * No longer used for screen time tracking.
 * Screen time is now read from UsageStatsManager via UsageStatsHelper.
 */
public class ScreenTimeBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // No-op: replaced by UsageStatsHelper
    }
}
