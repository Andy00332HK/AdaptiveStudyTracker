package com.example.adaptivestudytracker;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class TaskStorage {
    private static final String PREFS_NAME = "adaptive_study_tracker";
    private static final String KEY_TASKS = "tasks_json";
    private static final String KEY_COMPLETED_TASKS = "completed_tasks_json";

    private final SharedPreferences sharedPreferences;

    public TaskStorage(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<Task> loadTasks() {
        return loadTaskList(KEY_TASKS);
    }

    public List<Task> loadCompletedTasks() {
        return loadTaskList(KEY_COMPLETED_TASKS);
    }

    public void saveTasks(List<Task> tasks) {
        saveTaskList(KEY_TASKS, tasks);
    }

    public void saveCompletedTasks(List<Task> tasks) {
        saveTaskList(KEY_COMPLETED_TASKS, tasks);
    }

    public void addCompletedTask(Task task) {
        List<Task> completed = loadCompletedTasks();
        completed.add(task);
        saveCompletedTasks(completed);
    }

    public void deleteTaskById(String taskId) {
        List<Task> tasks = loadTasks();
        tasks.removeIf(task -> task.id != null && task.id.equals(taskId));
        saveTasks(tasks);
    }

    public void deleteCompletedTaskById(String taskId) {
        List<Task> tasks = loadCompletedTasks();
        tasks.removeIf(task -> task.id != null && task.id.equals(taskId));
        saveCompletedTasks(tasks);
    }

    public void clearCompletedTasks() {
        saveCompletedTasks(new ArrayList<>());
    }

    private List<Task> loadTaskList(String key) {
        String rawJson = sharedPreferences.getString(key, "[]");
        List<Task> tasks = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(rawJson);
            for (int i = 0; i < array.length(); i++) {
                tasks.add(Task.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return tasks;
    }

    private void saveTaskList(String key, List<Task> tasks) {
        JSONArray array = new JSONArray();
        for (Task task : tasks) {
            try {
                array.put(task.toJson());
            } catch (JSONException ignored) {
                // Skip malformed entry to avoid failing full persistence write.
            }
        }
        sharedPreferences.edit().putString(key, array.toString()).apply();
    }
}

