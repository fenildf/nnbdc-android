package com.nb.nnbdc.android.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.nb.nnbdc.R;

public class MyProgress extends ProgressBar {
    String text;
    Paint mPaint;

    public void setShowProgressValue(boolean showProgressValue) {
        this.showProgressValue = showProgressValue;
    }

    boolean showProgressValue = true;

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    int precision = 2;

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
        initText();
    }

    private int textSize = 64;

    public MyProgress(Context context) {
        super(context);
        initText();
    }

    public MyProgress(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initText();
    }


    public MyProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        initText();
    }

    @Override
    public synchronized void setProgress(int progress) {
        setText(progress);
        super.setProgress(progress);

    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //this.setText();
        Rect rect = new Rect();
        this.mPaint.getTextBounds(this.text, 0, this.text.length(), rect);
        int x = (getWidth() / 2) - rect.centerX();
        int y = (getHeight() / 2) - rect.centerY();
        canvas.drawText(this.text, x, y, this.mPaint);
    }

    //初始化，画笔
    private void initText() {
        this.mPaint = new Paint();
        this.mPaint.setColor(ContextCompat.getColor(getContext(), R.color.defaultTextColor));
        this.mPaint.setTextSize(textSize);
    }


    //设置文字内容
    private void setText(int progress) {
        double i = (progress * 100 + 0.0) / getMax();
        this.text = showProgressValue ? String.format("%d/%d (%." + precision + "f%%)", progress, getMax(), i) : String.format("%." + precision + "f%%", i);

    }


}
