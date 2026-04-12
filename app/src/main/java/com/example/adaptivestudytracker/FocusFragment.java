package com.example.adaptivestudytracker;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class FocusFragment extends Fragment {

    private TextView mTextViewTimer, mTextViewTotalFocus, mTextViewSessionType, mTextViewDndStatus;
    private ImageButton mButtonStartPause, mButtonStop;
    private SwitchMaterial mSwitchSilenceNotifications;
    private CountDownTimer mCountDownTimer;
    private NotificationManager notificationManager;
    private ScreenTimeTracker screenTimeTracker;

    private boolean mTimerRunning = false;
    private boolean isBreakTime = false;

    // Timer durations (in milliseconds)
    private static final long FOCUS_TIME = 1500000; // 25 min
    private static final long BREAK_TIME = 300000;  // 5 min

    private long mTimeLeftInMillis = FOCUS_TIME;
    private long mTotalFocusSeconds = 0;
    private int previousInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL;
    private boolean dndApplied = false;

    private static final String PREFS_FOCUS = "focus_prefs";
    private static final String KEY_SUPPRESS_NOTIFICATIONS = "suppress_notifications";

    // Called by MainActivity when user leaves Focus tab.
    public boolean onTabSwitchedAway() {
        boolean wasFocusRunning = mTimerRunning && !isBreakTime;
        endSessionAndRestoreTimer();
        return wasFocusRunning;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_focus, container, false);

        mTextViewTimer = v.findViewById(R.id.text_view_timer);
        mTextViewTotalFocus = v.findViewById(R.id.text_view_total_focus);
        mTextViewSessionType = v.findViewById(R.id.text_view_session_type); // Make sure this ID exists in XML
        mTextViewDndStatus = v.findViewById(R.id.text_view_dnd_status);
        mSwitchSilenceNotifications = v.findViewById(R.id.switch_silence_notifications);
        mButtonStartPause = v.findViewById(R.id.button_start_pause);
        mButtonStop = v.findViewById(R.id.button_stop);
        notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        screenTimeTracker = new ScreenTimeTracker(requireContext());
        mTotalFocusSeconds = screenTimeTracker.getTotalFocusSeconds();

        boolean suppressEnabled = requireContext()
                .getSharedPreferences(PREFS_FOCUS, Context.MODE_PRIVATE)
                .getBoolean(KEY_SUPPRESS_NOTIFICATIONS, false);
        mSwitchSilenceNotifications.setChecked(suppressEnabled);

        mSwitchSilenceNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            requireContext()
                    .getSharedPreferences(PREFS_FOCUS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_SUPPRESS_NOTIFICATIONS, isChecked)
                    .apply();

            if (isChecked && !notificationManager.isNotificationPolicyAccessGranted()) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            }
            if (!isChecked) {
                restoreNotificationsIfNeeded();
            }
            updateDndStatusText();
        });

        updateCountDownText();
        updateTotalFocusText();
        updateDndStatusText();
        updateUIState(); // Sets initial colors/text

        // Start/Pause Button Logic
        mButtonStartPause.setOnClickListener(view -> {
            if (mTimerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        // Stop/Reset Button Logic (The Square)
        mButtonStop.setOnClickListener(view -> resetTimer());

        return v;
    }

    private void startTimer() {
        applySuppressionIfNeeded();
        final long startTimeInSeconds = mTimeLeftInMillis / 1000;

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 100) {
            private long lastProcessedSecond = startTimeInSeconds;

            @Override
            public void onTick(long millisUntilFinished) {
                if (!isAdded() || getView() == null) {
                    return;
                }
                mTimeLeftInMillis = millisUntilFinished;
                long currentSecond = millisUntilFinished / 1000;

                // Precision check: only increment if a full second has passed
                if (currentSecond < lastProcessedSecond) {
                    if (!isBreakTime) {
                        mTotalFocusSeconds++;
                        screenTimeTracker.incrementFocusSeconds(1);
                        updateTotalFocusText();
                    }
                    lastProcessedSecond = currentSecond;
                }
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                if (!isAdded() || getView() == null) {
                    return;
                }
                mTimerRunning = false;
                handleSessionSwitch();
            }
        }.start();

        mTimerRunning = true;
        updateUIState();
    }

    private void pauseTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        restoreNotificationsIfNeeded();
        mTimerRunning = false;
        updateUIState();
    }

    private void resetTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        restoreNotificationsIfNeeded();
        mTimerRunning = false;

        // Reset to full time based on current mode
        mTimeLeftInMillis = isBreakTime ? BREAK_TIME : FOCUS_TIME;

        updateCountDownText();
        updateUIState();
    }

    private void handleSessionSwitch() {
        restoreNotificationsIfNeeded();
        // Switch the state
        isBreakTime = !isBreakTime;
        mTimeLeftInMillis = isBreakTime ? BREAK_TIME : FOCUS_TIME;

        updateCountDownText();
        updateUIState();
    }

    // Interrupted focus session should stop and return to a fresh focus timer.
    private void endSessionAndRestoreTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        restoreNotificationsIfNeeded();
        mTimerRunning = false;
        isBreakTime = false;
        mTimeLeftInMillis = FOCUS_TIME;
        updateCountDownText();
        updateUIState();
    }

    private void updateCountDownText() {
        if (mTextViewTimer == null) {
            return;
        }
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
        mTextViewTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void updateTotalFocusText() {
        if (mTextViewTotalFocus == null || !isAdded()) {
            return;
        }
        long minutes = mTotalFocusSeconds / 60;
        long seconds = mTotalFocusSeconds % 60;
        mTextViewTotalFocus.setText(
                getString(R.string.total_focus_format, minutes, seconds)
        );
    }

    /**
     * Updates all UI elements: Icons, Labels, and Colors based on current state.
     */
    private void updateUIState() {
        if (mButtonStartPause == null) {
            return;
        }
        // 1. Update Start/Pause Icon
        if (mTimerRunning) {
            mButtonStartPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            mButtonStartPause.setImageResource(android.R.drawable.ic_media_play);
        }


        // 3. Update Session Indicator Label & Color
        if (mTextViewSessionType != null) {
            if (isBreakTime) {
                mTextViewSessionType.setText(R.string.break_mode);
                mTextViewSessionType.setTextColor(Color.parseColor("#4CAF50")); // Green for rest
            } else {
                mTextViewSessionType.setText(R.string.focus_mode);
                mTextViewSessionType.setTextColor(Color.parseColor("#2196F3")); // Blue for work
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload today's focus seconds in case the date rolled over since the fragment was last shown
        mTotalFocusSeconds = screenTimeTracker.getTotalFocusSeconds();
        updateTotalFocusText();
        updateDndStatusText();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTimerRunning) {
            restoreNotificationsIfNeeded();
        }
    }

    @Override
    public void onDestroyView() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        restoreNotificationsIfNeeded();
        mTimerRunning = false;
        super.onDestroyView();
    }

    private boolean isSuppressionEnabled() {
        return requireContext()
                .getSharedPreferences(PREFS_FOCUS, Context.MODE_PRIVATE)
                .getBoolean(KEY_SUPPRESS_NOTIFICATIONS, false);
    }

    private void applySuppressionIfNeeded() {
        if (isBreakTime || !isSuppressionEnabled()) {
            return;
        }
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            updateDndStatusText();
            return;
        }
        if (!dndApplied) {
            previousInterruptionFilter = notificationManager.getCurrentInterruptionFilter();
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            dndApplied = true;
            updateDndStatusText();
        }
    }

    private void restoreNotificationsIfNeeded() {
        if (!dndApplied) {
            return;
        }
        if (notificationManager.isNotificationPolicyAccessGranted()) {
            notificationManager.setInterruptionFilter(previousInterruptionFilter);
        }
        dndApplied = false;
        updateDndStatusText();
    }

    private void updateDndStatusText() {
        if (mTextViewDndStatus == null) {
            return;
        }
        if (!isSuppressionEnabled()) {
            mTextViewDndStatus.setText(R.string.dnd_status_off);
            return;
        }
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            mTextViewDndStatus.setText(R.string.dnd_status_permission_needed);
            return;
        }
        mTextViewDndStatus.setText(dndApplied ? R.string.dnd_status_active : R.string.dnd_status_ready);
    }
}