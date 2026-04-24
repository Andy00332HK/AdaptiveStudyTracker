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
     * How long before a task due time to trigger a reminder.
     * If the task is due sooner than this, trigger as soon as possible (with a minimum delay).
     */
    private static final long REMINDER_OFFSET_MS = 30 * 60 * 1000L; // 30 minutes
    private static final long MIN_DELAY_MS = 5_000L; // minimum delay 5 seconds

    public static final String ACTION_TASK_REMINDER =
            "com.example.adaptivestudytracker.ACTION_TASK_REMINDER";
    public static final String EXTRA_TASK_ID    = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_TASK_DUE   = "extra_task_due";

    /** Schedule a reminder alarm for a single task */
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

        // Compute trigger time: due time minus offset, but at least now + MIN_DELAY_MS
        long triggerTime = task.dueTimeMillis - REMINDER_OFFSET_MS;
        if (triggerTime <= now) {
            // If due within offset, trigger as soon as possible
            triggerTime = now + MIN_DELAY_MS;
        }

        // If the due time has already passed, do not schedule
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
                    // No exact-alarm permission -> use setWindow with a 30s window
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
            // Fallback: use the basic set() call
            Log.w(TAG, "SecurityException, falling back to set(): " + e.getMessage());
            am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi);
        }
    }

    /** Cancel reminder for a single task */
    public static void cancelReminder(Context context, Task task) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(buildPendingIntent(context, task));
    }

    /** Reschedule reminders for all pending tasks */
    public static void rescheduleAll(Context context) {
        List<Task> tasks = new TaskStorage(context).loadTasks();
        for (Task t : tasks) {
            scheduleReminder(context, t);
        }
    }

    /** Cancel reminders for all tasks */
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