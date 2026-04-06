package com.example.adaptivestudytracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CalendarEventAdapter
        extends RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder> {

    private final List<CalendarImportHelper.CalendarEvent> events = new ArrayList<>();
    private final DateFormat dateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    public void submitList(List<CalendarImportHelper.CalendarEvent> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    /** 返回所有被用户勾选的事件 */
    public List<CalendarImportHelper.CalendarEvent> getSelectedEvents() {
        List<CalendarImportHelper.CalendarEvent> selected = new ArrayList<>();
        for (CalendarImportHelper.CalendarEvent e : events) {
            if (e.selected) selected.add(e);
        }
        return selected;
    }

    /** 全选 / 取消全选 */
    public void selectAll(boolean select) {
        for (CalendarImportHelper.CalendarEvent e : events) {
            e.selected = select;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        CalendarImportHelper.CalendarEvent event = events.get(position);

        holder.checkBox.setChecked(event.selected);
        holder.title.setText(event.title);

        String timeText = dateFormat.format(new Date(event.dtStart));
        if (event.dtEnd > event.dtStart) {
            timeText += " — " + dateFormat.format(new Date(event.dtEnd));
        }
        holder.time.setText(timeText);

        String calInfo = event.calendarName != null ? event.calendarName : "";
        if (event.description != null && !event.description.isEmpty()) {
            calInfo += (calInfo.isEmpty() ? "" : " · ") + event.description;
            // 截断描述
            if (calInfo.length() > 80) calInfo = calInfo.substring(0, 80) + "…";
        }
        holder.calendarName.setText(calInfo);

        // 点击整行也能切换复选框
        holder.itemView.setOnClickListener(v -> {
            event.selected = !event.selected;
            holder.checkBox.setChecked(event.selected);
        });

        holder.checkBox.setOnCheckedChangeListener((btn, checked) ->
                event.selected = checked);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final TextView title;
        final TextView time;
        final TextView calendarName;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox     = itemView.findViewById(R.id.checkbox_event);
            title        = itemView.findViewById(R.id.text_event_title);
            time         = itemView.findViewById(R.id.text_event_time);
            calendarName = itemView.findViewById(R.id.text_event_calendar);
        }
    }
}