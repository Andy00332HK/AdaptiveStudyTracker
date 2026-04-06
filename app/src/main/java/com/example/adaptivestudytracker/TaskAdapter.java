package com.example.adaptivestudytracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface TaskActionListener {
        void onTaskEdit(Task task);
        void onTaskDone(Task task);
        void onTaskDelete(Task task);
    }

    private final List<Task> tasks = new ArrayList<>();
    private final TaskActionListener taskActionListener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    public TaskAdapter(TaskActionListener taskActionListener) {
        this.taskActionListener = taskActionListener;
    }

    public void submitList(List<Task> newTasks) {
        tasks.clear();
        tasks.addAll(newTasks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        String dueDate = dateFormat.format(new Date(task.dueTimeMillis));

        holder.title.setText(task.title);
        holder.meta.setText(holder.itemView.getContext().getString(
                R.string.task_meta_format,
                task.category,
                task.importance,
                dueDate
        ));
        holder.reminder.setText(holder.itemView.getContext().getString(
                R.string.task_reminder_format,
                Task.REMINDER_DAILY.equals(task.reminderFrequency)
                        ? holder.itemView.getContext().getString(R.string.daily)
                        : holder.itemView.getContext().getString(R.string.off)
        ));
        holder.itemView.setOnClickListener(v -> taskActionListener.onTaskEdit(task));
        holder.buttonDone.setOnClickListener(v -> taskActionListener.onTaskDone(task));
        holder.buttonDelete.setOnClickListener(v -> taskActionListener.onTaskDelete(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView meta;
        final TextView reminder;
        final Button buttonDone;
        final Button buttonDelete;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_task_title);
            meta = itemView.findViewById(R.id.text_task_meta);
            reminder = itemView.findViewById(R.id.text_task_reminder);
            buttonDone = itemView.findViewById(R.id.button_task_done);
            buttonDelete = itemView.findViewById(R.id.button_task_delete);
        }
    }
}
