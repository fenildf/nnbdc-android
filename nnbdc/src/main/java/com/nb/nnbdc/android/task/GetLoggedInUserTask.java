package com.nb.nnbdc.android.task;

import com.nb.nnbdc.android.MyActivity;
import com.nb.nnbdc.android.MyAsyncTask;
import com.nb.nnbdc.android.util.Util;

import beidanci.vo.UserVo;

/**
 * 从服务端获取当前登录用户信息，并更新本地缓存。
 */
public class GetLoggedInUserTask extends MyAsyncTask<Void, Void, UserVo /*登录用户信息*/> {
    private CallBack callBack;

    public GetLoggedInUserTask(MyActivity myActivity, CallBack callBack) {
        super(myActivity);
        this.callBack = callBack;
    }

    @Override
    protected UserVo doInBackground(Void... params) {
        try {
            return getHttpClient().getLoggedInUser();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(final UserVo result) {
        super.onPostExecute(result);

        if (result == null) {
            callBack.onFailed();
        } else {
            //保存登录用户信息
            Util.saveLoggedInUserToCache(result, myActivity);

            callBack.onSuccess(result);
        }
    }

    public interface CallBack {
        void onSuccess(UserVo user);

        void onFailed();
    }

    public static void getLoggedInUser(MyActivity activity, CallBack callBack) {
        GetLoggedInUserTask task = new GetLoggedInUserTask(activity, callBack);
        task.execute((Void) null);
    }
}

