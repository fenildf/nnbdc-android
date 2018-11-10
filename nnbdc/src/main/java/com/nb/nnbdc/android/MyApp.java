package com.nb.nnbdc.android;

import android.app.Application;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.nb.nnbdc.R;
import com.nb.nnbdc.android.task.GetLoggedInUserTask;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import beidanci.vo.UserVo;

/**
 * Created by Administrator on 2015/11/29.
 */
public class MyApp extends Application {
    public Socket getSocket() {
        return socket;
    }

    private Socket socket;

    private List<SocketEventListener> socketEventListeners = new ArrayList<>();

    public UserVo getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(UserVo loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    private UserVo loggedInUser;

    public boolean isConnectedToSocketServer() {
        return isConnectedToSocketServer;
    }

    public void registerSocketEventListener(SocketEventListener listener) {
        socketEventListeners.add(listener);
    }

    public void setConnectedToSocketServer(boolean connectedToSocketServer) {
        isConnectedToSocketServer = connectedToSocketServer;
    }

    private boolean isConnectedToSocketServer;

    public interface SocketStatusListener {
        void onConnected();

        void onDisconnected();
    }

    private List<SocketStatusListener> socketStatusListeners = new ArrayList<>();

    public void registerSocketStatusListener(SocketStatusListener listener) {
        socketStatusListeners.add(listener);
    }

    private void initSocket() throws URISyntaxException {
        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        opts.reconnection = true;
        socket = IO.socket(getString(R.string.socketServerUrl), opts);

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                isConnectedToSocketServer = true;
                for (SocketStatusListener listener : socketStatusListeners) {
                    listener.onConnected();
                }
            }
        }).on("inviteYouToGame", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                for (SocketEventListener listener : socketEventListeners) {
                    listener.onSocketEvent("inviteYouToGame", args);
                }
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                isConnectedToSocketServer = false;
                for (SocketStatusListener listener : socketStatusListeners) {
                    listener.onDisconnected();
                }
            }
        });
        socket.connect();

        timer.schedule(new HeartBeatTask(), 1000, 5000);
    }

    Timer timer = new Timer();

    private class HeartBeatTask extends TimerTask {
        @Override
        public void run() {
            socket.emit("heartBeat", "");
        }
    }

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

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            initSocket();
        } catch (URISyntaxException e) {
            ToastUtil.showToast(this, "连接消费服务异常");
        }
    }

    public interface SocketEventListener {
        void onSocketEvent(final String event, final Object... args);
    }
}
