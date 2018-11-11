package com.nb.nnbdc.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.Msg;

import java.text.SimpleDateFormat;

public class MsgFragment extends MyFragment implements MainActivity.NewMsgListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_msg, container, false);
    }

    private void addAMsgToMsgsLayout(final Msg msg, ViewGroup msgsLayout) {
        ViewGroup msgLayout = (ViewGroup) getMainActivity().getLayoutInflater().inflate(R.layout.msg, null);
        TextView tvContent = (TextView) msgLayout.findViewWithTag("content");
        tvContent.setText(String.format("%s(%s)", msg.getContent(), new SimpleDateFormat("HH:mm").format(msg.getReceiveTime())));
        if (msg.isHasBeenRead()) {
            tvContent.setTextColor(getResources().getColor(R.color.msg_text_read));
        } else {
            tvContent.setTextColor(getResources().getColor(R.color.msg_text_unread));
        }

        Button btnOper = (Button) msgLayout.findViewWithTag("oper");
        btnOper.setText("接受");
        btnOper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMainActivity().switchToRussiaFragment(MsgFragment.this, (String) msg.getArgs()[2], null);
            }
        });

        msgsLayout.addView(msgLayout, 0); // 新消息排在前面
    }

    private ViewGroup msgsLayout;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 【返回】按钮
        Button btnCancel = (Button) getView().findViewById(R.id.cancelBtn);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMainActivity().switchToMeFragment(MsgFragment.this);
            }
        });

        // 渲染消息列表
        msgsLayout = (ViewGroup) getView().findViewById(R.id.msgs);
        for (Msg msg : getMainActivity().getMsgs()) {
            addAMsgToMsgsLayout(msg, msgsLayout);
        }

        // 所有消息均置为已读（只要打开了本界面就认为所有消息军被读过了）
        getMainActivity().setAllMsgsAsRead();

        // 监听新消息
        getMainActivity().registerNewMsgListener(this);
    }

    @Override
    public void onFragmentSwitched(MyFragment from, MyFragment to) {
        if (from == this) { // 离开本界面
            getMainActivity().unRegisterNewMsgListener(this);
        }
    }

    @Override
    public void onNewMsg(Msg msg) {
        addAMsgToMsgsLayout(msg, msgsLayout);

        // 所有消息均置为已读（只要打开了本界面就认为所有消息军被读过了）
        getMainActivity().setAllMsgsAsRead();
    }
}
