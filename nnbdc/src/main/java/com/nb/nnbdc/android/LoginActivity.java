package com.nb.nnbdc.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.task.GetLoggedInUserTask;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;
import com.nb.nnbdc.android.util.WordStore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import beidanci.vo.UserVo;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends MyActivity {


    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mLoginFormView;

    /**
     * 是否是快速体验
     */
    private boolean isFastTry;

    private ProgressDialog updateProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mLoginFormView = findViewById(R.id.login_form);
        CheckNewVersionTask task = new CheckNewVersionTask(this);
        task.execute((Void) null);

        WordStore.getInstance(this);//开始加载本地词库
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        boolean cancel = false;
        View focusView = null;
        String userName;
        String password;
        if (isFastTry) {
            userName = "guest";
            password = "";
        } else {
            userName = mEmailView.getText().toString();
            password = mPasswordView.getText().toString();

            // Check for a valid password, if the user entered one.
            if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
                mPasswordView.setError(getString(R.string.error_invalid_password));
                focusView = mPasswordView;
                cancel = true;
            }

            // Check for a valid email address.
            if (TextUtils.isEmpty(userName)) {
                mEmailView.setError(getString(R.string.error_field_required));
                focusView = mEmailView;
                cancel = true;
            } else if (!isEmailValid(userName)) {
                mEmailView.setError(getString(R.string.error_invalid_email));
                focusView = mEmailView;
                cancel = true;
            }
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(userName, password, isFastTry ? "USER_NAME" : "EMAIL", this);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    protected void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        final ProgressBar progressBar = getProgressBar();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends MyAsyncTask<Void, Void, String/*错误描述信息*/> {

        private final String loginType;
        private final String userName;
        private final String mPassword;

        UserLoginTask(String email, String password, String loginType, MyActivity myActivity) {
            super(myActivity);
            userName = email;
            mPassword = password;
            this.loginType = loginType;
        }

        public JSONObject login(String serviceBaseUrl, String userName, String password) throws JSONException, IOException {
            Map<String, Object> postParameters = new HashMap<>();
            postParameters.put("loginType", loginType);
            postParameters.put(loginType.equals("EMAIL") ? "email" : "userName", userName);
            postParameters.put("password", password);
            postParameters.put("clientType", "android");
            String serviceUrl = serviceBaseUrl + "/login.do";
            HttpClient httpClient = ((MyApp) getApplicationContext()).getHttpClient();
            String result = httpClient.sendAjax(serviceUrl, postParameters, "POST", 10000);
            JSONObject jo = new JSONObject(result);
            return jo;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                JSONObject jo = login(getString(R.string.service_url), userName, mPassword);
                return jo.getBoolean("success") ? null : jo.getString("msg");
            } catch (Exception e) {
                return "网络错误";
            }
        }

        @Override
        protected void onPostExecute(final String msg) {
            super.onPostExecute(msg);
            mAuthTask = null;
            showProgress(false);

            if (msg == null) { //登录成功
                //在本地保存登录用户的密码
                if (!userName.equals("guest")) { //正式用户（非游客）
                    SharedPreferences settings = getSharedPreferences("loggedInUser", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("userName", userName);
                    editor.putString("password", mPassword);
                    editor.commit();
                }

                getLoggedInUser(); //获取登录用户信息并保存在本地
            } else {//登录失败
                mPasswordView.setError(msg);
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mAuthTask = null;
            showProgress(false);
        }
    }

    private void getLoggedInUser() {
        GetLoggedInUserTask.getLoggedInUser(this, new GetLoggedInUserTask.CallBack() {

            @Override
            public void onSuccess(UserVo user) {
                //跳转到主页面
                getAppContext().setLoggedInUser(user);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailed() {
                showToast("获取用户信息失败");
            }
        });
    }


    /**
     * 检查程序是否存在新版本
     */
    public class CheckNewVersionTask extends MyAsyncTask<Void, Void, Object[]> {
        protected CheckNewVersionTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected Object[] doInBackground(Void... params) {
            try {
                return getHttpClient().getNewVersionCodeAndName();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Object[] codeAndName) {
            super.onPostExecute(codeAndName);
            if (codeAndName == null) {
                ToastUtil.showToast(LoginActivity.this, "网络异常");
                initLoginUI();
                return;
            }

            int vercode = Util.getVerCode(LoginActivity.this); // 获取当前程序版本
            int newVerCode = (Integer) codeAndName[0];
            if (newVerCode > vercode) {
                String newVersionName = (String) codeAndName[1];
                doNewVersionUpdate(newVersionName); // 更新新版本
            } else {
                //当前已是最新版本，正常启动
                initLoginUI();
            }
        }


    }

    private void initLoginUI() {
        //从本地存储读取上次登录的用户名和密码
        String userName = null;
        String password = null;
        try {
            UserVo user = Util.getCachedLoggedInUser(LoginActivity.this);
            if (!user.getUserName().startsWith("guest_")) {
                userName = user.getUserName();
                password = user.getPassword();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText(userName);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.integer.customImeActionId || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        mPasswordView.setText(password);

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button fastTryButton = (Button) findViewById(R.id.fastTry);
        fastTryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                isFastTry = true;
                attemptLogin();
            }
        });

    }

    private void doNewVersionUpdate(String versionName) {
        StringBuffer sb = new StringBuffer();
        sb.append("发现新版本(");
        sb.append(versionName);
        sb.append("), 是否更新?");
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle("软件更新")
                .setMessage(sb.toString())
                .setPositiveButton("更新",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                updateProgress = new ProgressDialog(LoginActivity.this);
                                updateProgress.setTitle("正在下载");
                                updateProgress.setMessage("请稍候...");
                                updateProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                updateProgress.show();
                                DownApkTask task = new DownApkTask(LoginActivity.this);
                                task.execute((Void) null);
                            }
                        })
                .setNegativeButton("暂不更新",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                initLoginUI();
                            }
                        }).create();// 创建
        // 显示对话框
        dialog.show();
    }


    /**
     * 下载新版本的apk
     */
    public class DownApkTask extends MyAsyncTask<Void, Void, String> {
        protected DownApkTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                String localApkFile = Environment.getExternalStorageDirectory() + "/nnbdc.apk";
                boolean succ = Util.downloadFile(getString(R.string.apkUrl), localApkFile, true);
                return succ ? localApkFile : null;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(final String localApkFile) {
            super.onPostExecute(localApkFile);
            updateProgress.cancel();
            if (localApkFile == null) {
                //下载失败，使用当前版本继续
                ToastUtil.showToast(LoginActivity.this, "下载新版本失败,仍然使用旧版本");
                initLoginUI();
            } else {
                //下载成功，使用新版本启动
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(localApkFile)),
                        "application/vnd.android.package-archive");
                LoginActivity.this.startActivity(intent);
                finish();
            }
        }

    }
}

