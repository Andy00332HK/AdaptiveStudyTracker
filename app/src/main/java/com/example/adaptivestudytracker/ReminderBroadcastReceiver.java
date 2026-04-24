package com.example.adaptivestudytracker;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_MARK_DONE =
            "com.example.adaptivestudytracker.ACTION_MARK_DONE";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Ensure notification channels exist
        NotificationHelper.createChannels(context);

        String action    = intent.getAction();
        String taskId    = intent.getStringExtra(TaskReminderManager.EXTRA_TASK_ID);
        String taskTitle = intent.getStringExtra(TaskReminderManager.EXTRA_TASK_TITLE);
        long   taskDue   = intent.getLongExtra(TaskReminderManager.EXTRA_TASK_DUE, 0);

        /* ---- 'Mark as Done' action clicked ---- */
        if (ACTION_MARK_DONE.equals(action) && taskId != null) {
            TaskStorage storage = new TaskStorage(context);
            List<Task> tasks = storage.loadTasks();
            Task target = null;
            for (Task t : tasks) {
                if (taskId.equals(t.id)) { target = t; break; }
            }
            if (target != null) {
                storage.addCompletedTask(target);
                storage.deleteTaskById(taskId);
            }
            NotificationManagerCompat.from(context).cancel(taskId.hashCode());
            return;
        }

        /* ---- Regular reminder: show notification ---- */
        if (taskTitle == null) taskTitle = "Task";

        String dueText = DateFormat
                .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(taskDue));

        // "Mark as Done" Action
        Intent doneIntent = new Intent(context, ReminderBroadcastReceiver.class);
        doneIntent.setAction(ACTION_MARK_DONE);
        doneIntent.putExtra(TaskReminderManager.EXTRA_TASK_ID, taskId);
        PendingIntent donePi = PendingIntent.getBroadcast(context,
                ("done_" + taskId).hashCode(), doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Tap notification to open the app
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(context,
                0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDERS)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("⏰ " + taskTitle)
                        .setContentText("Due: " + dueText)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Your task \"" + taskTitle
                                        + "\" is due at " + dueText
                                        + ". Stay on track!"))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(openPi)
                        .addAction(android.R.drawable.ic_menu_save,
                                "Mark as Done", donePi);

        try {
            NotificationManagerCompat.from(context)
                    .notify(taskId.hashCode(), builder.build());
        } catch (SecurityException ignored) {
            // Missing POST_NOTIFICATIONS permission on Android 13+
        }
    }
}