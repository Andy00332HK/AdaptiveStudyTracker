package com.example.adaptivestudytracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        // Reschedule all task reminders after boot
        TaskReminderManager.rescheduleAll(context);

        // Restart usage tracking service if it was enabled before reboot
        SettingsManager settings = new SettingsManager(context);
        if (settings.isUsageTrackingEnabled()) {
            PhoneUsageTrackingService.start(context);
        }
    }
}