package com.budscompanion.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

/**
 * Circular tick-gauge battery indicator.
 * Draws N tick marks around an arc; filled ticks = charged segments.
 * Used in DeviceListFragment and DeviceDetailFragment.
 *
 * XML attrs (optional):
 *   app:tickCount      — number of ticks (default 20)
 *   app:tickColor      — filled tick color  (default accent teal)
 *   app:emptyColor     — empty tick color
 *   app:tickThickness  — stroke width in dp (default 3)
 */
public class BatteryTickView extends View {

    private static final int DEFAULT_TICKS = 20;
    private static final float SWEEP_DEGREES = 240f;  // arc spans 240°, gap at bottom
    private static final float START_ANGLE  = 150f;   // start at bottom-left

    private int tickCount = DEFAULT_TICKS;
    private int level = 0;           // 0–100
    private float tickThicknessDp = 3f;

    private Paint filledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint emptyPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF arcRect = new RectF();

    public BatteryTickView(Context context) {
        super(context);
        init(context, null);
    }

    public BatteryTickView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BatteryTickView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        int accentColor = 0xFF3098AC;   // teal accent
        int emptyColor  = 0x33FFFFFF;   // translucent white

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BatteryTickView);
            tickCount       = a.getInt(R.styleable.BatteryTickView_tickCount, DEFAULT_TICKS);
            accentColor     = a.getColor(R.styleable.BatteryTickView_tickColor, accentColor);
            emptyColor      = a.getColor(R.styleable.BatteryTickView_emptyColor, emptyColor);
            tickThicknessDp = a.getDimension(R.styleable.BatteryTickView_tickThickness,
                    tickThicknessDp * context.getResources().getDisplayMetrics().density)
                    / context.getResources().getDisplayMetrics().density;
            a.recycle();
        }

        float density = context.getResources().getDisplayMetrics().density;

        filledPaint.setStyle(Paint.Style.STROKE);
        filledPaint.setColor(accentColor);
        filledPaint.setStrokeWidth(tickThicknessDp * density);
        filledPaint.setStrokeCap(Paint.Cap.ROUND);

        emptyPaint.setStyle(Paint.Style.STROKE);
        emptyPaint.setColor(emptyColor);
        emptyPaint.setStrokeWidth(tickThicknessDp * density);
        emptyPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /** Set battery level 0–100. Invalidates. */
    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(100, level));
        invalidate();
    }

    public int getLevel() { return level; }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float padding = filledPaint.getStrokeWidth() / 2f + 4;
        arcRect.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int filledTicks = Math.round(level / 100f * tickCount);
        float gapDegrees = 1.5f;  // gap between ticks
        float tickSweep = (SWEEP_DEGREES / tickCount) - gapDegrees;

        for (int i = 0; i < tickCount; i++) {
            float startAngle = START_ANGLE + i * (SWEEP_DEGREES / tickCount);
            Paint paint = (i < filledTicks) ? filledPaint : emptyPaint;
            canvas.drawArc(arcRect, startAngle, tickSweep, false, paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Square view; default 80dp
        int defaultSize = (int) (80 * getContext().getResources().getDisplayMetrics().density);
        int w = resolveSize(defaultSize, widthMeasureSpec);
        int h = resolveSize(defaultSize, heightMeasureSpec);
        int size = Math.min(w, h);
        setMeasuredDimension(size, size);
    }
}
