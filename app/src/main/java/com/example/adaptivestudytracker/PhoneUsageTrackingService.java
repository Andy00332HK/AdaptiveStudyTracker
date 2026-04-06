package com.example.adaptivestudytracker;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class PhoneUsageTrackingService extends Service {

    private static final String TAG = "UsageTrackingService";
    private static final int  NOTIF_ID_SERVICE = 9001;
    private static final int  NOTIF_ID_WARNING = 9002;
    /** 每60秒检查一次（方便测试；上线后可改回15分钟） */
    private static final long CHECK_INTERVAL   = 60 * 1000L;

    private Handler  handler;
    private Runnable checkRunnable;
    private boolean  warningShownToday = false;

    public static void start(Context context) {
        Intent intent = new Intent(context, PhoneUsageTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, PhoneUsageTrackingService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);

        handler = new Handler(Looper.getMainLooper());
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkUsageAgainstLimit();
                checkSleepTime();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID_SERVICE, buildForegroundNotification());
        handler.removeCallbacks(checkRunnable);
        handler.post(checkRunnable);
        Log.d(TAG, "Usage tracking service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkRunnable);
        Log.d(TAG, "Usage tracking service stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    /* ========== 检查使用量 ========== */
    private void checkUsageAgainstLimit() {
        if (!UsageStatsHelper.hasUsageAccess(this)) {
            Log.w(TAG, "No usage access permission");
            return;
        }

        SettingsManager settings = new SettingsManager(this);
        if (!settings.isUsageWarningsEnabled()) return;

        long usedMs  = UsageStatsHelper.getTodayTotalScreenTimeMs(this);
        long limitMs = settings.getUsageLimitMinutes() * 60_000L;

        long usedMin  = usedMs  / 60_000;
        long limitMin = limitMs / 60_000;

        Log.d(TAG, "Usage check: used=" + usedMin + "m, limit=" + limitMin + "m");

        if (usedMs >= limitMs && !warningShownToday) {
            showUsageWarningNotification(usedMin, limitMin);
            warningShownToday = true;
        } else if (usedMs < limitMs) {
            warningShownToday = false;
        }
    }

    /* ========== 检查睡眠时间 ========== */
    private void checkSleepTime() {
        SettingsManager settings = new SettingsManager(this);
        if (!settings.isRemindersEnabled()) return;

        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentHour   = now.get(java.util.Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(java.util.Calendar.MINUTE);
        int currentTotal  = currentHour * 60 + currentMinute;

        int sleepStartTotal = settings.getSleepStartHour() * 60
                + settings.getSleepStartMinute();

        // 在就寝时间的前后2分钟窗口内触发提醒（因为每分钟检查一次）
        int diff = currentTotal - sleepStartTotal;
        if (diff >= 0 && diff <= 2) {
            showSleepReminderNotification(settings.getSleepStartHour(),
                    settings.getSleepStartMinute());
        }
    }

    /* ========== 通知 ========== */
    private Notification buildForegroundNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_SERVICE)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentTitle("Usage Tracking Active")
                .setContentText("Monitoring your daily screen time")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void showUsageWarningNotification(long usedMin, long limitMin) {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new NotificationCompat.Builder(
                this, NotificationHelper.CHANNEL_USAGE)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("📱 Screen Time Limit Exceeded!")
                .setContentText(String.format(
                        "Used: %dm / Limit: %dm — Take a break!",
                        usedMin, limitMin))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();

        try {
            NotificationManagerCompat.from(this)
                    .notify(NOTIF_ID_WARNING, notif);
            Log.d(TAG, "Usage warning notification shown");
        } catch (SecurityException e) {
            Log.e(TAG, "Cannot show notification: " + e.getMessage());
        }
    }

    private static final int NOTIF_ID_SLEEP = 9003;

    private void showSleepReminderNotification(int hour, int minute) {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new NotificationCompat.Builder(
                this, NotificationHelper.CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("😴 Time for Bed!")
                .setContentText(String.format(
                        "It's %02d:%02d — Your scheduled bedtime. Get some rest!",
                        hour, minute))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();

        try {
            NotificationManagerCompat.from(this)
                    .notify(NOTIF_ID_SLEEP, notif);
            Log.d(TAG, "Sleep reminder notification shown");
        } catch (SecurityException e) {
            Log.e(TAG, "Cannot show sleep notification: " + e.getMessage());
        }
    }
}