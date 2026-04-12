package com.example.adaptivestudytracker;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class FocusFragment extends Fragment {

    // ── Mode ────────────────────────────────────────────────────────────────
    private enum FocusMode { POMODORO, INFINITE, COUNTDOWN }
    private FocusMode currentMode = FocusMode.POMODORO;

    // ── Views ────────────────────────────────────────────────────────────────
    private TextView mTextViewTimer, mTextViewTotalFocus, mTextViewSessionType, mTextViewDndStatus;
    private TextView mTextCountdownDuration;
    private View mCardCountdownDuration;
    private ImageButton mButtonStartPause, mButtonStop;
    private SwitchMaterial mSwitchSilenceNotifications;
    private MaterialButtonToggleGroup mToggleFocusMode;

    // ── State ────────────────────────────────────────────────────────────────
    private boolean mTimerRunning = false;
    private boolean isBreakTime = false;    // Pomodoro only
    private long mTotalFocusSeconds = 0;

    // ── Pomodoro / Countdown (CountDownTimer) ───────────────────────────────
    private static final long POMODORO_FOCUS_MS = 25 * 60 * 1000L;
    private static final long POMODORO_BREAK_MS = 5 * 60 * 1000L;
    private long mTimeLeftInMillis = POMODORO_FOCUS_MS;
    private CountDownTimer mCountDownTimer;

    // ── Countdown custom duration ────────────────────────────────────────────
    private long mCountdownDurationMs = 30 * 60 * 1000L; // default 30 min

    // ── Infinite (count-up) ──────────────────────────────────────────────────
    private long mInfiniteElapsedSeconds = 0L;
    private final Handler mInfiniteHandler = new Handler(Looper.getMainLooper());
    private Runnable mInfiniteRunnable;

    // ── DND ──────────────────────────────────────────────────────────────────
    private NotificationManager notificationManager;
    private int previousInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL;
    private boolean dndApplied = false;

    private static final String PREFS_FOCUS = "focus_prefs";
    private static final String KEY_SUPPRESS_NOTIFICATIONS = "suppress_notifications";

    private ScreenTimeTracker screenTimeTracker;

    // ── Called by MainActivity when user leaves the Focus tab ────────────────
    public boolean onTabSwitchedAway() {
        boolean wasFocusRunning = mTimerRunning
                && (currentMode != FocusMode.POMODORO || !isBreakTime);
        endSessionAndRestoreTimer();
        return wasFocusRunning;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_focus, container, false);

        mTextViewTimer = v.findViewById(R.id.text_view_timer);
        mTextViewTotalFocus = v.findViewById(R.id.text_view_total_focus);
        mTextViewSessionType = v.findViewById(R.id.text_view_session_type);
        mTextViewDndStatus = v.findViewById(R.id.text_view_dnd_status);
        mSwitchSilenceNotifications = v.findViewById(R.id.switch_silence_notifications);
        mButtonStartPause = v.findViewById(R.id.button_start_pause);
        mButtonStop = v.findViewById(R.id.button_stop);
        mToggleFocusMode = v.findViewById(R.id.toggle_focus_mode);
        mCardCountdownDuration = v.findViewById(R.id.card_countdown_duration);
        mTextCountdownDuration = v.findViewById(R.id.text_countdown_duration);

        notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        screenTimeTracker = new ScreenTimeTracker(requireContext());
        mTotalFocusSeconds = screenTimeTracker.getTotalFocusSeconds();

        // Restore DND pref
        boolean suppressEnabled = requireContext()
                .getSharedPreferences(PREFS_FOCUS, Context.MODE_PRIVATE)
                .getBoolean(KEY_SUPPRESS_NOTIFICATIONS, false);
        mSwitchSilenceNotifications.setChecked(suppressEnabled);
        mSwitchSilenceNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            requireContext().getSharedPreferences(PREFS_FOCUS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_SUPPRESS_NOTIFICATIONS, isChecked).apply();
            if (isChecked && !notificationManager.isNotificationPolicyAccessGranted()) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            }
            if (!isChecked) restoreNotificationsIfNeeded();
            updateDndStatusText();
        });

        // Mode toggle
        mToggleFocusMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_mode_pomodoro)  switchMode(FocusMode.POMODORO);
            else if (checkedId == R.id.btn_mode_infinite) switchMode(FocusMode.INFINITE);
            else if (checkedId == R.id.btn_mode_countdown) switchMode(FocusMode.COUNTDOWN);
        });

        // Countdown duration card
        mCardCountdownDuration.setOnClickListener(view -> showDurationPickerDialog());

        // Start/Pause and Stop buttons
        mButtonStartPause.setOnClickListener(view -> {
            if (mTimerRunning) pauseTimer();
            else startTimer();
        });
        mButtonStop.setOnClickListener(view -> resetTimer());

        refreshUI();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTotalFocusSeconds = screenTimeTracker.getTotalFocusSeconds();
        updateTotalFocusText();
        updateDndStatusText();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTimerRunning) restoreNotificationsIfNeeded();
    }

    @Override
    public void onDestroyView() {
        cancelAllTimers();
        restoreNotificationsIfNeeded();
        mTimerRunning = false;
        super.onDestroyView();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Mode switching
    // ═════════════════════════════════════════════════════════════════════════

    private void switchMode(FocusMode mode) {
        if (currentMode == mode) return;
        if (mTimerRunning) endSessionAndRestoreTimer();
        currentMode = mode;
        isBreakTime = false;
        resetTimerState();
        refreshUI();
    }

    private void resetTimerState() {
        switch (currentMode) {
            case POMODORO:  mTimeLeftInMillis = POMODORO_FOCUS_MS; break;
            case COUNTDOWN: mTimeLeftInMillis = mCountdownDurationMs; break;
            case INFINITE:  mInfiniteElapsedSeconds = 0; break;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Start / Pause / Reset / End
    // ═════════════════════════════════════════════════════════════════════════

    private void startTimer() {
        applySuppressionIfNeeded();
        switch (currentMode) {
            case POMODORO:  startPomodoroTimer(); break;
            case COUNTDOWN: startCountdownTimer(); break;
            case INFINITE:  startInfiniteTimer(); break;
        }
        mTimerRunning = true;
        updateUIState();
    }

    private void startPomodoroTimer() {
        final long startSec = mTimeLeftInMillis / 1000;
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 100) {
            private long lastSec = startSec;

            @Override public void onTick(long ms) {
                if (!isAdded() || getView() == null) return;
                mTimeLeftInMillis = ms;
                long cur = ms / 1000;
                if (cur < lastSec) {
                    if (!isBreakTime) {
                        mTotalFocusSeconds++;
                        screenTimeTracker.incrementFocusSeconds(1);
                        updateTotalFocusText();
                    }
                    lastSec = cur;
                }
                updateTimerText();
            }

            @Override public void onFinish() {
                if (!isAdded() || getView() == null) return;
                mTimerRunning = false;
                handlePomodoroSessionSwitch();
            }
        }.start();
    }

    private void startCountdownTimer() {
        final long startSec = mTimeLeftInMillis / 1000;
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 100) {
            private long lastSec = startSec;

            @Override public void onTick(long ms) {
                if (!isAdded() || getView() == null) return;
                mTimeLeftInMillis = ms;
                long cur = ms / 1000;
                if (cur < lastSec) {
                    mTotalFocusSeconds++;
                    screenTimeTracker.incrementFocusSeconds(1);
                    updateTotalFocusText();
                    lastSec = cur;
                }
                updateTimerText();
            }

            @Override public void onFinish() {
                if (!isAdded() || getView() == null) return;
                mTimerRunning = false;
                mTimeLeftInMillis = 0;
                updateTimerText();
                updateUIState();
                restoreNotificationsIfNeeded();
            }
        }.start();
    }

    private void startInfiniteTimer() {
        mInfiniteRunnable = new Runnable() {
            @Override public void run() {
                if (!isAdded() || getView() == null || !mTimerRunning) return;
                mInfiniteElapsedSeconds++;
                mTotalFocusSeconds++;
                screenTimeTracker.incrementFocusSeconds(1);
                updateTimerText();
                updateTotalFocusText();
                mInfiniteHandler.postDelayed(this, 1000);
            }
        };
        mInfiniteHandler.postDelayed(mInfiniteRunnable, 1000);
    }

    private void pauseTimer() {
        if (currentMode == FocusMode.INFINITE) {
            mInfiniteHandler.removeCallbacks(mInfiniteRunnable);
        } else {
            if (mCountDownTimer != null) mCountDownTimer.cancel();
        }
        restoreNotificationsIfNeeded();
        mTimerRunning = false;
        updateUIState();
    }

    private void resetTimer() {
        cancelAllTimers();
        restoreNotificationsIfNeeded();
        mTimerRunning = false;
        isBreakTime = false;
        resetTimerState();
        updateTimerText();
        updateUIState();
        updateSessionLabel();
    }

    private void handlePomodoroSessionSwitch() {
        restoreNotificationsIfNeeded();
        isBreakTime = !isBreakTime;
        mTimeLeftInMillis = isBreakTime ? POMODORO_BREAK_MS : POMODORO_FOCUS_MS;
        updateTimerText();
        updateUIState();
        updateSessionLabel();
    }

    private void endSessionAndRestoreTimer() {
        cancelAllTimers();
        restoreNotificationsIfNeeded();
        mTimerRunning = false;
        isBreakTime = false;
        resetTimerState();
        updateTimerText();
        updateUIState();
        updateSessionLabel();
    }

    private void cancelAllTimers() {
        if (mCountDownTimer != null) { mCountDownTimer.cancel(); mCountDownTimer = null; }
        if (mInfiniteRunnable != null) mInfiniteHandler.removeCallbacks(mInfiniteRunnable);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Duration picker dialog
    // ═════════════════════════════════════════════════════════════════════════

    private void showDurationPickerDialog() {
        if (mTimerRunning) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_duration_picker, null);
        NumberPicker hourPicker = dialogView.findViewById(R.id.picker_hours);
        NumberPicker minPicker = dialogView.findViewById(R.id.picker_minutes);
        hourPicker.setMinValue(0); hourPicker.setMaxValue(23);
        minPicker.setMinValue(0);  minPicker.setMaxValue(59);

        long totalSec = mCountdownDurationMs / 1000;
        hourPicker.setValue((int) (totalSec / 3600));
        minPicker.setValue((int) ((totalSec % 3600) / 60));

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.set_duration_title))
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    long ms = (hourPicker.getValue() * 3600L + minPicker.getValue() * 60L) * 1000L;
                    if (ms < 60_000L) ms = 60_000L; // minimum 1 minute
                    mCountdownDurationMs = ms;
                    mTimeLeftInMillis = ms;
                    updateCountdownDurationLabel();
                    updateTimerText();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UI update helpers
    // ═════════════════════════════════════════════════════════════════════════

    private void refreshUI() {
        updateTimerText();
        updateTotalFocusText();
        updateDndStatusText();
        updateUIState();
        updateSessionLabel();
        updateCountdownDurationLabel();
        mCardCountdownDuration.setVisibility(
                currentMode == FocusMode.COUNTDOWN ? View.VISIBLE : View.GONE);
    }

    private void updateTimerText() {
        if (mTextViewTimer == null) return;
        if (currentMode == FocusMode.INFINITE) {
            long h = mInfiniteElapsedSeconds / 3600;
            long m = (mInfiniteElapsedSeconds % 3600) / 60;
            long s = mInfiniteElapsedSeconds % 60;
            mTextViewTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
        } else {
            int min = (int) (mTimeLeftInMillis / 1000) / 60;
            int sec = (int) (mTimeLeftInMillis / 1000) % 60;
            mTextViewTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
        }
    }

    private void updateTotalFocusText() {
        if (mTextViewTotalFocus == null || !isAdded()) return;
        long min = mTotalFocusSeconds / 60;
        long sec = mTotalFocusSeconds % 60;
        mTextViewTotalFocus.setText(getString(R.string.total_focus_format, min, sec));
    }

    private void updateSessionLabel() {
        if (mTextViewSessionType == null) return;
        switch (currentMode) {
            case POMODORO:
                if (isBreakTime) {
                    mTextViewSessionType.setText(R.string.break_mode);
                    mTextViewSessionType.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    mTextViewSessionType.setText(R.string.focus_mode);
                    mTextViewSessionType.setTextColor(Color.parseColor("#2196F3"));
                }
                break;
            case INFINITE:
                mTextViewSessionType.setText(R.string.focus_mode_infinite_label);
                mTextViewSessionType.setTextColor(Color.parseColor("#7C3AED"));
                break;
            case COUNTDOWN:
                mTextViewSessionType.setText(R.string.focus_mode_countdown_label);
                mTextViewSessionType.setTextColor(Color.parseColor("#D97706"));
                break;
        }
    }

    private void updateCountdownDurationLabel() {
        if (mTextCountdownDuration == null) return;
        long totalSec = mCountdownDurationMs / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        if (h > 0) {
            mTextCountdownDuration.setText(
                    String.format(Locale.getDefault(), "%dh %02dm", h, m));
        } else {
            mTextCountdownDuration.setText(
                    String.format(Locale.getDefault(), "%d min", m));
        }
    }

    private void updateUIState() {
        if (mButtonStartPause == null) return;
        mButtonStartPause.setImageResource(
                mTimerRunning ? android.R.drawable.ic_media_pause
                              : android.R.drawable.ic_media_play);
        updateSessionLabel();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DND helpers (unchanged)
    // ═════════════════════════════════════════════════════════════════════════

    private boolean isSuppressionEnabled() {
        return requireContext().getSharedPreferences(PREFS_FOCUS, Context.MODE_PRIVATE)
                .getBoolean(KEY_SUPPRESS_NOTIFICATIONS, false);
    }

    private void applySuppressionIfNeeded() {
        if ((currentMode == FocusMode.POMODORO && isBreakTime) || !isSuppressionEnabled()) return;
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            updateDndStatusText(); return;
        }
        if (!dndApplied) {
            previousInterruptionFilter = notificationManager.getCurrentInterruptionFilter();
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            dndApplied = true;
            updateDndStatusText();
        }
    }

    private void restoreNotificationsIfNeeded() {
        if (!dndApplied) return;
        if (notificationManager.isNotificationPolicyAccessGranted()) {
            notificationManager.setInterruptionFilter(previousInterruptionFilter);
        }
        dndApplied = false;
        updateDndStatusText();
    }

    private void updateDndStatusText() {
        if (mTextViewDndStatus == null) return;
        if (!isSuppressionEnabled()) {
            mTextViewDndStatus.setText(R.string.dnd_status_off); return;
        }
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            mTextViewDndStatus.setText(R.string.dnd_status_permission_needed); return;
        }
        mTextViewDndStatus.setText(dndApplied ? R.string.dnd_status_active : R.string.dnd_status_ready);
    }
}

