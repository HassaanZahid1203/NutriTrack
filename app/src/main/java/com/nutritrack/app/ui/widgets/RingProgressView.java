package com.nutritrack.app.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.nutritrack.app.R;

/**
 * Donut/ring progress view for the dashboard calorie indicator.
 * Custom view chosen instead of pulling another lib (SRS minimal-dependency spirit).
 */
public class RingProgressView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private int progress = 0;          // 0..100
    private String value = "0";
    private String label = "";

    public RingProgressView(Context c) { this(c, null); }
    public RingProgressView(Context c, @Nullable AttributeSet attrs) { this(c, attrs, 0); }
    public RingProgressView(Context c, @Nullable AttributeSet attrs, int defStyle) {
        super(c, attrs, defStyle);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(0x33FFFFFF);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(Color.WHITE);

        valuePaint.setColor(Color.WHITE);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);

        labelPaint.setColor(0xCCFFFFFF);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        if (attrs != null) {
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.RingProgressView);
            progress = a.getInt(R.styleable.RingProgressView_rpv_progress, 0);
            String v = a.getString(R.styleable.RingProgressView_rpv_value);
            String l = a.getString(R.styleable.RingProgressView_rpv_label);
            if (v != null) value = v;
            if (l != null) label = l;
            a.recycle();
        }
    }

    public void setProgress(int p) {
        progress = Math.max(0, Math.min(100, p));
        invalidate();
    }

    public void setValue(String v) { value = v; invalidate(); }
    public void setLabel(String l) { label = l; invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float strokeWidth = Math.min(getWidth(), getHeight()) * 0.085f;
        trackPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeWidth(strokeWidth);

        float pad = strokeWidth / 2 + 2;
        arcRect.set(pad, pad, getWidth() - pad, getHeight() - pad);

        canvas.drawArc(arcRect, 0, 360, false, trackPaint);
        canvas.drawArc(arcRect, -90, 360 * (progress / 100f), false, progressPaint);

        // Center text: big value + small label below
        valuePaint.setTextSize(getWidth() * 0.22f);
        labelPaint.setTextSize(getWidth() * 0.10f);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        canvas.drawText(value, cx, cy + valuePaint.getTextSize() / 3, valuePaint);
        canvas.drawText(label, cx, cy + valuePaint.getTextSize() * 0.95f, labelPaint);
    }
}
