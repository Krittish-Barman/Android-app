package com.krittish.dailytracker.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.krittish.dailytracker.R;

/** Small custom bar chart. The last bar (today) uses the accent colour. */
public class BarChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float[] values = new float[0];
    private String[] labels = new String[0];

    public BarChartView(Context c) { this(c, null); }

    public BarChartView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(11 * c.getResources().getDisplayMetrics().scaledDensity);
        text.setColor(ContextCompat.getColor(c, R.color.dt_muted));
    }

    public void setData(float[] values, String[] labels) {
        this.values = values;
        this.labels = labels;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = values.length;
        if (n == 0) return;
        float max = 1;
        for (float v : values) max = Math.max(max, v);
        float labelH = text.getTextSize() + 8;
        float chartH = getHeight() - labelH;
        float slot = (float) getWidth() / n;
        float barW = slot * 0.6f;
        int primary = ContextCompat.getColor(getContext(), R.color.dt_primary);
        int accent = ContextCompat.getColor(getContext(), R.color.dt_accent);
        int empty = ContextCompat.getColor(getContext(), R.color.dt_divider);
        for (int i = 0; i < n; i++) {
            float h = values[i] / max * (chartH - 4);
            float cx = slot * i + slot / 2;
            paint.setColor(values[i] <= 0 ? empty : (i == n - 1 ? accent : primary));
            float barH = values[i] <= 0 ? 4 : Math.max(6, h);
            rect.set(cx - barW / 2, chartH - barH, cx + barW / 2, chartH);
            canvas.drawRoundRect(rect, 6, 6, paint);
            if (i < labels.length && labels[i] != null) canvas.drawText(labels[i], cx, getHeight() - 4, text);
        }
    }
}
