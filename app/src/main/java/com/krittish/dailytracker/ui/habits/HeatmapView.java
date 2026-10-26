package com.krittish.dailytracker.ui.habits;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.krittish.dailytracker.R;
import com.krittish.dailytracker.util.DateUtils;

import java.util.HashMap;
import java.util.Map;

/** Last 12 weeks as a 12 x 7 grid. Darker = done. Tap a day to fix a forgotten tick. */
public class HeatmapView extends View {
    public interface OnDayClickListener { void onDay(long epochDay); }

    private static final int WEEKS = 12;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private Map<Long, String> status = new HashMap<>();
    private long today = DateUtils.today();
    private int doneColor = 0xFF0F766E;
    private float cell;
    private final float gap;
    private OnDayClickListener listener;

    public HeatmapView(Context c) { this(c, null); }

    public HeatmapView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        gap = 4 * c.getResources().getDisplayMetrics().density;
    }

    public void setData(Map<Long, String> statusByDay, int color) {
        this.status = statusByDay;
        this.doneColor = color;
        this.today = DateUtils.today();
        invalidate();
    }

    public void setOnDayClickListener(OnDayClickListener l) { this.listener = l; }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        cell = (w - gap * (WEEKS - 1)) / WEEKS;
        int h = (int) (cell * 7 + gap * 6);
        setMeasuredDimension(w, h);
    }

    private long dayAt(int col, int row) {
        long firstWeek = DateUtils.weekStart(today) - (WEEKS - 1) * 7L;
        return firstWeek + col * 7L + row;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int empty = ContextCompat.getColor(getContext(), R.color.dt_divider);
        int skipped = ContextCompat.getColor(getContext(), R.color.dt_amber);
        float radius = cell / 4f;
        for (int col = 0; col < WEEKS; col++) {
            for (int row = 0; row < 7; row++) {
                long day = dayAt(col, row);
                if (day > today) continue;                           // future days stay blank
                String st = status.get(day);
                paint.setColor("DONE".equals(st) ? doneColor : "SKIPPED".equals(st) ? skipped : empty);
                float left = col * (cell + gap);
                float top = row * (cell + gap);
                rect.set(left, top, left + cell, top + cell);
                canvas.drawRoundRect(rect, radius, radius, paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP && listener != null && cell > 0) {
            int col = (int) (e.getX() / (cell + gap));
            int row = (int) (e.getY() / (cell + gap));
            if (col >= 0 && col < WEEKS && row >= 0 && row < 7) {
                long day = dayAt(col, row);
                if (day <= today) listener.onDay(day);
            }
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() { return super.performClick(); }
}
