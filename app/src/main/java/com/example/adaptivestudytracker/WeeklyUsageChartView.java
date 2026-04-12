package com.example.adaptivestudytracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeeklyUsageChartView extends View {

    private final Paint focusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint screenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint screenValuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private List<Long> focusSeconds = new ArrayList<>();
    private List<Long> screenMs = new ArrayList<>();
    private final List<String> dayLabels = new ArrayList<>();

    private final float horizontalPadding;
    private final float chartTop;
    private final float chartBottom;
    private final float barGap;

    public WeeklyUsageChartView(Context context) {
        this(context, null);
    }

    public WeeklyUsageChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        focusPaint.setColor(0xFF2563EB);
        screenPaint.setColor(0xFF10B981);
        gridPaint.setColor(0xFFE5E7EB);
        gridPaint.setStrokeWidth(dp(1f));

        labelPaint.setColor(0xFF6B7280);
        labelPaint.setTextSize(sp(12f));

        valuePaint.setColor(0xFF1D4ED8);
        valuePaint.setTextSize(sp(10f));

        screenValuePaint.setColor(0xFF047857);
        screenValuePaint.setTextSize(sp(10f));

        horizontalPadding = dp(14f);
        chartTop = dp(18f);
        chartBottom = dp(34f);
        barGap = dp(4f);

        setMinimumHeight((int) dp(220f));
    }

    public void setData(LocalDate weekStart, List<Long> focusSeconds, List<Long> screenMs) {
        this.focusSeconds = focusSeconds == null ? new ArrayList<>() : new ArrayList<>(focusSeconds);
        this.screenMs = screenMs == null ? new ArrayList<>() : new ArrayList<>(screenMs);

        int size = Math.min(this.focusSeconds.size(), this.screenMs.size());
        dayLabels.clear();
        LocalDate labelsStart = weekStart == null ? LocalDate.now() : weekStart;
        for (int i = 0; i < size; i++) {
            dayLabels.add(labelsStart.plusDays(i).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()));
        }
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int size = Math.min(focusSeconds.size(), screenMs.size());
        if (size == 0) {
            return;
        }

        float width = getWidth();
        float height = getHeight();
        float top = chartTop;
        float bottom = height - chartBottom;
        float left = horizontalPadding;
        float right = width - horizontalPadding;
        float drawableHeight = (bottom - top) * 0.85f;

        // True unified linear Y-axis — both series always scale relative to the same maximum.
        // If focus time is 25min and screen time is 3h, the focus bar will be ~14% as tall.
        float globalMaxSeconds = 1f;
        for (int i = 0; i < size; i++) {
            globalMaxSeconds = Math.max(globalMaxSeconds, focusSeconds.get(i));
            globalMaxSeconds = Math.max(globalMaxSeconds, screenMs.get(i) / 1000f);
        }

        for (int i = 0; i < 4; i++) {
            float y = top + (bottom - top) * i / 3f;
            canvas.drawLine(left, y, right, y, gridPaint);
        }

        float groupWidth = (right - left) / size;
        float singleBarWidth = (groupWidth - barGap) / 2f;

        for (int i = 0; i < size; i++) {
            float groupLeft = left + i * groupWidth;

            // Both bars divide by the same globalMaxSeconds — strict proportional scale.
            float focusHeight = (focusSeconds.get(i).floatValue() / globalMaxSeconds) * drawableHeight;
            float screenHeight = ((screenMs.get(i) / 1000f) / globalMaxSeconds) * drawableHeight;

            // Show a 1dp stub only for non-zero values so even tiny amounts are visible.
            if (focusSeconds.get(i) > 0 && focusHeight < dp(1f)) {
                focusHeight = dp(1f);
            }
            if (screenMs.get(i) > 0 && screenHeight < dp(1f)) {
                screenHeight = dp(1f);
            }

            float focusLeft = groupLeft + dp(2f);
            float focusRight = focusLeft + singleBarWidth;
            float screenLeft = focusRight + barGap;
            float screenRight = screenLeft + singleBarWidth;

            canvas.drawRoundRect(focusLeft, bottom - focusHeight, focusRight, bottom, dp(3f), dp(3f), focusPaint);
            canvas.drawRoundRect(screenLeft, bottom - screenHeight, screenRight, bottom, dp(3f), dp(3f), screenPaint);

            String focusLabel = formatShortDuration(focusSeconds.get(i));
            float labelWidth = valuePaint.measureText(focusLabel);
            float labelX = focusLeft + (singleBarWidth - labelWidth) / 2f;
            float labelY = Math.max(top + dp(12f), bottom - focusHeight - dp(4f));
            canvas.drawText(focusLabel, labelX, labelY, valuePaint);

            long rawScreenSec = screenMs.get(i) / 1000;
            String screenLabel = formatShortDuration(rawScreenSec);
            float screenLabelWidth = screenValuePaint.measureText(screenLabel);
            float screenLabelX = screenLeft + (singleBarWidth - screenLabelWidth) / 2f;
            float screenLabelY = Math.max(top + dp(12f), bottom - screenHeight - dp(4f));
            canvas.drawText(screenLabel, screenLabelX, screenLabelY, screenValuePaint);

            if (i < dayLabels.size()) {
                String day = dayLabels.get(i);
                float textWidth = labelPaint.measureText(day);
                float textX = groupLeft + (groupWidth - textWidth) / 2f;
                canvas.drawText(day, textX, height - dp(10f), labelPaint);
            }
        }
    }

    private String formatShortDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh", hours);
        }
        return String.format(Locale.getDefault(), "%dm", minutes);
    }


    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
