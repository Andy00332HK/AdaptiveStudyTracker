package com.example.adaptivestudytracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecentCompletedTaskAdapter extends RecyclerView.Adapter<RecentCompletedTaskAdapter.TaskViewHolder> {

    private final List<Task> tasks = new ArrayList<>();

    public void submitList(List<Task> newTasks) {
        tasks.clear();
        tasks.addAll(newTasks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_completed_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.title.setText(task.title == null || task.title.isEmpty()
                ? holder.itemView.getContext().getString(R.string.untitled_task)
                : task.title);
        holder.meta.setText(holder.itemView.getContext().getString(
                R.string.recent_task_meta_format,
                task.category,
                task.importance
        ));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView meta;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_recent_task_title);
            meta = itemView.findViewById(R.id.text_recent_task_meta);
        }
    }
}

