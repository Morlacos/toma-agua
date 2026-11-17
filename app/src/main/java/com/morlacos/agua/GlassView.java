package com.morlacos.agua;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** A glass whose water level shows how much time is left until the next reminder. */
public class GlassView extends View {
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint water = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cup = new Path();
    private final Path wave = new Path();
    private float level = 1f, shown = 1f, phase = 0f;
    private boolean animate = false;
    private final float d;

    public GlassView(Context c, AttributeSet a) {
        super(c, a);
        d = getResources().getDisplayMetrics().density;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(4 * d);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setColor(c.getColor(R.color.water_dark));
        fill.setColor(c.getColor(R.color.glass_bg));
        shine.setColor(0x40FFFFFF);
    }

    public void setLevel(float f) {
        level = Math.max(0f, Math.min(1f, f));
        invalidate();
    }

    public void setAnimating(boolean on) {
        animate = on;
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        float m = 4 * d;
        cup.reset();
        cup.moveTo(m, m);
        cup.lineTo(w - m, m);
        cup.lineTo(w * 0.80f, h - m - 10 * d);
        cup.quadTo(w * 0.79f, h - m, w * 0.72f, h - m);
        cup.lineTo(w * 0.28f, h - m);
        cup.quadTo(w * 0.21f, h - m, w * 0.20f, h - m - 10 * d);
        cup.close();
    }

    @Override protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        if (w == 0) return;
        shown += (level - shown) * 0.08f;
        float m = 4 * d;
        float top = m + (1f - shown) * (h - 2 * m);

        cv.drawPath(cup, fill);
        cv.save();
        cv.clipPath(cup);
        wave.reset();
        float amp = 5 * d;
        wave.moveTo(0, top);
        for (int x = 0; x <= w; x += 4) {
            double t = (x / (double) w) * Math.PI * 3 + phase;
            wave.lineTo(x, top + (float) Math.sin(t) * amp);
        }
        wave.lineTo(w, h);
        wave.lineTo(0, h);
        wave.close();
        water.setShader(new LinearGradient(0, top, 0, h,
                getContext().getColor(R.color.water), getContext().getColor(R.color.water_dark),
                Shader.TileMode.CLAMP));
        cv.drawPath(wave, water);
        cv.drawRoundRect(new RectF(w * 0.27f, h * 0.12f, w * 0.27f + 9 * d, h * 0.72f), 6 * d, 6 * d, shine);
        cv.restore();
        cv.drawPath(cup, stroke);

        boolean settling = Math.abs(level - shown) > 0.002f;
        if (animate || settling) {
            phase += 0.05f;
            postInvalidateOnAnimation();
        }
    }
}
