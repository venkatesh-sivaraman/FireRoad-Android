package com.base12innovations.android.fireroad.utils;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;

import java.util.List;

public class DocumentIconView extends View {

    public DocumentIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        filler = new Paint();
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public enum IconShape {
        ROUNDED_RECT, CIRCLE
    }

    public IconShape iconShape = IconShape.ROUNDED_RECT;

    public static class ColorSector {
        public int color;
        public float proportion;

        public ColorSector(int c, float p) {
            this.color = c;
            this.proportion = p;
        }
    }

    private Paint filler;
    private List<ColorSector> sectors;
    private BlurMaskFilter blur = new BlurMaskFilter(30, BlurMaskFilter.Blur.NORMAL);

    public void setColorSectors(List<ColorSector> colors) {
        sectors = colors;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = (float)getWidth();
        float height = (float)getHeight();
        Path p = new Path();
        if (iconShape == IconShape.ROUNDED_RECT) {
            float[] radii = new float[] {
                    width / 4.0f,width / 4.0f,width / 4.0f,width / 4.0f,
                    width / 4.0f,width / 4.0f,width / 4.0f,width / 4.0f
            };
            p.addRoundRect(0.0f, 0.0f, width, height, radii, Path.Direction.CW);
        } else if (iconShape == IconShape.CIRCLE) {
            p.addCircle(width / 2.0f, height / 2.0f, Math.min(width / 2.0f, height / 2.0f), Path.Direction.CW);
        }
        canvas.clipPath(p);

        // Draw sectors
        if (sectors != null) {
            float currentAngle = 0.0f;
            float inset = -width / 2.0f;
            for (ColorSector sector : sectors) {
                filler.setColor(sector.color);
                filler.setStyle(Paint.Style.FILL);
                filler.setMaskFilter(blur);

                float newAngle = currentAngle + sector.proportion * 360.0f;
                if (sector.proportion < 0.999f) {
                    canvas.drawArc(inset, inset, width - inset, height - inset, currentAngle, newAngle - currentAngle, true, filler);
                } else {
                    canvas.drawPaint(filler);
                }
                currentAngle = newAngle;
            }

        } else {
            filler.setColor(0xBBBBBBBB);
            filler.setStyle(Paint.Style.FILL);
            canvas.drawPaint(filler);
        }
    }
}
