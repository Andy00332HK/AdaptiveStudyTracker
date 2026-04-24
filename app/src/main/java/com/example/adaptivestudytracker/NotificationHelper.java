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

            // 1) Task reminders — high importance so they show as banners
            NotificationChannel reminders = new NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            reminders.setDescription("Notifications for upcoming task deadlines");

            // 2) Usage warnings — high importance so they appear as banners
            NotificationChannel usage = new NotificationChannel(
                    CHANNEL_USAGE,
                    "Usage Warnings",
                    NotificationManager.IMPORTANCE_HIGH);
            usage.setDescription("Alerts when screen time exceeds your limit");

            // 3) Foreground service — low importance, persistent but unobtrusive
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