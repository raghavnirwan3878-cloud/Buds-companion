package com.budscompanion.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class BatteryTickView extends View {

    private static final int   DEFAULT_TICKS  = 20;
    private static final float SWEEP_DEGREES  = 240f;
    private static final float START_ANGLE    = 150f;

    private int   tickCount       = DEFAULT_TICKS;
    private int   level           = 0;
    private float tickStrokePx;          // stored in pixels, set once in init()

    private final Paint filledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect     = new RectF();

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
        float density = context.getResources().getDisplayMetrics().density;

        int   accentColor    = 0xFF3098AC;
        int   emptyColor     = 0x33FFFFFF;
        float defaultStrokePx = 3f * density;   // 3dp default

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BatteryTickView);
            tickCount     = a.getInt(R.styleable.BatteryTickView_tickCount, DEFAULT_TICKS);
            accentColor   = a.getColor(R.styleable.BatteryTickView_tickColor, accentColor);
            emptyColor    = a.getColor(R.styleable.BatteryTickView_emptyColor, emptyColor);
            // getDimension() already returns pixels — no manual density multiply needed
            defaultStrokePx = a.getDimension(
                    R.styleable.BatteryTickView_tickThickness, defaultStrokePx);
            a.recycle();
        }

        tickStrokePx = defaultStrokePx;

        filledPaint.setStyle(Paint.Style.STROKE);
        filledPaint.setColor(accentColor);
        filledPaint.setStrokeWidth(tickStrokePx);
        filledPaint.setStrokeCap(Paint.Cap.ROUND);

        emptyPaint.setStyle(Paint.Style.STROKE);
        emptyPaint.setColor(emptyColor);
        emptyPaint.setStrokeWidth(tickStrokePx);
        emptyPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(100, level));
        invalidate();
    }

    public int getLevel() { return level; }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float padding = tickStrokePx / 2f + 4;
        arcRect.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int filledTicks = Math.round(level / 100f * tickCount);
        float gapDegrees = 1.5f;
        float tickSweep  = (SWEEP_DEGREES / tickCount) - gapDegrees;

        for (int i = 0; i < tickCount; i++) {
            float startAngle = START_ANGLE + i * (SWEEP_DEGREES / tickCount);
            canvas.drawArc(arcRect, startAngle, tickSweep, false,
                    i < filledTicks ? filledPaint : emptyPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultSize = (int)(80 * getContext().getResources().getDisplayMetrics().density);
        int w    = resolveSize(defaultSize, widthMeasureSpec);
        int h    = resolveSize(defaultSize, heightMeasureSpec);
        int size = Math.min(w, h);
        setMeasuredDimension(size, size);
    }
}
