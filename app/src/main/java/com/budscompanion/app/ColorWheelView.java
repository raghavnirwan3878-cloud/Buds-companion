package com.budscompanion.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

/**
 * HSV color wheel picker.
 * Draws a hue ring; user taps/drags to pick hue.
 * Saturation and Value fixed at 1.0 for vivid accent colors.
 *
 * Reports color via OnColorChangedListener.
 */
public class ColorWheelView extends View {

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private Paint ringPaint;
    private Paint selectorPaint;
    private SweepGradient gradient;

    private float cx, cy, radius, ringWidth;
    private float selectedHue = 193f;   // default teal ~#3098AC
    private OnColorChangedListener listener;

    private static final int[] COLORS = {
            0xFFFF0000, 0xFFFF7F00, 0xFFFFFF00, 0xFF00FF00,
            0xFF00FFFF, 0xFF0000FF, 0xFF7F00FF, 0xFFFF00FF, 0xFFFF0000
    };

    public ColorWheelView(Context context) { super(context); init(); }
    public ColorWheelView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);

        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setColor(Color.WHITE);
        selectorPaint.setStrokeWidth(6f);
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        selectedHue = hsv[0];
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(new float[]{selectedHue, 1f, 1f});
    }

    public void setOnColorChangedListener(OnColorChangedListener l) {
        this.listener = l;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cx = w / 2f;
        cy = h / 2f;
        radius = Math.min(w, h) / 2f * 0.85f;
        ringWidth = Math.min(w, h) / 2f * 0.15f;

        ringPaint.setStrokeWidth(ringWidth);
        gradient = new SweepGradient(cx, cy, COLORS, null);
        ringPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(cx, cy, radius, ringPaint);

        // Draw selector indicator at selected hue position
        double rad = Math.toRadians(selectedHue);
        float sx = cx + (float)(radius * Math.cos(rad));
        float sy = cy + (float)(radius * Math.sin(rad));
        canvas.drawCircle(sx, sy, ringWidth * 0.6f, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
            event.getAction() == MotionEvent.ACTION_MOVE) {

            float dx = event.getX() - cx;
            float dy = event.getY() - cy;
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            if (angle < 0) angle += 360f;

            selectedHue = angle;
            invalidate();

            if (listener != null) listener.onColorChanged(getColor());
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int def = (int)(180 * getContext().getResources().getDisplayMetrics().density);
        int w = resolveSize(def, widthMeasureSpec);
        int h = resolveSize(def, heightMeasureSpec);
        int size = Math.min(w, h);
        setMeasuredDimension(size, size);
    }
}
