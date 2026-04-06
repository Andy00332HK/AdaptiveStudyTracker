package com.example.adaptivestudytracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        // 重新调度所有任务提醒
        TaskReminderManager.rescheduleAll(context);

        // 如果用户之前开启了使用量追踪服务，重新启动
        SettingsManager settings = new SettingsManager(context);
        if (settings.isUsageTrackingEnabled()) {
            PhoneUsageTrackingService.start(context);
        }
    }
}