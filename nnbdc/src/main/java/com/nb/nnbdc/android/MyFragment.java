package com.nb.nnbdc.android;


import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.Util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Administrator on 2016/2/11.
 */
public abstract class MyFragment extends Fragment implements FragmentSwitchListener {
    public HttpClient getHttpClient() {
        return ((MyApp) getActivity().getApplicationContext()).getHttpClient();
    }

    public MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    protected void showProgress(final boolean show) {
        ((MyActivity) getActivity()).showProgress(show);
    }

    public void showHint(String hint) {
        Util.showHintMsg(hint, ((MyActivity) getActivity()).hintHandler);
    }

    protected void showConfirmDlg(String title, String msg, DialogInterface.OnClickListener okClickedListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(msg);
        builder.setTitle(title);
        builder.setPositiveButton("确认", okClickedListener);
        builder.setNegativeButton("取消", null);
        builder.create().show();
    }

    protected void showInputDlg(String title, String inputType, final InputDlgListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        final EditText textView = new EditText(getActivity());
        if (inputType.equals("text")) {
        } else if (inputType.equals("number")) {
            int type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL;
            textView.setInputType(type);
        }
        builder.setView(textView);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onInput(textView.getText().toString());
            }
        });
        builder.setNegativeButton("取消", null);
        builder.create().show();
    }

    private int m_year;
    private int m_month;
    private int m_dayOfMonth;
    private int m_hour;
    private int m_minute;

    protected void showDateTimePickerDlg(final EditText target) {
        ViewGroup pickerLayout = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.date_time_picker, null);

        //日期控件
        final DatePicker datePicker = (DatePicker) pickerLayout.findViewById(R.id.datePicker);
        final TimePicker timePicker = (TimePicker) pickerLayout.findViewById(R.id.timerPicker);
        Date now = new Date();
        m_year = now.getYear() + 1900;
        m_month = now.getMonth();
        m_dayOfMonth = now.getDate();
        m_hour = now.getHours();
        m_minute = now.getMinutes();
        datePicker.init(m_year, m_month, m_dayOfMonth, new DatePicker.OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker view, int year,
                                      int monthOfYear, int dayOfMonth) {
                m_year = year;
                m_month = monthOfYear;
                m_dayOfMonth = dayOfMonth;
            }
        });

        timePicker.setIs24HourView(true);
        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay,
                                      int minute) {
                m_hour = hourOfDay;
                m_minute = minute;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(pickerLayout);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String time;
                Calendar calendar = Calendar.getInstance();
                calendar.set(m_year, m_month, m_dayOfMonth, m_hour, m_minute);
                SimpleDateFormat format = new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm");
                time = format.format(calendar.getTime());
                target.setText(time);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.create().show();
    }

    protected interface InputDlgListener {
        void onInput(String content);
    }

    /**
     * 保持到main activity的引用，防止main activity被自动回收
     */
    private Activity mainActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mainActivity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMainActivity().registerFragmentSwitchListener(this);
    }

    @Override
    public void onDestroy() {
        getMainActivity().unRegisterFragmentSwitchListener(this);
        super.onDestroy();
    }
}
