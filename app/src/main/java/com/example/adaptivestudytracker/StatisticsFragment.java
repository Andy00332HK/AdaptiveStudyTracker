package com.example.adaptivestudytracker;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class StatisticsFragment extends Fragment {

    private static final int INSIGHTS_REFRESH_MS = 1000;

    private WeeklyUsageChartView weeklyChartView;
    private TextView weekRangeView;
    private TextView productiveDayView;
    private TextView totalFocusWeekView;
    private TextView totalScreenWeekView;
    private TextView averageFocusView;
    private TextView usageAccessHint;

    private ScreenTimeTracker screenTimeTracker;
    private SharedScreenTimeState sharedScreenTimeState;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) {
                return;
            }
            refreshInsights();
            refreshHandler.postDelayed(this, INSIGHTS_REFRESH_MS);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        weeklyChartView = view.findViewById(R.id.view_weekly_chart);
        weekRangeView = view.findViewById(R.id.text_week_range);
        productiveDayView = view.findViewById(R.id.text_most_productive_day);
        totalFocusWeekView = view.findViewById(R.id.text_week_total_focus);
        totalScreenWeekView = view.findViewById(R.id.text_week_total_screen);
        averageFocusView = view.findViewById(R.id.text_week_average_focus);
        usageAccessHint = view.findViewById(R.id.text_insights_usage_access_hint);

        screenTimeTracker = new ScreenTimeTracker(requireContext());
        sharedScreenTimeState = SharedScreenTimeState.getInstance();
        refreshInsights();
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedScreenTimeState.beginVisibleSession();
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        refreshHandler.removeCallbacks(refreshRunnable);
        if (!hidden && isAdded()) {
            sharedScreenTimeState.beginVisibleSession();
            refreshHandler.post(refreshRunnable);
        }
    }

    private void refreshInsights() {
        SharedScreenTimeState.Snapshot snapshot = sharedScreenTimeState.update(requireContext());
        LocalDate weekStart = snapshot.weekStart;
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Long> focusDaily = screenTimeTracker.getCurrentWeekFocusSecondsMondayFirst();
        List<Long> screenDaily = snapshot.hasUsageAccess ? snapshot.weekScreenDailyMs : createZeroList(7);

        usageAccessHint.setVisibility(snapshot.hasUsageAccess ? View.GONE : View.VISIBLE);
        weeklyChartView.setData(weekStart, focusDaily, screenDaily);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault());
        weekRangeView.setText(getString(R.string.insights_week_range_format,
                weekStart.format(formatter), weekEnd.format(formatter)));

        long totalFocusSeconds = 0L;
        long totalScreenMs = 0L;
        long bestFocus = -1L;
        int bestDayIndex = 0;

        for (int i = 0; i < focusDaily.size(); i++) {
            long focusValue = focusDaily.get(i);
            totalFocusSeconds += focusValue;
            if (focusValue > bestFocus) {
                bestFocus = focusValue;
                bestDayIndex = i;
            }
        }

        for (long screenValue : screenDaily) {
            totalScreenMs += screenValue;
        }

        LocalDate bestDate = weekStart.plusDays(bestDayIndex);
        String productiveDay = bestDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());

        productiveDayView.setText(getString(R.string.insights_most_productive_day_format, productiveDay, formatDuration(bestFocus)));
        totalFocusWeekView.setText(getString(R.string.insights_total_focus_week_format, formatDuration(totalFocusSeconds)));
        totalScreenWeekView.setText(getString(R.string.insights_total_screen_week_format, formatDuration(totalScreenMs / 1000)));
        long averageFocusSeconds = focusDaily.isEmpty() ? 0 : totalFocusSeconds / focusDaily.size();
        averageFocusView.setText(getString(R.string.insights_average_focus_day_format, formatDuration(averageFocusSeconds)));
    }

    private List<Long> createZeroList(int count) {
        List<Long> values = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(0L);
        }
        return values;
    }


    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }
}