package com.nb.nnbdc.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;

/**
 * Created by Administrator on 2015/11/29.
 */
public class MyActivity extends AppCompatActivity {
    protected Util.HintHandler hintHandler;
    protected Util.ReLoginHandler reLoginHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hintHandler = new Util.HintHandler(this);
        ((MyApp) getApplication()).setHintHandler(hintHandler);
        reLoginHandler = new Util.ReLoginHandler(this);
        ((MyApp) getApplication()).setReLoginHandler(reLoginHandler);
    }

    protected ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.loading_progress);
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
        }
    }

    public void onReLoginRequired() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public HttpClient getHttpClient() {
        return ((MyApp) getApplicationContext()).getHttpClient();
    }

    protected MyApp getAppContext() {
        return (MyApp) getApplicationContext();
    }

    public void showToast(String msg){
        ToastUtil.showToast(this, msg);
    }

}
