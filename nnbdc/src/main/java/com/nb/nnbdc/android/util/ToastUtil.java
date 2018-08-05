package com.nb.nnbdc.android.util;

import android.content.Context;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nb.nnbdc.R;

/**
 * Created by Administrator on 2016/2/15.
 */
public class ToastUtil {
    private static Toast toast;
    private static CountDownTimer cdt;

    public static void showToast(Context context, String content) {
        if (toast == null) {
            toast = new Toast(context);
            toast.setGravity(Gravity.CENTER, 0, 0);
        }

        View view = View.inflate(context, R.layout.toast, null);
        toast.setView(view);
        TextView tv_content = (TextView) view.findViewById(R.id.content);
        tv_content.setText(content);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
        if (cdt == null) {
            cdt = new CountDownTimer(1500, 1500) {

                @Override
                public void onTick(long arg0) {

                }

                @Override
                public void onFinish() {
                    toast.cancel();
                }
            };
        } else {
            cdt.cancel();
            cdt = new CountDownTimer(1500, 1500) {

                @Override
                public void onTick(long arg0) {

                }

                @Override
                public void onFinish() {
                    toast.cancel();
                }
            };
        }
        cdt.start();
    }
}

