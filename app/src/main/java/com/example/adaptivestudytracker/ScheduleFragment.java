package com.example.adaptivestudytracker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScheduleFragment extends Fragment {

    private TaskStorage taskStorage;
    private TaskAdapter taskAdapter;
    private final List<Task> tasks = new ArrayList<>();

    //  Activity Result Launchers

    private final ActivityResultLauncher<Intent> editTaskLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            reloadTasks();
                        }
                    });

    // Calendar import result
    private final ActivityResultLauncher<Intent> importCalendarLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            reloadTasks();
                            Toast.makeText(requireContext(),
                                    R.string.calendar_import_success,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    // Runtime permission request
    private final ActivityResultLauncher<String> requestCalendarPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), granted -> {
                        if (granted) {
                            openCalendarImport();
                        } else {
                            Toast.makeText(requireContext(),
                                    R.string.calendar_permission_denied,
                                    Toast.LENGTH_LONG).show();
                        }
                    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskStorage = new TaskStorage(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_tasks);
        FloatingActionButton fabAddTask = view.findViewById(R.id.fab_add_task);
        FloatingActionButton fabImportCalendar = view.findViewById(R.id.fab_import_calendar);

        taskAdapter = new TaskAdapter(new TaskAdapter.TaskActionListener() {
            @Override
            public void onTaskEdit(Task task) {
                openEditTaskScreen(task);
            }

            @Override
            public void onTaskDone(Task task) {
                markTaskDone(task);
            }

            @Override
            public void onTaskDelete(Task task) {
                deleteTask(task);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(taskAdapter);

        fabAddTask.setOnClickListener(v -> openEditTaskScreen(null));

        // Calendar import button
        fabImportCalendar.setOnClickListener(v -> checkCalendarPermissionAndImport());
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadTasks();
    }

    private void reloadTasks() {
        tasks.clear();
        tasks.addAll(taskStorage.loadTasks());
        tasks.sort(Comparator.comparingLong(task -> task.dueTimeMillis));
        taskAdapter.submitList(tasks);
    }

    private void openEditTaskScreen(@Nullable Task task) {
        Intent intent = new Intent(requireContext(), EditTaskActivity.class);
        if (task != null) {
            intent.putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id);
        }
        editTaskLauncher.launch(intent);
    }

    private void markTaskDone(@NonNull Task task) {
        TaskReminderManager.cancelReminder(requireContext(), task);
        taskStorage.addCompletedTask(task);
        taskStorage.deleteTaskById(task.id);
        reloadTasks();
    }

    private void deleteTask(@NonNull Task task) {
        TaskReminderManager.cancelReminder(requireContext(), task);
        taskStorage.deleteTaskById(task.id);
        reloadTasks();
    }

    /* ========== Calendar import logic ========== */

    /**
     * Check READ_CALENDAR permission; open import screen if granted, otherwise request it.
     */
    private void checkCalendarPermissionAndImport() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED) {
            openCalendarImport();
        } else {
            requestCalendarPermission.launch(Manifest.permission.READ_CALENDAR);
        }
    }

    /**
     * Open the calendar import screen using an explicit Intent.
     */
    private void openCalendarImport() {
        Intent intent = new Intent(requireContext(), CalendarImportActivity.class);
        importCalendarLauncher.launch(intent);
    }
}