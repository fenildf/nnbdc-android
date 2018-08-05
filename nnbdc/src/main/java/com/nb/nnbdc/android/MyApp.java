package com.nb.nnbdc.android;

import android.app.Application;

import com.nb.nnbdc.android.util.Util;

/**
 * Created by Administrator on 2015/11/29.
 */
public class MyApp extends Application {
    public Util.HintHandler getHintHandler() {
        return hintHandler;
    }

    public void setHintHandler(Util.HintHandler hintHandler) {
        this.hintHandler = hintHandler;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    protected Util.HintHandler hintHandler;

    public Util.ReLoginHandler getReLoginHandler() {
        return reLoginHandler;
    }

    public void setReLoginHandler(Util.ReLoginHandler reLoginHandler) {
        this.reLoginHandler = reLoginHandler;
    }

    protected Util.ReLoginHandler reLoginHandler;

    public HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new HttpClient(this);
        }
        return httpClient;
    }

    private HttpClient httpClient;


}
