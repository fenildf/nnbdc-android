package com.nb.nnbdc.android.task;

import android.os.AsyncTask;

import com.nb.nnbdc.android.MyActivity;
import com.nb.nnbdc.android.util.ToastUtil;

import java.io.IOException;

import beidanci.vo.Result;

/**
 * Created by Administrator on 2016/8/8.
 */
public class AddRawWordTask extends AsyncTask<Void, Void, Result> {
    String wordWordSpell;
    MyActivity activity;

    public AddRawWordTask(String spell, MyActivity activity) {
        this.wordWordSpell = spell;
        this.activity=activity;
    }

    @Override
    protected Result doInBackground(Void... params) {
        try {
            Result result = activity.getHttpClient().addRawWord(wordWordSpell);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Result result) {
        if (result == null) {
            ToastUtil.showToast(activity, "系统异常");
        } else if (result.isSuccess()) {
            ToastUtil.showToast(activity, String.format("[%s]已成功添加到生词本", wordWordSpell));
        } else {
            ToastUtil.showToast(activity, result.getMsg());
        }
    }

}
