package com.example.adaptivestudytracker;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class EditTaskActivity extends AppCompatActivity {
    public static final String EXTRA_TASK_ID = "extra_task_id";

    private EditText editTitle;
    private Spinner spinnerCategory;
    private Spinner spinnerImportance;
    private SwitchMaterial switchDailyReminder;
    private TextView textDueDate;

    private long selectedDueTimeMillis = -1L;
    private TaskStorage taskStorage;
    private String editingTaskId;

    private final List<String> categories = Arrays.asList(
            Task.CATEGORY_STUDY,
            Task.CATEGORY_LIFE,
            Task.CATEGORY_SLEEP,
            Task.CATEGORY_EXERCISE
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        MaterialToolbar toolbar = findViewById(R.id.edit_top_app_bar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        taskStorage = new TaskStorage(this);

        editTitle = findViewById(R.id.edit_task_title);
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerImportance = findViewById(R.id.spinner_importance);
        switchDailyReminder = findViewById(R.id.switch_daily_reminder);
        textDueDate = findViewById(R.id.text_due_date);
        Button buttonPickDateTime = findViewById(R.id.button_pick_due);
        Button buttonSave = findViewById(R.id.button_save_task);
        Button buttonCancel = findViewById(R.id.button_cancel_task);

        setupSpinners();

        editingTaskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (editingTaskId != null && !editingTaskId.isEmpty()) {
            loadTaskForEdit(editingTaskId);
            setTitle(R.string.edit_task_title);
        } else {
            setTitle(R.string.create_task_title);
        }

        buttonPickDateTime.setOnClickListener(v -> openDateTimePicker());
        buttonSave.setOnClickListener(v -> saveTask());
        buttonCancel.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_task, menu);
        MenuItem deleteItem = menu.findItem(R.id.action_delete_current_task);
        if (deleteItem != null) {
            deleteItem.setVisible(editingTaskId != null && !editingTaskId.isEmpty());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.action_task_history) {
            startActivity(new Intent(this, TaskHistoryActivity.class));
            return true;
        }
        if (id == R.id.action_delete_current_task) {
            deleteCurrentTask();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSpinners() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> importanceAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                Arrays.asList(
                        getString(R.string.importance_1),
                        getString(R.string.importance_2),
                        getString(R.string.importance_3),
                        getString(R.string.importance_4),
                        getString(R.string.importance_5)
                )
        );
        importanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImportance.setAdapter(importanceAdapter);
        spinnerImportance.setSelection(2);
    }

    private void loadTaskForEdit(String taskId) {
        for (Task task : taskStorage.loadTasks()) {
            if (taskId.equals(task.id)) {
                editTitle.setText(task.title);
                spinnerCategory.setSelection(categories.indexOf(task.category));
                spinnerImportance.setSelection(Math.max(0, task.importance - 1));
                // Reminder switch on if task has a reminder set
                switchDailyReminder.setChecked(!Task.REMINDER_NONE.equals(task.reminderFrequency));
                selectedDueTimeMillis = task.dueTimeMillis;
                updateDueTimeLabel();
                break;
            }
        }
    }

    private void openDateTimePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            this,
                            (timeView, hourOfDay, minute) -> {
                                Calendar selected = Calendar.getInstance();
                                selected.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                                selected.set(Calendar.MILLISECOND, 0);
                                selectedDueTimeMillis = selected.getTimeInMillis();
                                updateDueTimeLabel();
                            },
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDueTimeLabel() {
        if (selectedDueTimeMillis <= 0L) {
            textDueDate.setText(R.string.no_due_time_selected);
            return;
        }
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        textDueDate.setText(dateFormat.format(selectedDueTimeMillis));
    }


    private void saveTask() {
        String title = editTitle.getText().toString();
        String category = spinnerCategory.getSelectedItem().toString();
        String reminder = switchDailyReminder.isChecked() ? Task.REMINDER_DAILY : Task.REMINDER_NONE;
        int importance = spinnerImportance.getSelectedItemPosition() + 1;

        TaskFormValidator.ValidationResult validationResult = TaskFormValidator.validate(
                title,
                category,
                selectedDueTimeMillis,
                reminder,
                importance
        );

        if (!validationResult.isValid) {
            Toast.makeText(this, validationResult.errorMessage, Toast.LENGTH_SHORT).show();
            return;
        }

        List<Task> tasks = taskStorage.loadTasks();
        Task target = null;

        if (editingTaskId != null) {
            for (Task task : tasks) {
                if (editingTaskId.equals(task.id)) {
                    target = task;
                    break;
                }
            }
        }

        if (target == null) {
            target = new Task();
            target.id = UUID.randomUUID().toString();
            tasks.add(target);
        }

        target.title = title.trim();
        target.category = category;
        target.dueTimeMillis = selectedDueTimeMillis;
        target.reminderFrequency = reminder;
        target.importance = importance;

        taskStorage.saveTasks(tasks);

        // Schedule reminder for the task (if enabled in settings)
        TaskReminderManager.scheduleReminder(this, target);


        setResult(RESULT_OK);
        finish();
    }

    private void deleteCurrentTask() {
        if (editingTaskId == null || editingTaskId.isEmpty()) {
            return;
        }

        // Cancel any scheduled reminder for this task before deleting
        for (Task task : taskStorage.loadTasks()) {
            if (editingTaskId.equals(task.id)) {
                TaskReminderManager.cancelReminder(this, task);
                break;
            }
        }

        taskStorage.deleteTaskById(editingTaskId);
        setResult(RESULT_OK);
        finish();
    }
}

