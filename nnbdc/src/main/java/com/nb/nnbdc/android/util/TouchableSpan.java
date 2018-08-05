package com.nb.nnbdc.android.util;

import android.text.TextPaint;
import android.text.style.ClickableSpan;

/**
 * Created by Administrator on 2016/5/15.
 */
public abstract class TouchableSpan extends ClickableSpan {
    private boolean mIsPressed;
    private int mPressedBackgroundColor;
    private int mNormalTextColor;
    private int mPressedTextColor;

    public TouchableSpan(int normalTextColor, int pressedTextColor, int pressedBackgroundColor) {
        mNormalTextColor = normalTextColor;
        mPressedTextColor = pressedTextColor;
        mPressedBackgroundColor = pressedBackgroundColor;
    }

    public void setPressed(boolean isSelected) {
        mIsPressed = isSelected;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setColor(mIsPressed ? mPressedTextColor : mNormalTextColor);
        if (mIsPressed) {
            ds.bgColor = mPressedBackgroundColor;
        }
        ds.setUnderlineText(false);
    }
}
