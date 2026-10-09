package dev.bennett.codexmeter;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Static emerald halo kept inside the cell. No animation, software layer or bitmap allocation. */
final class CalendarGlowDrawable extends Drawable {
    private static final int[] HALO = {0x1834D399, 0x3034D399, 0x5034D399, 0xFF087653};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private final boolean selected, today, dark;
    private int alpha = 255;

    CalendarGlowDrawable(float density, boolean selected, boolean today, boolean dark) {
        this.density = density; this.selected = selected; this.today = today; this.dark = dark;
    }

    @Override public void draw(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        // Four translucent rings give a visible halo without bleeding into adjacent dates.
        for (int layer = 0; layer < 4; layer++) {
            rect.set(getBounds()); rect.inset((1 + layer) * density, (1 + layer) * density);
            color(HALO[layer]);
            canvas.drawRoundRect(rect, 10 * density, 10 * density, paint);
        }
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth((selected ? 2 : 1) * density);
        color(selected ? (dark ? 0xFFFFFFFF : 0xFF062F23) : today ? 0xFFFFFFFF : 0xFF6EE7B7);
        canvas.drawRoundRect(rect, 10 * density, 10 * density, paint);
    }

    private void color(int value) { paint.setColor(value); paint.setAlpha((value >>> 24) * alpha / 255); }
    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
