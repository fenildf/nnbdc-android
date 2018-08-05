package com.nb.nnbdc.android;

import android.os.AsyncTask;

import com.nb.nnbdc.android.MyActivity;
import com.nb.nnbdc.android.util.ToastUtil;

/**
 * Created by Administrator on 2016/8/2.
 */
public abstract class MyAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    protected MyActivity myActivity;

    protected MyAsyncTask(MyActivity myActivity) {
        super();
        this.myActivity = myActivity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        myActivity.showProgress(true);
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        myActivity.showProgress(false);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        myActivity.showProgress(false);
    }

    @Override
    protected void onCancelled(Result result) {
        super.onCancelled(result);
        myActivity.showProgress(false);
    }

    protected HttpClient getHttpClient() {
        return myActivity.getHttpClient();
    }

    protected void showToast(String msg) {
        ToastUtil.showToast(myActivity, "获取用户信息失败");
    }
}
