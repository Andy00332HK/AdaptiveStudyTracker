package com.example.adaptivestudytracker;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private static final int DASHBOARD_REFRESH_MS = 1000;
    private static final long USAGE_RESYNC_INTERVAL_MS = 15000L;
    private static final int MAX_RECENT_TASKS = 5;

    private TextView totalScreenTimeView;
    private TextView appScreenTimeView;
    private TextView totalFocusTimeView;
    private TextView recentEmptyView;
    private View usageAccessBanner;
    private ScreenTimeTracker screenTimeTracker;
    private TaskStorage taskStorage;
    private RecentCompletedTaskAdapter recentCompletedTaskAdapter;

    private long displayedTotalScreenMs;
    private long displayedAppScreenMs;
    private long lastResyncElapsedMs;
    private long lastTickElapsedMs;
    private long lastSyncedDayStartMs = -1L;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) {
                return;
            }
            refreshMetrics();
            refreshHandler.postDelayed(this, DASHBOARD_REFRESH_MS);
        }
    };

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
        recentEmptyView = view.findViewById(R.id.text_recent_completed_empty);
        usageAccessBanner = view.findViewById(R.id.layout_usage_access_banner);
        Button grantButton = view.findViewById(R.id.button_grant_usage_access);
        RecyclerView recentRecycler = view.findViewById(R.id.recycler_recent_completed);

        screenTimeTracker = new ScreenTimeTracker(requireContext());
        taskStorage = new TaskStorage(requireContext());

        recentCompletedTaskAdapter = new RecentCompletedTaskAdapter();
        recentRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentRecycler.setAdapter(recentCompletedTaskAdapter);

        grantButton.setOnClickListener(v ->
                startActivity(UsageStatsHelper.getUsageAccessSettingsIntent()));

        refreshMetrics();
    }

    @Override
    public void onResume() {
        super.onResume();
        lastTickElapsedMs = SystemClock.elapsedRealtime();
        lastResyncElapsedMs = 0L;
        refreshRecentCompletedTasks();
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void refreshMetrics() {
        boolean hasAccess = UsageStatsHelper.hasUsageAccess(requireContext());
        usageAccessBanner.setVisibility(hasAccess ? View.GONE : View.VISIBLE);

        if (hasAccess) {
            long nowElapsedMs = SystemClock.elapsedRealtime();
            if (lastTickElapsedMs == 0L) {
                lastTickElapsedMs = nowElapsedMs;
            }
            long deltaMs = Math.max(0L, nowElapsedMs - lastTickElapsedMs);
            lastTickElapsedMs = nowElapsedMs;

            if (lastResyncElapsedMs == 0L || (nowElapsedMs - lastResyncElapsedMs) >= USAGE_RESYNC_INTERVAL_MS) {
                long todayStartMs = getStartOfTodayMs();
                long usageTotalMs = UsageStatsHelper.getTodayTotalScreenTimeMs(requireContext());
                long usageAppMs = UsageStatsHelper.getTodayAppScreenTimeMs(requireContext());

                if (lastSyncedDayStartMs != todayStartMs) {
                    // Day rollover: accept system values directly.
                    displayedTotalScreenMs = usageTotalMs;
                    displayedAppScreenMs = usageAppMs;
                    lastSyncedDayStartMs = todayStartMs;
                } else {
                    // Same day: never regress because UsageStatsManager can lag temporarily.
                    displayedTotalScreenMs = Math.max(displayedTotalScreenMs, usageTotalMs);
                    displayedAppScreenMs = Math.max(displayedAppScreenMs, usageAppMs);
                }
                lastResyncElapsedMs = nowElapsedMs;
            } else {
                // While dashboard is visible, this app is foreground; increment for smoother real-time UI.
                displayedTotalScreenMs += deltaMs;
                displayedAppScreenMs += deltaMs;
            }

            totalScreenTimeView.setText(getString(R.string.total_screen_time_format, formatDuration(displayedTotalScreenMs)));
            appScreenTimeView.setText(getString(R.string.app_screen_time_format, formatDuration(displayedAppScreenMs)));
        } else {
            totalScreenTimeView.setText(getString(R.string.total_screen_time_format, getString(R.string.usage_access_required)));
            appScreenTimeView.setText(getString(R.string.app_screen_time_format, getString(R.string.usage_access_required)));
        }

        long totalFocusSeconds = screenTimeTracker.getTotalFocusSeconds();
        totalFocusTimeView.setText(getString(R.string.total_focus_time_format, formatFocusDuration(totalFocusSeconds)));
    }

    private void refreshRecentCompletedTasks() {
        List<Task> completed = new ArrayList<>(taskStorage.loadCompletedTasks());
        Collections.reverse(completed);
        if (completed.size() > MAX_RECENT_TASKS) {
            completed = completed.subList(0, MAX_RECENT_TASKS);
        }
        recentCompletedTaskAdapter.submitList(completed);
        recentEmptyView.setVisibility(completed.isEmpty() ? View.VISIBLE : View.GONE);
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

    private long getStartOfTodayMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
