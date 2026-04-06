package com.example.adaptivestudytracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private final ActivityResultLauncher<Intent> editTaskLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    reloadTasks();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskStorage = new TaskStorage(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_tasks);
        FloatingActionButton fabAddTask = view.findViewById(R.id.fab_add_task);

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
        taskStorage.addCompletedTask(task);
        taskStorage.deleteTaskById(task.id);
        reloadTasks();
    }

    private void deleteTask(@NonNull Task task) {
        taskStorage.deleteTaskById(task.id);
        reloadTasks();
    }
}