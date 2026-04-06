package com.example.adaptivestudytracker;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CalendarImportActivity extends AppCompatActivity {

    private CalendarEventAdapter adapter;
    private TaskStorage taskStorage;
    private TextView emptyView;
    private View bottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_import);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.import_top_app_bar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        taskStorage = new TaskStorage(this);

        emptyView   = findViewById(R.id.text_empty_calendar);
        bottomBar   = findViewById(R.id.layout_bottom_bar);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_events);
        CheckBox selectAll = findViewById(R.id.checkbox_select_all);
        MaterialButton buttonImport = findViewById(R.id.button_import_selected);

        adapter = new CalendarEventAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 加载未来7天的日历事件
        loadEvents();

        // 全选
        selectAll.setOnCheckedChangeListener((btn, checked) ->
                adapter.selectAll(checked));

        // 导入选中的事件
        buttonImport.setOnClickListener(v -> importSelectedEvents());
    }

    private void loadEvents() {
        List<CalendarImportHelper.CalendarEvent> events =
                CalendarImportHelper.getUpcomingEvents(this, 7);

        if (events.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            bottomBar.setVisibility(View.VISIBLE);
            adapter.submitList(events);
        }
    }

    private void importSelectedEvents() {
        List<CalendarImportHelper.CalendarEvent> selected =
                adapter.getSelectedEvents();

        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.no_events_selected,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 读取现有任务，检查重复
        List<Task> existingTasks = taskStorage.loadTasks();
        int importedCount = 0;
        int skippedCount  = 0;

        for (CalendarImportHelper.CalendarEvent event : selected) {
            // 简单去重：标题和截止时间都相同则跳过
            boolean duplicate = false;
            for (Task existing : existingTasks) {
                if (existing.title.equals(event.title)
                        && Math.abs(existing.dueTimeMillis - event.dtEnd) < 60_000) {
                    duplicate = true;
                    break;
                }
            }

            if (duplicate) {
                skippedCount++;
                continue;
            }

            Task newTask = CalendarImportHelper.eventToTask(event);
            existingTasks.add(newTask);

            // 为导入的任务调度提醒
            TaskReminderManager.scheduleReminder(this, newTask);
            importedCount++;
        }

        // 保存
        taskStorage.saveTasks(existingTasks);

        // 提示结果
        String message;
        if (skippedCount > 0) {
            message = getString(R.string.import_result_with_skip,
                    importedCount, skippedCount);
        } else {
            message = getString(R.string.import_result, importedCount);
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        setResult(RESULT_OK);
        finish();
    }
}