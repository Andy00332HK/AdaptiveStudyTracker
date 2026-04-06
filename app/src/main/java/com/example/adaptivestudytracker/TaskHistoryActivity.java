package com.example.adaptivestudytracker;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TaskHistoryActivity extends AppCompatActivity {

    private TaskStorage taskStorage;
    private CompletedTaskAdapter adapter;
    private final List<Task> completedTasks = new ArrayList<>();
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_history);

        MaterialToolbar toolbar = findViewById(R.id.history_top_app_bar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setTitle(R.string.task_history);
        }

        taskStorage = new TaskStorage(this);
        emptyView = findViewById(R.id.text_empty_history);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_completed_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CompletedTaskAdapter(this::deleteCompletedTask);
        recyclerView.setAdapter(adapter);

        reloadCompletedTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadCompletedTasks();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.action_delete_all_completed) {
            taskStorage.clearCompletedTasks();
            reloadCompletedTasks();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCompletedTask(Task task) {
        taskStorage.deleteCompletedTaskById(task.id);
        reloadCompletedTasks();
    }

    private void reloadCompletedTasks() {
        completedTasks.clear();
        completedTasks.addAll(taskStorage.loadCompletedTasks());
        completedTasks.sort(Comparator.comparingLong(task -> task.dueTimeMillis));
        adapter.submitList(completedTasks);
        emptyView.setVisibility(completedTasks.isEmpty() ? View.VISIBLE : View.GONE);
    }
}

