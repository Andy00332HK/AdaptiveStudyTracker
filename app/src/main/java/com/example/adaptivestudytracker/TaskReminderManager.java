package com.example.adaptivestudytracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.List;

public class TaskReminderManager {

    private static final String TAG = "TaskReminderManager";

    /**
     * 提前提醒的时间。
     * 如果 DDL 距离现在不足这个时间，则立刻触发（延迟5秒确保系统能调度）。
     */
    private static final long REMINDER_OFFSET_MS = 30 * 60 * 1000L; // 1小时
    private static final long MIN_DELAY_MS = 5_000L; // 最小延迟5秒

    public static final String ACTION_TASK_REMINDER =
            "com.example.adaptivestudytracker.ACTION_TASK_REMINDER";
    public static final String EXTRA_TASK_ID    = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_TASK_DUE   = "extra_task_due";

    /** 为单个任务设置提醒闹钟 */
    public static void scheduleReminder(Context context, Task task) {
        SettingsManager settings = new SettingsManager(context);
        if (!settings.isRemindersEnabled()) {
            Log.d(TAG, "Reminders disabled globally, skipping: " + task.title);
            return;
        }
        if (Task.REMINDER_NONE.equals(task.reminderFrequency)) {
            Log.d(TAG, "Task reminder is NONE, skipping: " + task.title);
            return;
        }

        long now = System.currentTimeMillis();

        // 计算触发时间：DDL前1小时，但至少是"现在+5秒"
        long triggerTime = task.dueTimeMillis - REMINDER_OFFSET_MS;
        if (triggerTime <= now) {
            // DDL不足1小时，改为尽快触发
            triggerTime = now + MIN_DELAY_MS;
        }

        // 如果DDL已经过了，不设置提醒
        if (task.dueTimeMillis <= now) {
            Log.d(TAG, "Task already past due, skipping: " + task.title);
            return;
        }

        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildPendingIntent(context, task);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerTime, pi);
                    Log.d(TAG, "Scheduled EXACT alarm for: " + task.title);
                } else {
                    // 无精确闹钟权限 → 用 setWindow 给一个30秒窗口
                    am.setWindow(AlarmManager.RTC_WAKEUP,
                            triggerTime, 30_000L, pi);
                    Log.d(TAG, "Scheduled WINDOW alarm for: " + task.title
                            + " (no exact alarm permission)");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pi);
                Log.d(TAG, "Scheduled exact alarm (API 23+) for: " + task.title);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            }
        } catch (SecurityException e) {
            // 最后兜底：用最基本的 set()
            Log.w(TAG, "SecurityException, falling back to set(): " + e.getMessage());
            am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi);
        }
    }

    /** 取消单个任务的闹钟 */
    public static void cancelReminder(Context context, Task task) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(buildPendingIntent(context, task));
    }

    /** 重新调度所有未完成任务 */
    public static void rescheduleAll(Context context) {
        List<Task> tasks = new TaskStorage(context).loadTasks();
        for (Task t : tasks) {
            scheduleReminder(context, t);
        }
    }

    /** 取消所有任务的闹钟 */
    public static void cancelAll(Context context) {
        List<Task> tasks = new TaskStorage(context).loadTasks();
        for (Task t : tasks) {
            cancelReminder(context, t);
        }
    }

    private static PendingIntent buildPendingIntent(Context context, Task task) {
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction(ACTION_TASK_REMINDER);
        intent.putExtra(EXTRA_TASK_ID,    task.id);
        intent.putExtra(EXTRA_TASK_TITLE, task.title);
        intent.putExtra(EXTRA_TASK_DUE,   task.dueTimeMillis);
        return PendingIntent.getBroadcast(context,
                task.id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}