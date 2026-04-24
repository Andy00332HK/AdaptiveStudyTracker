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

public class CompletedTaskAdapter extends RecyclerView.Adapter<CompletedTaskAdapter.CompletedTaskViewHolder> {

    public interface CompletedTaskActionListener {
        void onDeleteCompletedTask(Task task);
        void onRestoreCompletedTask(Task task);
    }

    private final List<Task> tasks = new ArrayList<>();
    private final CompletedTaskActionListener actionListener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    public CompletedTaskAdapter(CompletedTaskActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<Task> newTasks) {
        tasks.clear();
        tasks.addAll(newTasks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CompletedTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_completed_task, parent, false);
        return new CompletedTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompletedTaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.title.setText(task.title);
        holder.meta.setText(holder.itemView.getContext().getString(
                R.string.task_meta_format,
                task.category,
                task.importance,
                dateFormat.format(new Date(task.dueTimeMillis))
        ));
        holder.buttonDelete.setOnClickListener(v -> actionListener.onDeleteCompletedTask(task));
        if (holder.buttonRestore != null) {
            holder.buttonRestore.setOnClickListener(v -> actionListener.onRestoreCompletedTask(task));
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class CompletedTaskViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView meta;
        final Button buttonDelete;
        final Button buttonRestore;

        CompletedTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_completed_task_title);
            meta = itemView.findViewById(R.id.text_completed_task_meta);
            buttonDelete = itemView.findViewById(R.id.button_delete_completed_task);
            // buttonRestore may be null in older layouts, guard accordingly
            Button tmpRestore = null;
            try {
                tmpRestore = itemView.findViewById(R.id.button_restore_completed_task);
            } catch (Exception ignored) {
            }
            buttonRestore = tmpRestore;
        }
    }
}

