package com.botherconsulting.geoconnectable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.view.View;

/**
 * Created by dalemacdonald on 11/21/17.
 */

public class OuterCircleTextView extends View {

        private String MY_TEXT = "xjaphx: Draw Text on Curve";
        private Path mArc;

        private Paint mPaintText;

        public OuterCircleTextView(Context context) {
            super(context);
            setPath(20,20, 200, 200, -180, 200);
            setTextStyle(Paint.ANTI_ALIAS_FLAG, Paint.Style.FILL_AND_STROKE, Color.WHITE,20f);

        }

        public OuterCircleTextView(Context context, String newText) {
            super(context);
            setText(newText);
        }

        public void setText(String newText) {
            MY_TEXT = newText;
            invalidate();
        }

        public void setTextStyle (int flags, Paint.Style style, int color, float size) {
            mPaintText = new Paint(flags);
            mPaintText.setStyle(style);
            mPaintText.setColor(color);
            mPaintText.setTextSize(size);
            invalidate();
        }

        public void setPath(int left,
                            int right,
                            int top,
                            int bottom,
                            int startAngle,
                            int sweepAngle) {
            mArc = new Path();
            RectF oval = new RectF(left,top,right,bottom);
            mArc.addArc(oval, startAngle, sweepAngle);

        }

        public void setPath(RectF newOval,
                            int startAngle,
                            int sweepAngle) {
            mArc = new Path();
            RectF oval = newOval;
            mArc.addArc(oval, startAngle, sweepAngle);

        }




        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawTextOnPath(MY_TEXT, mArc, 0, 20, mPaintText);
            invalidate();
        }
    }
