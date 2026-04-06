package com.example.adaptivestudytracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {

    public static final String CHANNEL_REMINDERS = "channel_reminders";
    public static final String CHANNEL_USAGE     = "channel_usage";
    public static final String CHANNEL_SERVICE   = "channel_service";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    context.getSystemService(NotificationManager.class);

            // 1) 任务提醒 — 高优先级，弹出横幅
            NotificationChannel reminders = new NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            reminders.setDescription("Notifications for upcoming task deadlines");

            // 2) 使用量警告 — ★ 改为 HIGH，这样会弹出横幅
            NotificationChannel usage = new NotificationChannel(
                    CHANNEL_USAGE,
                    "Usage Warnings",
                    NotificationManager.IMPORTANCE_HIGH);
            usage.setDescription("Alerts when screen time exceeds your limit");

            // 3) 前台服务 — 低优先级，常驻但不打扰
            NotificationChannel service = new NotificationChannel(
                    CHANNEL_SERVICE,
                    "Background Service",
                    NotificationManager.IMPORTANCE_LOW);
            service.setDescription("Persistent notification for usage tracking");

            nm.createNotificationChannel(reminders);
            nm.createNotificationChannel(usage);
            nm.createNotificationChannel(service);
        }
    }
}