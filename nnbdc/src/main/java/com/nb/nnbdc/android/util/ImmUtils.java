package com.nb.nnbdc.android.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by Administrator on 2016/8/7.
 */
public class ImmUtils {
    public static void openImmWindow(Activity activity, View focusView) {
        if (activity == null || focusView == null) return;
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(focusView, InputMethodManager.SHOW_IMPLICIT);
        }
    }


    public static void closeImmWindow(Activity activity, View focusView) {
        if (activity == null || focusView == null) return;
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(focusView.getWindowToken()
                    , InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }


}
