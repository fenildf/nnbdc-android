package com.nb.nnbdc.android.dlg;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.MyActivity;
import com.nb.nnbdc.android.util.ImmUtils;
import com.nb.nnbdc.android.util.StringUtils;
import com.nb.nnbdc.android.util.ToastUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import beidanci.vo.Result;
import beidanci.vo.SentenceVo;
import beidanci.vo.UserVo;

/**
 * 邀请空闲用户进行比赛的对话框
 */
public class InviteDialog extends Dialog {
    private MyActivity activity;
    private List<UserVo> idleUsers;
    private List<InviteUserListener> inviteUserListeners = new ArrayList<>();

    public InviteDialog(Context context, int theme, List<UserVo> idleUsers) {
        super(context, theme);
        this.activity = (MyActivity) context;
        this.idleUsers = idleUsers;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invite_dialog);
        setCanceledOnTouchOutside(false);

        for (final UserVo idleUser : idleUsers) {
            View idleUserLayout = getLayoutInflater().inflate(R.layout.idle_user, null);
            final TextView nicknameView = ((TextView) idleUserLayout.findViewWithTag("nickname"));
            nicknameView.setText(idleUser.getDisplayNickName());
            final Button inviteBtn = ((Button) idleUserLayout.findViewWithTag("btnInvite"));
            inviteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (InviteUserListener inviteUserListener : inviteUserListeners) {
                        inviteUserListener.invite(idleUser);
                    }
                    nicknameView.setText(nicknameView.getText() + " (已邀请)");
                    inviteBtn.setVisibility(View.GONE);
                }
            });
            ((ViewGroup) findViewById(R.id.idleUsers)).addView(idleUserLayout);
        }

        Button cancelBtn = (Button) findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    public void registerInviteUserListeners(InviteUserListener inviteUserListener) {
        inviteUserListeners.add(inviteUserListener);
    }

    public interface InviteUserListener {
        void invite(UserVo userToInvite);
    }
}
