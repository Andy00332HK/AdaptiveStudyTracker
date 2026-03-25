package com.example.adaptivestudytracker;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.util.Locale;

public class FocusFragment extends Fragment {

    private TextView mTextViewTimer, mTextViewTotalFocus, mTextViewSessionType;
    private ImageButton mButtonStartPause, mButtonStop;
    private CountDownTimer mCountDownTimer;

    private boolean mTimerRunning = false;
    private boolean isBreakTime = false;

    // Timer durations (in milliseconds)
    private static final long FOCUS_TIME = 1500000; // 25 min
    private static final long BREAK_TIME = 300000;  // 5 min

    private long mTimeLeftInMillis = FOCUS_TIME;
    private long mTotalFocusSeconds = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_focus, container, false);

        mTextViewTimer = v.findViewById(R.id.text_view_timer);
        mTextViewTotalFocus = v.findViewById(R.id.text_view_total_focus);
        mTextViewSessionType = v.findViewById(R.id.text_view_session_type); // Make sure this ID exists in XML
        mButtonStartPause = v.findViewById(R.id.button_start_pause);
        mButtonStop = v.findViewById(R.id.button_stop);

        updateCountDownText();
        updateTotalFocusText();
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
        final long startTimeInSeconds = mTimeLeftInMillis / 1000;

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 100) {
            private long lastProcessedSecond = startTimeInSeconds;

            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                long currentSecond = millisUntilFinished / 1000;

                // Precision check: only increment if a full second has passed
                if (currentSecond < lastProcessedSecond) {
                    if (!isBreakTime) {
                        mTotalFocusSeconds++;
                        updateTotalFocusText();
                    }
                    lastProcessedSecond = currentSecond;
                }
                updateCountDownText();
            }

            @Override
            public void onFinish() {
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
        mTimerRunning = false;
        updateUIState();
    }

    private void resetTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        mTimerRunning = false;

        // Reset to full time based on current mode
        mTimeLeftInMillis = isBreakTime ? BREAK_TIME : FOCUS_TIME;

        updateCountDownText();
        updateUIState();
    }

    private void handleSessionSwitch() {
        // Switch the state
        isBreakTime = !isBreakTime;
        mTimeLeftInMillis = isBreakTime ? BREAK_TIME : FOCUS_TIME;

        updateCountDownText();
        updateUIState();
    }

    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
        mTextViewTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void updateTotalFocusText() {
        long minutes = mTotalFocusSeconds / 60;
        long seconds = mTotalFocusSeconds % 60;
        mTextViewTotalFocus.setText(
                String.format(Locale.getDefault(), "Total Focused: %02d:%02d", minutes, seconds)
        );
    }

    /**
     * Updates all UI elements: Icons, Labels, and Colors based on current state.
     */
    private void updateUIState() {
        // 1. Update Start/Pause Icon
        if (mTimerRunning) {
            mButtonStartPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            mButtonStartPause.setImageResource(android.R.drawable.ic_media_play);
        }


        // 3. Update Session Indicator Label & Color
        if (mTextViewSessionType != null) {
            if (isBreakTime) {
                mTextViewSessionType.setText("BREAK MODE");
                mTextViewSessionType.setTextColor(Color.parseColor("#4CAF50")); // Green for rest
            } else {
                mTextViewSessionType.setText("FOCUS MODE");
                mTextViewSessionType.setTextColor(Color.parseColor("#2196F3")); // Blue for work
            }
        }
    }
}