package com.nb.nnbdc.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.Msg;

import java.util.List;

public class MsgFragment extends MyFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_msg, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 渲染消息列表
        ViewGroup msgsLayout = (ViewGroup) getView().findViewById(R.id.msgs);
        for (Msg msg : getMainActivity().getMsgs()) {
            ViewGroup msgLayout = (ViewGroup) getMainActivity().getLayoutInflater().inflate(R.layout.msg, null);
            TextView tvContent = (TextView) msgLayout.findViewWithTag("content");
            tvContent.setText(msg.getContent());
            Button btnOper = (Button) msgLayout.findViewWithTag("oper");
            btnOper.setText("接受");
            msgsLayout.addView(msgLayout);
        }
    }

    @Override
    public void onFragmentSwitched(MyFragment from, MyFragment to) {

    }
}
