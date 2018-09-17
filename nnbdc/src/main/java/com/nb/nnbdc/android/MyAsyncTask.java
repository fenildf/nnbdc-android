package com.nb.nnbdc.android;

import android.os.AsyncTask;
import android.os.Looper;

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

    private boolean isInMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (isInMainThread()) {
            myActivity.showProgress(true);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        if (isInMainThread()) {
            myActivity.showProgress(false);
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (isInMainThread()) {
            myActivity.showProgress(false);
        }
    }

    @Override
    protected void onCancelled(Result result) {
        super.onCancelled(result);
        if (isInMainThread()) {
            myActivity.showProgress(false);
        }
    }

    protected HttpClient getHttpClient() {
        return myActivity.getHttpClient();
    }

    protected void showToast(String msg) {
        ToastUtil.showToast(myActivity, "获取用户信息失败");
    }
}
