package com.botherconsulting.geoconnectable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by dalemacdonald on 11/21/17.
 */

public class RadarOverlayView extends View {

    private float hOffset = 600;
    private float vOffset = 600;

    private Paint paint;

    public RadarOverlayView(Context context) {
        super(context);

        // create the Paint and set its color
        paint = new Paint();
        paint.setColor(Color.GRAY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLUE);
        canvas.drawCircle(hOffset, vOffset, 60, paint);
    }

}
