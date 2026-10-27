package com.example.campuscat.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.campuscat.R;
import com.example.campuscat.domain.TimeFormatter;
import com.google.android.material.color.MaterialColors;

/** 최근 며칠의 공부 시간을 막대로 보여주는 간단한 차트. 마지막 막대가 오늘이다. */
public class WeekBarChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private long[] minutes = new long[0];
    private String[] labels = new String[0];
    private long goalMinutes;

    public WeekBarChartView(@NonNull Context context) {
        this(context, null);
    }

    public WeekBarChartView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int primary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary);
        int container = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer);
        int muted = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
        int outline = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline);

        barPaint.setColor(container);
        todayPaint.setColor(primary);

        labelPaint.setColor(muted);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(sp(12));

        todayLabelPaint.set(labelPaint);
        todayLabelPaint.setColor(primary);
        todayLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);

        valuePaint.set(labelPaint);
        valuePaint.setTextSize(sp(10));

        goalPaint.setColor(outline);
        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(dp(1));
        goalPaint.setPathEffect(new DashPathEffect(new float[]{dp(4), dp(4)}, 0));

        setContentDescription(context.getString(R.string.chart_description));
    }

    public void setData(@NonNull long[] minutes, @NonNull String[] labels, long goalMinutes) {
        this.minutes = minutes.clone();
        this.labels = labels.clone();
        this.goalMinutes = goalMinutes;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int count = minutes.length;
        if (count == 0) {
            return;
        }
        float left = getPaddingLeft();
        float right = getWidth() - getPaddingRight();
        float top = getPaddingTop() + valuePaint.getTextSize() + dp(4);
        float labelBaseline = getHeight() - getPaddingBottom() - dp(2);
        float bottom = labelBaseline - labelPaint.getTextSize() - dp(6);
        float chartHeight = Math.max(1f, bottom - top);

        long max = Math.max(goalMinutes, 60);
        for (long value : minutes) {
            max = Math.max(max, value);
        }

        float slot = (right - left) / count;
        float barWidth = Math.min(slot * 0.55f, dp(28));
        float radius = barWidth / 2f;

        for (int i = 0; i < count; i++) {
            boolean today = i == count - 1;
            float centerX = left + slot * i + slot / 2f;
            float barHeight = chartHeight * minutes[i] / max;
            if (minutes[i] > 0) {
                barHeight = Math.max(barHeight, radius * 2f);
            }
            if (barHeight > 0) {
                rect.set(centerX - barWidth / 2f, bottom - barHeight, centerX + barWidth / 2f, bottom);
                canvas.drawRoundRect(rect, radius, radius, today ? todayPaint : barPaint);
                canvas.drawText(TimeFormatter.formatCompact(minutes[i]), centerX, rect.top - dp(4), valuePaint);
            }
            if (i < labels.length) {
                canvas.drawText(labels[i], centerX, labelBaseline, today ? todayLabelPaint : labelPaint);
            }
        }

        if (goalMinutes > 0) {
            float goalY = bottom - chartHeight * goalMinutes / max;
            canvas.drawLine(left, goalY, right, goalY, goalPaint);
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
