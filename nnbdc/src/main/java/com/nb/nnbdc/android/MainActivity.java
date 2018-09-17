package com.nb.nnbdc.android;

import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.github.nkzawa.socketio.client.Ack;
import com.nb.nnbdc.R;

import java.util.LinkedList;
import java.util.List;

import beidanci.vo.UserVo;


public class MainActivity extends MyActivity {

    private RadioGroup bottomMenu;
    private RadioButton btnBdc;
    private RadioButton btnRawWord;
    private RadioButton btnMe;
    private RadioButton btnSearch;
    private RadioButton btnGame;

    /**
     * 当点击【学习】按钮时，只有第一次点击才会显示背单词前的页面（这个页面对背单词的流程很重要，因为该页面准备今日的单词），
     * 当然，每次都显示也没有问题，这样做是为了提升用户体验：点击【学习】按钮直接回到先前学习的地方，这对于在学习过程中
     * 查词(会离开背单词页面)的体验很重要
     */
    private boolean hasShownBeforeBdcFrame = false;

    float alphaForDisable = 0.4f;
    float alphaForEnable = 0.6f;


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

        //初始化界面
        initView();

        getAppContext().registerSocketStatusListener(new MyApp.SocketStatusListener() {
            @Override
            public void onConnected() {
                tryReportUserToSocketServer();
            }

            @Override
            public void onDisconnected() {
                tryReportUserToSocketServer();
            }
        });
        tryReportUserToSocketServer();
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
        fTransaction.commitAllowingStateLoss();
        currentFragment = rawWordFragment;

        setTitle("生词本");
        btnRawWord.setChecked(true);
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
        fTransaction.commitAllowingStateLoss();
        currentFragment = meFragment;

        setTitle("我的学习进度");
        btnMe.setChecked(true);
        btnMe.setAlpha(alphaForEnable);

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
        fTransaction.commitAllowingStateLoss();
        currentFragment = selectBookFragment;

        setTitle("单词书");

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
        fTransaction.commitAllowingStateLoss();
        currentFragment = searchFragment;

        setTitle("查找单词");
        btnSearch.setChecked(true);
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
        fTransaction.commitAllowingStateLoss();
        currentFragment = gameCenterFragment;

        setTitle("游戏大厅");
        btnGame.setChecked(true);
        btnGame.setAlpha(alphaForEnable);

        fireFragmentSwitchEvent(from, currentFragment);
    }

    public void switchToRussiaFragment(MyFragment from, String hallName, String exceptRoom) {
        bottomMenu.setVisibility(View.VISIBLE);
        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        hideAllFragment(fTransaction);
        if (russiaFragment != null) {
            fTransaction.remove(russiaFragment);
        }
        russiaFragment = new RussiaFragment(hallName, exceptRoom);
        fTransaction.add(R.id.main_content, russiaFragment);
        fTransaction.commitAllowingStateLoss();
        currentFragment = russiaFragment;

        setTitle("游戏");
        btnGame.setChecked(true);
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
        fTransaction.commitAllowingStateLoss();
        currentFragment = beforeBdcFragment;

        setTitle("今日学习计划");
        btnBdc.setChecked(true);
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
        fTransaction.commitAllowingStateLoss();
        currentFragment = bdcFragment;

        setTitle("背单词");
        btnBdc.setChecked(true);
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
        fTransaction.commitAllowingStateLoss();
        currentFragment = finishFragment;

        setTitle("今日学习已完成");
        btnBdc.setChecked(true);
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
        fTransaction.commitAllowingStateLoss();
        currentFragment = stageReviewFragment;

        setTitle("阶段复习");
        btnBdc.setChecked(true);
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
        btnBdc.setAlpha(alphaForDisable);
        btnRawWord.setAlpha(alphaForDisable);
        btnMe.setAlpha(alphaForDisable);
        btnSearch.setAlpha(alphaForDisable);
        btnGame.setAlpha(alphaForDisable);

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
    }

    private List<FragmentSwitchListener> fragmentSwitchListeners = new LinkedList<>();

    public void registerFragmentSwitchListener(FragmentSwitchListener listener) {
        if (!fragmentSwitchListeners.contains(listener)) {
            fragmentSwitchListeners.add(listener);
        }
    }

    public void unRegisterFragmentSwitchListener(FragmentSwitchListener listener) {
        fragmentSwitchListeners.remove(listener);
    }

    private void fireFragmentSwitchEvent(MyFragment from, MyFragment to) {
        for (FragmentSwitchListener listener : fragmentSwitchListeners) {
            listener.onFragmentSwitched(from, to);
        }
    }
}
