package com.example.adaptivestudytracker;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private SettingsManager settingsManager;
    private TextView usageLimitLabel;
    private SeekBar seekBarUsageLimit;
    private Button buttonSleepStart;
    private Button buttonSleepEnd;
    private Button buttonShareSummary;
    private SwitchMaterial switchReminders;
    private SwitchMaterial switchUsageWarnings;
    private SwitchMaterial switchUsageTracking;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settingsManager = new SettingsManager(requireContext());

        usageLimitLabel = view.findViewById(R.id.text_usage_limit_label);
        seekBarUsageLimit = view.findViewById(R.id.seekbar_usage_limit);
        buttonSleepStart = view.findViewById(R.id.button_sleep_start);
        buttonSleepEnd = view.findViewById(R.id.button_sleep_end);
        switchReminders = view.findViewById(R.id.switch_task_reminders);
        switchUsageWarnings = view.findViewById(R.id.switch_usage_warnings);
        switchUsageTracking = view.findViewById(R.id.switch_usage_tracking);
        buttonShareSummary = view.findViewById(R.id.button_share_summary);

        loadCurrentSettings();
        setupListeners();
    }

    /* ----- Load settings and update UI ----- */
    private void loadCurrentSettings() {
        int limitMin = settingsManager.getUsageLimitMinutes();
        seekBarUsageLimit.setProgress(limitMin);
        updateUsageLimitLabel(limitMin);

        updateTimeButton(buttonSleepStart,
                settingsManager.getSleepStartHour(),
                settingsManager.getSleepStartMinute());
        updateTimeButton(buttonSleepEnd,
                settingsManager.getSleepEndHour(),
                settingsManager.getSleepEndMinute());

        switchReminders.setChecked(settingsManager.isRemindersEnabled());
        switchUsageWarnings.setChecked(settingsManager.isUsageWarningsEnabled());
        switchUsageTracking.setChecked(settingsManager.isUsageTrackingEnabled());
    }

    /* ----- Listeners ----- */
    private void setupListeners() {

        // SeekBar: daily usage limit
        seekBarUsageLimit.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                updateUsageLimitLabel(Math.max(10, progress));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                int value = Math.max(10, sb.getProgress());
                settingsManager.setUsageLimitMinutes(value);
            }
        });

        // Sleep start time
        buttonSleepStart.setOnClickListener(v ->
                new TimePickerDialog(requireContext(), (view, h, m) -> {
                    settingsManager.setSleepStart(h, m);
                    updateTimeButton(buttonSleepStart, h, m);
                }, settingsManager.getSleepStartHour(),
                        settingsManager.getSleepStartMinute(), true).show()
        );

        // Wakeup time
        buttonSleepEnd.setOnClickListener(v ->
                new TimePickerDialog(requireContext(), (view, h, m) -> {
                    settingsManager.setSleepEnd(h, m);
                    updateTimeButton(buttonSleepEnd, h, m);
                }, settingsManager.getSleepEndHour(),
                        settingsManager.getSleepEndMinute(), true).show()
        );

        // Task reminders switch
        switchReminders.setOnCheckedChangeListener((btn, checked) -> {
            settingsManager.setRemindersEnabled(checked);
            if (checked) {
                TaskReminderManager.rescheduleAll(requireContext());
            } else {
                TaskReminderManager.cancelAll(requireContext());
            }
        });

        // Usage warnings switch
        switchUsageWarnings.setOnCheckedChangeListener((btn, checked) ->
                settingsManager.setUsageWarningsEnabled(checked));

        // Background tracking switch
        switchUsageTracking.setOnCheckedChangeListener((btn, checked) -> {
            settingsManager.setUsageTrackingEnabled(checked);
            if (checked) {
                if (!UsageStatsHelper.hasUsageAccess(requireContext())) {
                    Toast.makeText(requireContext(),
                            "Please grant Usage Access first",
                            Toast.LENGTH_LONG).show();
                    startActivity(UsageStatsHelper.getUsageAccessSettingsIntent());
                    switchUsageTracking.setChecked(false);
                    return;
                }
                PhoneUsageTrackingService.start(requireContext());
            } else {
                PhoneUsageTrackingService.stop(requireContext());
            }
        });

        // Share button (implicit intent)
        buttonShareSummary.setOnClickListener(v -> shareDailySummary());
    }

    /* ----- Helper methods ----- */
    private void updateUsageLimitLabel(int minutes) {
        usageLimitLabel.setText(String.format(Locale.getDefault(),
                "Daily screen time limit: %dh %dm", minutes / 60, minutes % 60));
    }

    private void updateTimeButton(Button button, int hour, int minute) {
        button.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }

    /**
     * Share a brief summary of today's focus and usage via an implicit intent.
     */
    private void shareDailySummary() {
        try {
            ScreenTimeTracker tracker = new ScreenTimeTracker(requireContext());
            long focusMin = tracker.getTotalFocusSeconds() / 60;

            // screen usage time
            boolean hasAccess = UsageStatsHelper.hasUsageAccess(requireContext());
            String usageTimeFormatted = "N/A";
            if (hasAccess) {
                long totalScreenMs = UsageStatsHelper.getTodayTotalScreenTimeMs(requireContext());
                long totalScreenMinutes = totalScreenMs / (1000 * 60);
                long hours = totalScreenMinutes / 60;
                long minutes = totalScreenMinutes % 60;
                usageTimeFormatted = String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
            }

            // usage limit
            int limitMinutes = settingsManager.getUsageLimitMinutes();
            long limitHours = limitMinutes / 60;
            long limitMins = limitMinutes % 60;

            // sleep time
            int sleepStartHour = settingsManager.getSleepStartHour();
            int sleepStartMinute = settingsManager.getSleepStartMinute();
            int sleepEndHour = settingsManager.getSleepEndHour();
            int sleepEndMinute = settingsManager.getSleepEndMinute();

            String message = String.format(Locale.getDefault(),
                    "📊 My Adaptive Study Tracker Summary:\n" +
                            "🎯 Today's focus time: %d minutes\n" +
                            "📱 Usage time: %s\n" +
                            "📱 Usage limit: %dh %dm\n" +
                            "😴 Sleep schedule: %02d:%02d – %02d:%02d\n" +
                            "Keep up the great work! 💪",
                    focusMin,              // %d
                    usageTimeFormatted,    // %s
                    limitHours,
                    limitMins,
                    sleepStartHour,
                    sleepStartMinute,
                    sleepEndHour,
                    sleepEndMinute);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(share, "Share via"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(),
                    "Error sharing summary: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}