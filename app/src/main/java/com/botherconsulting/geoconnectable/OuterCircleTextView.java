package com.botherconsulting.geoconnectable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

/**
 * Created by dalemacdonald on 11/21/17.
 */

public class OuterCircleTextView extends android.support.v7.widget.AppCompatTextView {

    private String MY_TEXT = "xjaphx: Draw Text on Curve";
    private Path mArc;
    private float hOffset;
    private float vOffset;

    private Paint mPaintText;

    public OuterCircleTextView(Context context, AttributeSet attrs) {
        super(context,attrs);
        setPath(20,20, 200, Path.Direction.CW, -180, 200);
        setTextStyle(Paint.ANTI_ALIAS_FLAG, Paint.Style.FILL_AND_STROKE, Color.WHITE,32f);
    }

    public OuterCircleTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context,attrs,defStyleAttr);
        setPath(20,20, 200, Path.Direction.CW, -180, 200);
        setTextStyle(Paint.ANTI_ALIAS_FLAG, Paint.Style.FILL_AND_STROKE, Color.WHITE,20f);
    }

    public OuterCircleTextView(Context context) {
        super(context);
        setPath(20,20, 200, Path.Direction.CW, -180, 200);
        setTextStyle(Paint.ANTI_ALIAS_FLAG, Paint.Style.FILL_AND_STROKE, Color.WHITE,32f);

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
        mPaintText.setShadowLayer(2.0f,2.0f,2.0f,0xff0000);
        //invalidate();
    }

    public void setPath(int _x,
                        int _y,
                        float _radius,
                        android.graphics.Path.Direction _direction,
                        float _hOffset,
                        float _vOffset) {
        mArc = new Path();
        mArc.addCircle(_x,_y, _radius, _direction);
        hOffset = _hOffset;
        vOffset = _vOffset;
        //invalidate();
    }





    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawTextOnPath(MY_TEXT, mArc, hOffset, vOffset, mPaintText);
        //invalidate();
    }
}
