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
     * 表示一个日历事件（尚未导入为 Task）
     */
    public static class CalendarEvent {
        public long   eventId;
        public String title;
        public String description;
        public long   dtStart;   // 开始时间（毫秒）
        public long   dtEnd;     // 结束时间（毫秒）
        public String calendarName;
        public boolean selected = false; // 用户是否勾选

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
     * 读取未来 7 天的日历事件。
     * 使用系统 Content Provider: CalendarContract.Events [1]
     *
     * @param context 需要有 READ_CALENDAR 权限
     * @return 事件列表，如果没有权限或没有事件则返回空列表
     */
    public static List<CalendarEvent> getUpcomingEvents(Context context, int days) {
        List<CalendarEvent> events = new ArrayList<>();

        // 查询时间范围：现在 → 未来 days 天
        long now = System.currentTimeMillis();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.DAY_OF_YEAR, days);
        long endTime = end.getTimeInMillis();

        // 要查询的列
        String[] projection = {
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.CALENDAR_DISPLAY_NAME
        };

        // 查询条件：开始时间在 now 到 endTime 之间
        String selection = CalendarContract.Events.DTSTART + " >= ? AND "
                + CalendarContract.Events.DTSTART + " <= ?";
        String[] selectionArgs = {
                String.valueOf(now),
                String.valueOf(endTime)
        };

        // 按开始时间排序
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
     * 将日历事件转换为 Task 对象
     */
    public static Task eventToTask(CalendarEvent event) {
        Task task = new Task();
        task.id = "cal_" + event.eventId + "_" + System.currentTimeMillis();
        task.title = event.title;
        task.category = Task.CATEGORY_STUDY; // 默认分类，用户可稍后编辑
        task.dueTimeMillis = event.dtEnd > 0 ? event.dtEnd : event.dtStart;
        task.reminderFrequency = Task.REMINDER_DAILY;
        task.importance = 3; // 默认重要性
        return task;
    }
}