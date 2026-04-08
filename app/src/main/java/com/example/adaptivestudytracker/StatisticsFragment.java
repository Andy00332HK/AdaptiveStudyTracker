package com.example.adaptivestudytracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import java.util.List;

public class StatisticsFragment extends Fragment {

    private TaskStorage taskStorage;
    private ScreenTimeTracker tracker;

    private TextView insightsView;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        insightsView = view.findViewById(R.id.text_insights);

        taskStorage = new TaskStorage(requireContext());
        tracker = new ScreenTimeTracker(requireContext());

        loadInsights();

        return view;
    }

    private void loadInsights() {
        List<Task> active = taskStorage.loadTasks();
        List<Task> completed = taskStorage.loadCompletedTasks();

        long focusSec = tracker.getTotalFocusSeconds();

        String suggestion;

        if (focusSec < 1800) {
            suggestion = "Low focus time today. Consider a Pomodoro session.";
        } else if (completed.size() >= 3) {
            suggestion = "Excellent productivity today.";
        } else {
            suggestion = "Maintain balance between tasks and rest.";
        }

        String text =
                "Pending Tasks: " + active.size() + "\n\n" +
                        "Completed Tasks: " + completed.size() + "\n\n" +
                        "Focus Time: " + (focusSec / 60) + " minutes\n\n" +
                        "Suggestion:\n" + suggestion;

        insightsView.setText(text);
    }
}