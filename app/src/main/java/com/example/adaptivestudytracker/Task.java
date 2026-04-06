package com.example.adaptivestudytracker;

import org.json.JSONException;
import org.json.JSONObject;

public class Task {
    public static final String CATEGORY_STUDY = "study";
    public static final String CATEGORY_LIFE = "life";
    public static final String CATEGORY_SLEEP = "sleep";
    public static final String CATEGORY_EXERCISE = "exercise";

    public static final String REMINDER_NONE = "none";
    public static final String REMINDER_DAILY = "daily";
    public static final String REMINDER_WEEKLY = "weekly";
    public static final String REMINDER_CUSTOM = "custom";

    public String id;
    public String title;
    public String category;
    public long dueTimeMillis;
    public String reminderFrequency;
    public int importance;

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("title", title);
        jsonObject.put("category", category);
        jsonObject.put("dueTimeMillis", dueTimeMillis);
        jsonObject.put("reminderFrequency", reminderFrequency);
        jsonObject.put("importance", importance);
        return jsonObject;
    }

    public static Task fromJson(JSONObject jsonObject) {
        Task task = new Task();
        task.id = jsonObject.optString("id", "");
        task.title = jsonObject.optString("title", "");
        task.category = jsonObject.optString("category", CATEGORY_STUDY);
        task.dueTimeMillis = jsonObject.optLong("dueTimeMillis", System.currentTimeMillis());
        task.reminderFrequency = jsonObject.optString("reminderFrequency", REMINDER_NONE);
        task.importance = jsonObject.optInt("importance", 3);
        return task;
    }
}

