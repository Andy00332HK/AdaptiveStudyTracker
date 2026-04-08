package com.example.adaptivestudytracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView totalScreenTimeView;
    private TextView appScreenTimeView;
    private TextView totalFocusTimeView;
    private TextView taskSummaryView;
    private TextView completedSummaryView;
    private TextView productivityScoreView;

    private View usageAccessBanner;

    private ScreenTimeTracker screenTimeTracker;
    private TaskStorage taskStorage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        totalScreenTimeView = view.findViewById(R.id.text_total_screen_time);
        appScreenTimeView = view.findViewById(R.id.text_app_screen_time);
        totalFocusTimeView = view.findViewById(R.id.text_total_focus_time);
        taskSummaryView = view.findViewById(R.id.text_task_summary);
        completedSummaryView = view.findViewById(R.id.text_completed_summary);
        productivityScoreView = view.findViewById(R.id.text_productivity_score);

        usageAccessBanner = view.findViewById(R.id.layout_usage_access_banner);

        Button grantButton = view.findViewById(R.id.button_grant_usage_access);

        screenTimeTracker = new ScreenTimeTracker(requireContext());
        taskStorage = new TaskStorage(requireContext());

        grantButton.setOnClickListener(v ->
                startActivity(UsageStatsHelper.getUsageAccessSettingsIntent()));

        refreshMetrics();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshMetrics();
    }

    private void refreshMetrics() {
        boolean hasAccess = UsageStatsHelper.hasUsageAccess(requireContext());
        usageAccessBanner.setVisibility(hasAccess ? View.GONE : View.VISIBLE);

        long totalScreenMs = 0;
        long appScreenMs = 0;

        if (hasAccess) {
            totalScreenMs = UsageStatsHelper.getTodayTotalScreenTimeMs(requireContext());
            appScreenMs = UsageStatsHelper.getTodayAppScreenTimeMs(requireContext());

            totalScreenTimeView.setText("Total Screen Time: " + formatDuration(totalScreenMs));
            appScreenTimeView.setText("App Usage Time: " + formatDuration(appScreenMs));
        } else {
            totalScreenTimeView.setText("Total Screen Time: permission required");
            appScreenTimeView.setText("App Usage Time: permission required");
        }

        long focusSeconds = screenTimeTracker.getTotalFocusSeconds();
        totalFocusTimeView.setText("Focus Time: " + formatFocusDuration(focusSeconds));

        List<Task> activeTasks = taskStorage.loadTasks();
        List<Task> completedTasks = taskStorage.loadCompletedTasks();

        taskSummaryView.setText("Pending Tasks: " + activeTasks.size());
        completedSummaryView.setText("Completed Tasks: " + completedTasks.size());

        int score = calculateProductivityScore(
                focusSeconds,
                totalScreenMs,
                completedTasks.size()
        );

        productivityScoreView.setText("Productivity Score: " + score + "%");
    }

    private int calculateProductivityScore(long focusSec,
                                           long screenMs,
                                           int completedTasks) {

        int score = 50;

        score += Math.min((int)(focusSec / 60), 20);
        score += completedTasks * 5;
        score -= Math.min((int)(screenMs / 3600000), 20);

        return Math.max(0, Math.min(score, 100));
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        return String.format(Locale.getDefault(),
                "%02d hr %02d min", hours, minutes);
    }

    private String formatFocusDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        return String.format(Locale.getDefault(),
                "%02d hr %02d min", hours, minutes);
    }
}