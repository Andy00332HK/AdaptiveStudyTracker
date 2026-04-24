package com.example.adaptivestudytracker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarImportHelper {

    private static final String TAG = "CalendarImportHelper";

    /**
     * Represents a calendar event (not yet converted into a Task).
     */
    public static class CalendarEvent {
        public long   eventId;
        public String title;
        public String description;
        public long   dtStart;   // start time (ms)
        public long   dtEnd;     // end time (ms)
        public String calendarName;
        public boolean selected = false; // whether the user selected this event

        public CalendarEvent(long eventId, String title, String description,
                             long dtStart, long dtEnd, String calendarName) {
            this.eventId = eventId;
            this.title = title;
            this.description = description;
            this.dtStart = dtStart;
            this.dtEnd = dtEnd;
            this.calendarName = calendarName;
        }
    }

    /**
     * Read upcoming calendar events for the next `days` days.
     * Uses the system Calendar content provider (CalendarContract.Events).
     *
     * @param context requires READ_CALENDAR permission
     * @return list of events, empty if none or permission is missing
     */
    public static List<CalendarEvent> getUpcomingEvents(Context context, int days) {
        List<CalendarEvent> events = new ArrayList<>();

        // Query range: now -> now + days
        long now = System.currentTimeMillis();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.DAY_OF_YEAR, days);
        long endTime = end.getTimeInMillis();

        // Projection: columns to query
        String[] projection = {
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.CALENDAR_DISPLAY_NAME
        };

        // Selection: events with start between now and endTime
        String selection = CalendarContract.Events.DTSTART + " >= ? AND "
                + CalendarContract.Events.DTSTART + " <= ?";
        String[] selectionArgs = {
                String.valueOf(now),
                String.valueOf(endTime)
        };

        // Sort by start time
        String sortOrder = CalendarContract.Events.DTSTART + " ASC";

        Uri uri = CalendarContract.Events.CONTENT_URI;

        try (Cursor cursor = context.getContentResolver().query(
                uri, projection, selection, selectionArgs, sortOrder)) {

            if (cursor == null) {
                Log.w(TAG, "Cursor is null — no calendar access?");
                return events;
            }

            int idIdx    = cursor.getColumnIndex(CalendarContract.Events._ID);
            int titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE);
            int descIdx  = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION);
            int startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART);
            int endIdx   = cursor.getColumnIndex(CalendarContract.Events.DTEND);
            int calIdx   = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME);

            while (cursor.moveToNext()) {
                long   eventId     = cursor.getLong(idIdx);
                String title       = cursor.getString(titleIdx);
                String description = cursor.getString(descIdx);
                long   dtStart     = cursor.getLong(startIdx);
                long   dtEnd       = endIdx >= 0 ? cursor.getLong(endIdx) : dtStart;
                String calName     = cursor.getString(calIdx);

                if (title == null || title.trim().isEmpty()) {
                    title = "(No Title)";
                }

                events.add(new CalendarEvent(
                        eventId, title, description, dtStart, dtEnd, calName));
            }

            Log.d(TAG, "Found " + events.size() + " upcoming events");

        } catch (SecurityException e) {
            Log.e(TAG, "No READ_CALENDAR permission: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error reading calendar: " + e.getMessage());
        }

        return events;
    }

    /**
     * Convert a CalendarEvent into a Task instance.
     */
    public static Task eventToTask(CalendarEvent event) {
        Task task = new Task();
        task.id = "cal_" + event.eventId + "_" + System.currentTimeMillis();
        task.title = event.title;
        task.category = Task.CATEGORY_STUDY; // default category; user can edit later
        task.dueTimeMillis = event.dtEnd > 0 ? event.dtEnd : event.dtStart;
        task.reminderFrequency = Task.REMINDER_DAILY;
        task.importance = 3; // default importance
        return task;
    }
}