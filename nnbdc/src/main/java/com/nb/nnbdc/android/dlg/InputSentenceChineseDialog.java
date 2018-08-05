package com.nb.nnbdc.android.dlg;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.MyActivity;
import com.nb.nnbdc.android.util.ImmUtils;
import com.nb.nnbdc.android.util.ToastUtil;

import java.io.IOException;

import beidanci.vo.Result;
import beidanci.vo.SentenceVo;

/**
 * Created by Administrator on 2016/5/15.
 */
public class InputSentenceChineseDialog extends Dialog {
    private MyActivity activity;

    public SentenceVo getSentence() {
        return sentence;
    }

    private SentenceVo sentence;

    public InputSentenceChineseDialog(Context context, int theme, SentenceVo sentence) {
        super(context, theme);
        this.activity = (MyActivity) context;
        this.sentence = sentence;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_chinese_dialog);
        setCanceledOnTouchOutside(false);

        Button okBtn = (Button) findViewById(R.id.okBtn);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String chinese = ((EditText) findViewById(R.id.content)).getText().toString().trim();
                SaveSentenceDiyItemTask task = new SaveSentenceDiyItemTask(chinese);
                task.execute((Void) null);
            }
        });

        Button cancelBtn = (Button) findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        TextView hintView = (TextView) findViewById(R.id.hint);
        hintView.setText(sentence.getEnglish());

        // 显示/隐藏帮助文字
        ImageView btnHelp = (ImageView) findViewById(R.id.btnHelp);
        final TextView txtHelp = (TextView) findViewById(R.id.txtHelp);
        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (txtHelp.getVisibility() == View.VISIBLE) {
                    txtHelp.setVisibility(View.GONE);
                } else {
                    txtHelp.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public class SaveSentenceDiyItemTask extends AsyncTask<Void, Void, Result> {
        private String chinese;

        public SaveSentenceDiyItemTask(String chinese) {
            this.chinese = chinese;
        }

        @Override
        protected Result doInBackground(Void... params) {
            try {
                Result<SentenceVo> result = activity.getHttpClient().saveSentenceDiyItem(sentence.getId(), chinese);
                sentence = activity.getHttpClient().getSentence(sentence.getId());
                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Result result) {
            ImmUtils.closeImmWindow(activity, findViewById(R.id.content));
            if (result == null) {
                ToastUtil.showToast(activity, "系统异常");
            } else if (result.isSuccess()) {
                dismiss();
            } else {
                ToastUtil.showToast(activity, result.getMsg());
            }
        }

    }


}
