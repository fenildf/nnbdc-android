package com.nb.nnbdc.android;

import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.github.nkzawa.socketio.client.Ack;
import com.google.gson.reflect.TypeToken;
import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.Msg;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import beidanci.vo.UserVo;


public class MainActivity extends MyActivity implements MyApp.SocketStatusListener {

    private RadioGroup bottomMenu;
    private RadioButton btnBdc;
    private RadioButton btnRawWord;

    private RadioButton btnMe;
    private TextView tvMsgCount;

    private RadioButton btnSearch;
    private RadioButton btnGame;

    private int selectedBtnTextColor = -1;

    public int getUnReadMsgCount() {
        int count = 0;
        for (Msg msg : msgs) {
            if (!msg.isHasBeenRead()) {
                count++;
            }
        }
        return count;
    }

    public int getAllMsgCount() {
        return msgs.size();
    }

    /**
     * 把所有消息都置为已读状态
     */
    public void setAllMsgsAsRead() {
        for (Msg msg : msgs) {
            msg.setHasBeenRead(true);
        }
        renderUnViewdMsgCount();
    }

    /**
     * 当点击【学习】按钮时，只有第一次点击才会显示背单词前的页面（这个页面对背单词的流程很重要，因为该页面准备今日的单词），
     * 当然，每次都显示也没有问题，这样做是为了提升用户体验：点击【学习】按钮直接回到先前学习的地方，这对于在学习过程中
     * 查词(会离开背单词页面)的体验很重要
     */
    private boolean hasShownBeforeBdcFrame = false;

    float alphaForDisable = 0.4f;
    float alphaForEnable = 0.6f;

    public List<Msg> getMsgs() {
        return msgs;
    }

    List<Msg> msgs = new ArrayList<>();


    public MeFragment getMeFragment() {
        return meFragment;
    }

    private MeFragment meFragment;

    private RawWordFragment rawWordFragment;

    public RawWordFragment getRawWordFragment() {
        return rawWordFragment;
    }

    public SelectBookFragment getSelectBookFragment() {
        return selectBookFragment;
    }

    private SelectBookFragment selectBookFragment;
    private MsgFragment msgFragment;

    public BeforeBdcFragment getBeforeBdcActivity() {
        return beforeBdcFragment;
    }

    private BeforeBdcFragment beforeBdcFragment;

    public BdcFragment getBdcActivity() {
        return bdcFragment;
    }

    private BdcFragment bdcFragment;

    public FinishFragment getFinishActivity() {
        return finishFragment;
    }

    private FinishFragment finishFragment;

    public StageReviewFragment getStageReviewFragment() {
        return stageReviewFragment;
    }

    private StageReviewFragment stageReviewFragment;

    public SearchFragment getSearchFragment() {
        return searchFragment;
    }

    private SearchFragment searchFragment;

    private GameCenterFragment gameCenterFragment;
    private RussiaFragment russiaFragment;

    private MyFragment currentFragment;

    private ProgressDialog updateProgress;

    public boolean isUserReportedToSocketServer() {
        return isUserReportedToSocketServer;
    }

    private boolean isUserReportedToSocketServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化界面
        initView();

        // 监听socket状态变化
        getAppContext().registerSocketStatusListener(this);
        tryReportUserToSocketServer();

        // 监听某些全局性的socket事件
        getAppContext().registerSocketEventListener(new MyApp.SocketEventListener() {
            @Override
            public void onSocketEvent(final String event, final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (event.equals("inviteYouToGame")) {
                                // 反序列化socket消息
                                JSONArray params = (JSONArray) args[0];
                                JSONObject userObj = (JSONObject) params.get(0);
                                Type objectType = new TypeToken<UserVo>() {
                                }.getType();
                                UserVo sender = Util.getGsonBuilder().create().fromJson(userObj.toString(), objectType);
                                String gameType = (String) params.get(1);
                                int room = (int) params.get(2);
                                String hallName = (String) params.get(3);
                                String content = sender.getDisplayNickName() + "邀请你进行游戏，级别:" + hallName;

                                // 构造消息对象
                                Msg msg = new Msg("inviteYouToGame", content, sender, new Object[]{gameType, room, hallName}, false);
                                msgs.add(msg);

                                // 播放提示音，并渲染未读消息数量小图标
                                Util.playSoundByResId(R.raw.explode, MainActivity.this);
                                renderUnViewdMsgCount();

                                // 通知监听者有新消息
                                for (NewMsgListener listener : newMsgListeners) {
                                    listener.onNewMsg(msg);
                                }
                            }
                        } catch (Exception e) {
                            ToastUtil.showToast(MainActivity.this, "系统发生异常：" + e.getMessage());
                        }
                    }
                });
            }
        });

        selectedBtnTextColor = getResources().getColor(R.color.bottomMenuTextColorSelected);
    }

    @Override
    protected void onDestroy() {
        getAppContext().unregisterSocketStatusListener(this);
        super.onDestroy();
    }

    public void renderUnViewdMsgCount() {
        int unViewedMsgCount = getUnReadMsgCount();
        tvMsgCount.setText(String.valueOf(unViewedMsgCount));
        if (unViewedMsgCount == 0) {
            tvMsgCount.setVisibility(View.GONE);
        } else {
            tvMsgCount.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 向Socket服务器上报用户名(login)
     */
    public void tryReportUserToSocketServer() {
        final MyApp app = getAppContext();
        final UserVo user = app.getLoggedInUser();
        if (user == null) throw new AssertionError();

        if (app.isConnectedToSocketServer() && !isUserReportedToSocketServer) {
            app.getSocket().emit("reportUser", user.getId(), new Ack() {
                @Override
                public void call(Object... args) {
                    isUserReportedToSocketServer = true;
                }
            });
        } else if (!app.isConnectedToSocketServer()) {
            isUserReportedToSocketServer = false;
        }
    }


    public void switchToRawWordFragment(MyFragment from) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (rawWordFragment != null) {
            fTransaction.remove(rawWordFragment);
        }
        rawWordFragment = new RawWordFragment();
        fTransaction.add(R.id.main_content, rawWordFragment);
        fTransaction.commit();
        currentFragment = rawWordFragment;

        setTitle("生词本");
        btnRawWord.setChecked(true);
        btnRawWord.setTextColor(selectedBtnTextColor);
        btnRawWord.setAlpha(alphaForEnable);

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchToMeFragment(MyFragment from) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (meFragment != null) {
            fTransaction.remove(meFragment);
        }
        meFragment = new MeFragment();
        fTransaction.add(R.id.main_content, meFragment);
        fTransaction.commit();
        currentFragment = meFragment;

        setTitle("我的学习进度");
        btnMe.setChecked(true);
        if (selectedBtnTextColor != -1) { // MeFragment是程序启动后第一个页面，所以有可能颜色还没有初始化
            btnMe.setTextColor(selectedBtnTextColor);
        }
        btnMe.setAlpha(alphaForEnable);
        tvMsgCount.setAlpha(alphaForEnable);

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchToSelectBookFragment(MyFragment from) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (selectBookFragment != null) {
            fTransaction.remove(selectBookFragment);
        }
        selectBookFragment = new SelectBookFragment();
        fTransaction.add(R.id.main_content, selectBookFragment);
        fTransaction.commit();
        currentFragment = selectBookFragment;

        setTitle("单词书");

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchToMsgFragment(MyFragment from) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (msgFragment != null) {
            fTransaction.remove(msgFragment);
        }
        msgFragment = new MsgFragment();
        fTransaction.add(R.id.main_content, msgFragment);
        fTransaction.commit();
        currentFragment = msgFragment;

        setTitle("消息");

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchToSearchFragment(MyFragment from) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (searchFragment == null) {
            searchFragment = new SearchFragment();
            fTransaction.add(R.id.main_content, searchFragment);
        } else {
            fTransaction.show(searchFragment);
        }
        fTransaction.commit();
        currentFragment = searchFragment;

        setTitle("查找单词");
        btnSearch.setChecked(true);
        btnSearch.setTextColor(selectedBtnTextColor);
        btnSearch.setAlpha(alphaForEnable);

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchToGameCenterFragment(MyFragment from) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (gameCenterFragment != null) {
            fTransaction.remove(gameCenterFragment);
        }
        gameCenterFragment = new GameCenterFragment();
        fTransaction.add(R.id.main_content, gameCenterFragment);
        fTransaction.commit();
        currentFragment = gameCenterFragment;

        setTitle("游戏大厅");
        btnGame.setChecked(true);
        btnGame.setTextColor(selectedBtnTextColor);
        btnGame.setAlpha(alphaForEnable);

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchToRussiaFragment(MyFragment from, String hallName, Integer exceptRoom) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (russiaFragment != null) {
            fTransaction.remove(russiaFragment);
        }
        russiaFragment = new RussiaFragment(hallName, exceptRoom);
        fTransaction.add(R.id.main_content, russiaFragment);
        fTransaction.commit();
        currentFragment = russiaFragment;

        setTitle("比赛");
        btnGame.setChecked(true);
        btnGame.setTextColor(selectedBtnTextColor);
        btnGame.setAlpha(alphaForEnable);

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchToBeforeBdcFragment(MyFragment from) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (beforeBdcFragment != null) {
            fTransaction.remove(beforeBdcFragment);
        }
        beforeBdcFragment = new BeforeBdcFragment();
        fTransaction.add(R.id.main_content, beforeBdcFragment);
        fTransaction.commit();
        currentFragment = beforeBdcFragment;

        setTitle("今日学习计划");
        btnBdc.setChecked(true);
        btnBdc.setTextColor(selectedBtnTextColor);
        btnBdc.setAlpha(alphaForEnable);

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchBdcFragment(MyFragment from, String fromFragment, boolean recreateFrame) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (bdcFragment != null && recreateFrame) {
            fTransaction.remove(bdcFragment);
            bdcFragment = null;
        }
        if (bdcFragment == null || !bdcFragment.isShowingAWord()) {
            bdcFragment = new BdcFragment();
            fTransaction.add(R.id.main_content, bdcFragment);
            bdcFragment.setFromPage(fromFragment);
        } else {
            fTransaction.show(bdcFragment);
        }
        fTransaction.commit();
        currentFragment = bdcFragment;

        setTitle("背单词");
        btnBdc.setChecked(true);
        btnBdc.setTextColor(selectedBtnTextColor);
        btnBdc.setAlpha(alphaForEnable);

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchToFinishFragment(MyFragment from) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (finishFragment != null) {
            fTransaction.remove(finishFragment);
        }
        finishFragment = new FinishFragment();
        fTransaction.add(R.id.main_content, finishFragment);
        fTransaction.commit();
        currentFragment = finishFragment;

        setTitle("今日学习已完成");
        btnBdc.setChecked(true);
        btnBdc.setTextColor(selectedBtnTextColor);
        btnBdc.setAlpha(alphaForEnable);

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchToStageReviewFragment(MyFragment from) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (stageReviewFragment != null) {
            fTransaction.remove(stageReviewFragment);
        }
        stageReviewFragment = new StageReviewFragment();
        fTransaction.add(R.id.main_content, stageReviewFragment);
        fTransaction.commit();
        currentFragment = stageReviewFragment;

        setTitle("阶段复习");
        btnBdc.setChecked(true);
        btnBdc.setTextColor(selectedBtnTextColor);
        btnBdc.setAlpha(alphaForEnable);

        fireFragmentSwitchEvent(from, currentFragment);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }

    public void initView() {
        bottomMenu = (RadioGroup) findViewById(R.id.tab_menu);
        btnBdc = (RadioButton) bottomMenu.findViewById(R.id.btnBdc);
        btnRawWord = (RadioButton) bottomMenu.findViewById(R.id.btnRawWord);
        btnMe = (RadioButton) bottomMenu.findViewById(R.id.btnMe);
        tvMsgCount = (TextView) bottomMenu.findViewById(R.id.msg_count);
        btnSearch = (RadioButton) bottomMenu.findViewById(R.id.btnSearch);
        btnGame = (RadioButton) bottomMenu.findViewById(R.id.btnGame);

        //学习进度按钮
        btnMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToMeFragment(currentFragment);
            }
        });

        //生词本按钮
        btnRawWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToRawWordFragment(currentFragment);
            }
        });

        //学习按钮
        btnBdc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasShownBeforeBdcFrame || bdcFragment == null || !bdcFragment.isShowingAWord()) {
                    switchToBeforeBdcFragment(currentFragment);
                    hasShownBeforeBdcFrame = true;
                } else {
                    switchBdcFragment(currentFragment, null, false);
                }
            }
        });

        //搜索按钮
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToSearchFragment(currentFragment);
            }
        });

        //游戏按钮
        btnGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToGameCenterFragment(currentFragment);
            }
        });

        //设置默认的tab
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            btnMe.callOnClick();
        } else {
            btnMe.performClick();
        }
    }

    //隐藏所有Fragment
    private void hideAllFragment(FragmentTransaction fTransaction) {
        int default_text_color = getResources().getColor(R.color.bottomMenuTextColorNormal);
        btnBdc.setAlpha(alphaForDisable);
        btnBdc.setTextColor(default_text_color);
        btnRawWord.setAlpha(alphaForDisable);
        btnRawWord.setTextColor(default_text_color);
        btnMe.setAlpha(alphaForDisable);
        btnMe.setTextColor(default_text_color);
        tvMsgCount.setAlpha(alphaForDisable);
        tvMsgCount.setTextColor(default_text_color);
        btnSearch.setAlpha(alphaForDisable);
        btnSearch.setTextColor(default_text_color);
        btnGame.setAlpha(alphaForDisable);
        btnGame.setTextColor(default_text_color);

        if (meFragment != null) {
            fTransaction.hide(meFragment);
        }
        if (selectBookFragment != null) {
            fTransaction.hide(selectBookFragment);
        }
        if (beforeBdcFragment != null) {
            fTransaction.hide(beforeBdcFragment);
        }
        if (bdcFragment != null) {
            fTransaction.hide(bdcFragment);
        }
        if (finishFragment != null) {
            fTransaction.hide(finishFragment);
        }
        if (stageReviewFragment != null) {
            fTransaction.hide(stageReviewFragment);
        }
        if (rawWordFragment != null) {
            fTransaction.hide(rawWordFragment);
        }
        if (searchFragment != null) {
            fTransaction.hide(searchFragment);
        }
        if (gameCenterFragment != null) {
            fTransaction.hide(gameCenterFragment);
        }
        if (russiaFragment != null) {
            fTransaction.hide(russiaFragment);
        }
        if (msgFragment != null) {
            fTransaction.hide(msgFragment);
        }
    }

    private List<FragmentSwitchListener> fragmentSwitchListeners = new LinkedList<>();

    public void registerFragmentSwitchListener(FragmentSwitchListener listener) {
        if (!fragmentSwitchListeners.contains(listener)) {
            fragmentSwitchListeners.add(listener);
        }
    }

    public void unRegisterFragmentSwitchListener(FragmentSwitchListener listener) {
        boolean success = fragmentSwitchListeners.remove(listener);
        Assert.assertTrue(success);
    }

    private void fireFragmentSwitchEvent(MyFragment from, MyFragment to) {
        for (FragmentSwitchListener listener : fragmentSwitchListeners) {
            listener.onFragmentSwitched(from, to);
        }
    }

    private List<NewMsgListener> newMsgListeners = new ArrayList<>();

    public void registerNewMsgListener(NewMsgListener listener) {
        newMsgListeners.add(listener);
    }

    public void unRegisterNewMsgListener(NewMsgListener listener) {
        boolean success = newMsgListeners.remove(listener);
        Assert.assertTrue(success);
    }

    @Override
    public void onConnected() {
        tryReportUserToSocketServer();
    }

    @Override
    public void onDisconnected() {
        tryReportUserToSocketServer();
    }

    public interface NewMsgListener {
        void onNewMsg(Msg msg);
    }
}
