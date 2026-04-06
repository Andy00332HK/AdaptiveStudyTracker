package com.example.adaptivestudytracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView totalScreenTimeView;
    private TextView appScreenTimeView;
    private TextView totalFocusTimeView;
    private View usageAccessBanner;
    private ScreenTimeTracker screenTimeTracker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        totalScreenTimeView = view.findViewById(R.id.text_total_screen_time);
        appScreenTimeView = view.findViewById(R.id.text_app_screen_time);
        totalFocusTimeView = view.findViewById(R.id.text_total_focus_time);
        usageAccessBanner = view.findViewById(R.id.layout_usage_access_banner);
        Button grantButton = view.findViewById(R.id.button_grant_usage_access);
        screenTimeTracker = new ScreenTimeTracker(requireContext());

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

        if (hasAccess) {
            long totalScreenMs = UsageStatsHelper.getTodayTotalScreenTimeMs(requireContext());
            long appScreenMs = UsageStatsHelper.getTodayAppScreenTimeMs(requireContext());
            totalScreenTimeView.setText(getString(R.string.total_screen_time_format, formatDuration(totalScreenMs)));
            appScreenTimeView.setText(getString(R.string.app_screen_time_format, formatDuration(appScreenMs)));
        } else {
            totalScreenTimeView.setText(getString(R.string.total_screen_time_format, getString(R.string.usage_access_required)));
            appScreenTimeView.setText(getString(R.string.app_screen_time_format, getString(R.string.usage_access_required)));
        }

        long totalFocusSeconds = screenTimeTracker.getTotalFocusSeconds();
        totalFocusTimeView.setText(getString(R.string.total_focus_time_format, formatFocusDuration(totalFocusSeconds)));
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatFocusDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }
}

