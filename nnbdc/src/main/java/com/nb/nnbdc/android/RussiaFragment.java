package com.nb.nnbdc.android;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.nb.nnbdc.R;
import com.nb.nnbdc.android.dlg.InviteDialog;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import beidanci.vo.UserGameInfo;
import beidanci.vo.UserVo;
import beidanci.vo.WordVo;

import static android.util.TypedValue.COMPLEX_UNIT_SP;


public class RussiaFragment extends MyFragment implements MyApp.SocketStatusListener {
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private Socket socket;
    private String hallName;
    private Integer exceptRoom;

    UserVo loggedInUser;

    /**
     * 点击时不允许显示详情的单词（避免泄露答案）
     */
    String forbiddenWordForDetail;

    boolean forbiddenPopupDetailForAllWords = false;

    public RussiaFragment() {
    }

    public RussiaFragment(String hallName, Integer exceptRoom) {
        this.hallName = hallName;
        this.exceptRoom = exceptRoom;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_russia, container, false);
        Assert.assertTrue(view != null);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getMainActivity().getAppContext().registerSocketStatusListener(this);

        initGame();
    }

    private void initSocket() {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(final MainActivity activity) {
                socket = activity.getAppContext().getSocket();
                socket.off("sysCmd");
                socket.on("sysCmd", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String cmd = (String) args[0];
                                if (cmd.equals("BEGIN_EXERCISE")) {
                                    isExercise = true;
                                    startGame();
                                    appendMsg(0, "牛牛", "练习开始");
                                } else if (cmd.equals("BEGIN")) {
                                    isExercise = false;
                                    startGame();
                                    appendMsg(0, "牛牛", "比赛开始");
                                } else {
                                    Log.e("", "不支持的命令： " + cmd);
                                }
                            }
                        });
                    }
                });
                socket.off("idleUsers");
                socket.on("idleUsers", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    idleUsers.clear();
                                    JSONArray userObjs = (JSONArray) args[0];
                                    for (int i = 0; i < userObjs.length(); i++) {
                                        JSONObject userObj = (JSONObject) userObjs.get(i);
                                        Type objectType = new TypeToken<UserVo>() {
                                        }.getType();
                                        UserVo userVo = Util.getGsonBuilder().create().fromJson(userObj.toString(), objectType);
                                        idleUsers.add(userVo);
                                    }

                                    InviteDialog inviteDialog = new InviteDialog(activity, R.style.dialog, idleUsers);
                                    inviteDialog.registerInviteUserListeners(new InviteDialog.InviteUserListener() {
                                        @Override
                                        public void invite(UserVo userToInvite) {
                                            sendUserCmd("inviteUser", new Object[]{userToInvite.getId(), "russia", roomId, hallName});
                                        }
                                    });
                                    inviteDialog.show();
                                } catch (JSONException e) {
                                    Log.e("", e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                socket.off("userStarted");
                socket.on("userStarted", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int userId = (int) args[0];
                                Player player;
                                if (userId == playerA.userId) {
                                    player = playerA;
                                } else {
                                    player = playerB;
                                }

                                player.started = true;
                                renderUserInfo(player);
                            }
                        });
                    }
                });
                socket.off("giveProps");
                socket.on("giveProps", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isExercise) {
                                    try {
                                        JSONArray params = (JSONArray) args[0];
                                        int propsIndex = (Integer) params.get(0);
                                        int propsCount = (int) params.get(1);
                                        playerA.props[propsIndex] = propsCount;

                                        // 播放提示声音
                                        getAvailableActivity(new IActivityEnabledListener() {
                                            @Override
                                            public void onActivityEnabled(MainActivity activity) {
                                                Util.playSoundByResId(R.raw.explode, activity);
                                            }
                                        });

                                        renderProps();
                                    } catch (JSONException e) {
                                        Log.e("", e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                });
                socket.off("noEnoughCowDung");
                socket.on("noEnoughCowDung", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final int minCount = (int) args[0];
                                getAvailableActivity(new IActivityEnabledListener() {
                                    @Override
                                    public void onActivityEnabled(MainActivity activity) {
                                        ToastUtil.showToast(activity, "开始游戏需要至少" + minCount + "个牛粪");
                                    }
                                });
                            }
                        });
                    }
                });
                socket.off("enterRoom");
                socket.on("enterRoom", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray params = (JSONArray) args[0];
                                    int userId = (int) params.get(0);
                                    String nickName = (String) params.get(1);
                                    Player player = userId == loggedInUser.getId() ? playerA : playerB;
                                    player.userId = userId;
                                    player.nickName = nickName;

                                    //播放开门声
                                    playSound(R.raw.enterroom);

                                    appendMsg(0, "牛牛", nickName + "进来了");
                                } catch (JSONException e) {
                                    Log.e("", e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                socket.off("leaveRoom");
                socket.on("leaveRoom", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray params = (JSONArray) args[0];
                                    int userId = (int) params.get(0);
                                    String nickName = (String) params.get(1);
                                    Player player = userId == loggedInUser.getId() ? playerA : playerB;
                                    player.userId = null;
                                    player.gameInfo = null;

                                    //播放离开房间的声音
                                    playSound(R.raw.leaveroom);

                                    renderUserInfo(player);

                                } catch (JSONException e) {
                                    Log.e("", e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                socket.off("propsUsed");
                socket.on("propsUsed", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray params = (JSONArray) args[0];
                                    int userId = (int) params.get(0);
                                    int propsIndex = (int) params.get(1);
                                    int currNumber = (int) params.get(2);
                                    String nickName = (String) params.get(3);
                                    appendMsg(0, "牛牛", nickName + "使用了道具");

                                    // 己方使用了道具
                                    if (userId == loggedInUser.getId()) {
                                        playerA.props[propsIndex] = currNumber;

                                        if (propsIndex == 0) { // 【加一行】
                                            liftDeadWords(playerB, playerB.wordDivHeight);
                                        } else if (propsIndex == 1) { // 【减一行】
                                            liftDeadWords(playerA, (-1) * playerA.wordDivHeight);
                                        }
                                    } else { // 对方使用了道具
                                        if (propsIndex == 0) { // 【加一行】
                                            liftDeadWords(playerA, playerA.wordDivHeight);
                                        } else if (propsIndex == 1) { // 【减一行】
                                            liftDeadWords(playerB, (-1) * playerB.wordDivHeight);
                                        }
                                    }

                                    renderProps();
                                } catch (JSONException e) {
                                    Log.e("", e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                socket.off("userGameInfo");
                socket.on("userGameInfo", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                JSONObject wordObj = (JSONObject) args[0];
                                Type objectType = new TypeToken<UserGameInfo>() {
                                }.getType();
                                UserGameInfo userGameInfo = Util.getGsonBuilder().create().fromJson(wordObj.toString(), objectType);

                                Player player;
                                if (userGameInfo.getUserId().equals(loggedInUser.getId())) {
                                    player = playerA;
                                } else {
                                    player = playerB;
                                }
                                player.gameInfo = userGameInfo;

                                renderUserInfo(player);
                            }
                        });
                    }
                });
                socket.off("enterWait");
                socket.on("enterWait", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                gameState = "waiting";
                            }
                        });
                    }
                });
                socket.off("enterReady");
                socket.on("enterReady", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                gameState = "ready";
                            }
                        });
                    }
                });
                socket.off("scoreAdjust");
                socket.on("scoreAdjust", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray params = (JSONArray) args[0];
                                    playerA.scoreAdjust = (int) params.get(0);
                                    playerA.cowdungAdjust = (int) params.get(1);
                                    renderScoreAdjust(playerA, false);
                                } catch (JSONException e) {
                                    Log.e("", e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                socket.off("roomId");
                socket.on("roomId", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                roomId = (int) args[0];
                                renderRoomId();
                            }
                        });
                    }
                });
                socket.off("wordA");
                socket.on("wordA", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isPlaying) {
                                    return;
                                }
                                try {
                                    JSONArray params = (JSONArray) args[0];
                                    JSONObject wordObj = (JSONObject) params.get(0);
                                    Type objectType = new TypeToken<WordVo>() {
                                    }.getType();
                                    playerA.currWord = Util.getGsonBuilder().create().fromJson(wordObj.toString(), objectType);
                                    JSONArray meanings = (JSONArray) params.get(1);
                                    playerA.otherWordMeanings[0] = meanings.get(0).toString();
                                    playerA.otherWordMeanings[1] = meanings.get(1).toString();


                                    // 为正确答案随机选择一个索引号（1～3）
                                    int correctIndex = (int) Math.ceil((3.0 * Math.random()));
                                    if (correctIndex == 0) {
                                        correctIndex = 1;
                                    }
                                    if (correctIndex == 4) {
                                        correctIndex = 3;
                                    }
                                    playerA.correctIndex = correctIndex;

                                    // 禁止显示当前下落单词的详情
                                    forbiddenWordForDetail = playerA.currWord.getSpell();

                                    // 更新界面上的单词显示
                                    renderPlayerAWord();
                                    updateUI();

                                    //下载单词发音并自动发音
                                    Util.downloadPronounceAndPlay(playerA.currWord.getSpell(), null, mediaPlayer, getString(R.string.sound_base_url));

                                } catch (JSONException e) {
                                    Log.e("", e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                socket.off("wordB");
                socket.on("wordB", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isPlaying) {
                                    return;
                                }
                                if (isExercise) {
                                    return;
                                }
                                try {
                                    JSONArray params = (JSONArray) args[0];
                                    String answerResult = (String) params.get(0);

                                    if (answerResult.equals("true")) {
                                        playerB.droppingWordView.setY(0);
                                    } else if (answerResult.equals("false")) {
                                        dropWord2Bottom(playerB);
                                    }

                                    String spell = (String) params.get(1);
                                    WordVo word = new WordVo(spell);
                                    playerB.currWord = word;

                                    // 更新界面上的单词显示
                                    getAvailableActivity(new IActivityEnabledListener() {
                                        @Override
                                        public void onActivityEnabled(MainActivity activity) {
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    playerB.droppingWordView.setText(playerB.currWord.getSpell());
                                                }
                                            });
                                        }
                                    });
                                } catch (JSONException e) {
                                    Log.e("", e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                socket.off("loser");
                socket.on("loser", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isPlaying = false;
                                isShowingResult = true;
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        isShowingResult = false;
                                    }
                                }, 4000);

                                Integer loserId = (Integer) args[0];
                                if (loserId.equals(loggedInUser.getId())) {
                                    if (isExercise) {
                                        isExercise = false;
                                        gameResultHint1 = "游戏结束！";
                                        gameResultHint2 = "回答错误的单词，已被自动加入到生词本";
                                    } else {
                                        gameResultHint1 = "失败了，别灰心，继续努力！";
                                        gameResultHint2 = "回答错误的单词，已被自动加入到生词本";
                                    }

                                    playSound(R.raw.failed);

                                } else {
                                    gameResultHint1 = "胜利啦！";
                                    gameResultHint2 = "回答错误的单词，已被自动加入到生词本";
                                    dropWord2Bottom(playerB);
                                    playSound(R.raw.victory);
                                }

                                // 游戏已经分出胜负，允许查询单词的意思了
                                forbiddenPopupDetailForAllWords = false;
                                forbiddenWordForDetail = "";

                                renderGameResult();
                            }
                        });
                    }
                });
            }
        });
    }

    private void renderScoreAdjust(Player player, boolean hide) {
        ViewGroup scoreAdjustArea = (ViewGroup) player.info.findViewById(R.id.scoreAdjustArea);
        if (hide) {
            scoreAdjustArea.setVisibility(View.GONE);
        } else {
            scoreAdjustArea.setVisibility(View.VISIBLE);
            TextView scoreAdjustView = (TextView) scoreAdjustArea.findViewWithTag("scoreAdjust");
            scoreAdjustView.setText(player.scoreAdjust > 0 ? "+ " + player.scoreAdjust : "- " + player.scoreAdjust * (-1));
            scoreAdjustView.setTextColor(player.scoreAdjust > 0 ? Color.GREEN : Color.RED);
            TextView cowdungAdjustView = (TextView) scoreAdjustArea.findViewWithTag("cowdungAdjust");
            cowdungAdjustView.setText(player.cowdungAdjust > 0 ? "+ " + player.cowdungAdjust : "- " + player.cowdungAdjust * (-1));
            cowdungAdjustView.setTextColor(player.cowdungAdjust > 0 ? Color.GREEN : Color.RED);
        }
    }

    private void renderUserInfo(Player player) {
        ((TextView) player.info.findViewWithTag("nickname")).setText(player.gameInfo == null ? "" : player.gameInfo.getNickName());
        ((TextView) player.info.findViewWithTag("gameScore")).setText(player.gameInfo == null ? "" : String.valueOf(player.gameInfo.getScore()));
        ((TextView) player.info.findViewWithTag("cowDung")).setText(player.gameInfo == null ? "" : String.valueOf(player.gameInfo.getCowDung()));
        ((TextView) player.info.findViewWithTag("winAndLost")).setText(player.gameInfo == null ? "" :
                String.format("%d胜%d负", player.gameInfo.getWinCount(), player.gameInfo.getLostCount()));
        ((TextView) player.info.findViewWithTag("winRatio")).setText(player.gameInfo == null ? "" :
                player.gameInfo.getWinCount() + player.gameInfo.getLostCount() == 0 ? "-" :
                        String.format("%.2f%%", player.gameInfo.getWinCount() * 100.0 / (player.gameInfo.getWinCount() + player.gameInfo.getLostCount() + 0.0)));

        ((TextView) player.info.findViewWithTag("status")).setText(player.gameInfo == null ? "" : player.started ? "已开始..." : "");
    }

    private void renderGameResult() {
        ((TextView) getView().findViewById(R.id.gameResultHint1)).setText(gameResultHint1);
        ((TextView) getView().findViewById(R.id.gameResultHint2)).setText(gameResultHint2);
    }

    private void renderRoomId() {
        ((Button) getView().findViewById(R.id.btnChangeRoom)).setText(String.format("(%d号) 换房间", roomId));
    }

    private void runOnUiThread(final Runnable runnable) {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.runOnUiThread(runnable);
            }
        });
    }

    private void renderProps() {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView view = (TextView) getView().findViewById(R.id.plus_count);
                        view.setText(String.valueOf(playerA.props[0]));
                        View incArea = getView().findViewById(R.id.incArea);
                        if (playerA.props[0] > 0 && isPlaying) {
                            incArea.setVisibility(View.VISIBLE);
                        } else {
                            incArea.setVisibility(View.GONE);
                        }

                        view = (TextView) getView().findViewById(R.id.minus_count);
                        view.setText(String.valueOf(playerA.props[1]));
                        View decArea = getView().findViewById(R.id.decArea);
                        if (playerA.props[1] > 0 && isPlaying) {
                            decArea.setVisibility(View.VISIBLE);
                        } else {
                            decArea.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }

    private void playSound(final int soundId) {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                Util.playSoundByResId(soundId, activity);
            }
        });
    }

    private void renderPlayerAWord() {
        if (playerA.currWord != null) {
            playerA.droppingWordView.setText(playerA.currWord.getSpell());
            renderAnswerBtns();
        }
    }

    private void liftDeadWords(final Player player, final int delta) {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (delta >= 0) {
                            player.jacksArea.getLayoutParams().height += delta;
                        } else {
                            if (player.jacksArea.getLayoutParams().height > 0) {
                                player.jacksArea.getLayoutParams().height += delta;
                            } else if (player.deadWordsArea.getChildCount() > 0) {
                                removeDeadWordAtBottom(player);
                            }
                        }
                    }
                });
            }
        });
    }

    private void appendMsg(int senderId /* 发送者ID，为0表示系统 */, String senderNickName, String msg) {

    }

    private void renderAnswerBtns() {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 根据随机生成的正确答案序号，排列三个备选答案
                        List<String> answers = new ArrayList<>();
                        answers.add(playerA.otherWordMeanings[0]);
                        answers.add(playerA.otherWordMeanings[1]);
                        answers.add(playerA.correctIndex - 1, playerA.currWord.getMeaningStr());

                        getBtn1().setText(answers.get(0));
                        getBtn2().setText(answers.get(1));
                        getBtn3().setText(answers.get(2));
                    }
                });
            }
        });
    }

    /**
     * 根据游戏当前状态显示/隐藏界面元素
     */
    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getView() == null) {
                    return;
                }

                // 玩家A信息区/动画区切换
                if (isPlaying) {
                    playerA.info.setVisibility(View.GONE);
                    playerA.field.setVisibility(View.VISIBLE);
                } else {
                    playerA.info.setVisibility(View.VISIBLE);
                    playerA.field.setVisibility(View.GONE);
                }

                // 玩家B信息区/动画区切换
                if (isPlaying && !isExercise) {
                    playerB.info.setVisibility(View.GONE);
                    playerB.field.setVisibility(View.VISIBLE);
                } else {
                    playerB.info.setVisibility(View.VISIBLE);
                    playerB.field.setVisibility(View.GONE);
                }

                // 道具区显示/隐藏
                renderProps();

                // 控制按钮区显示/隐藏
                ViewGroup controlBtns = (ViewGroup) getView().findViewById(R.id.controlBtns);
                if (isPlaying || isShowingResult) {
                    controlBtns.setVisibility(View.GONE);
                } else {
                    controlBtns.setVisibility(View.VISIBLE);
                }

                // 开始比赛按钮显示/隐藏
                Button startMatchBtn = (Button) getView().findViewById(R.id.btnStartGame);
                if (gameState.equals("ready") && !playerA.started) {
                    startMatchBtn.setVisibility(View.VISIBLE);
                } else {
                    startMatchBtn.setVisibility(View.GONE);
                }

                // 邀请对手按钮显示/隐藏
                Button inviteBtn = (Button) getView().findViewById(R.id.btnInvite);
                if (gameState.equals("waiting")) {
                    inviteBtn.setVisibility(View.VISIBLE);
                } else {
                    inviteBtn.setVisibility(View.GONE);
                }

                // 答案按钮区显示/隐藏
                ViewGroup answerBtns = (ViewGroup) getView().findViewById(R.id.answerBtns);
                if (isPlaying && playerA.currWord != null) {
                    answerBtns.setVisibility(View.VISIBLE);

                    Button btn4 = (Button) getView().findViewById(R.id.btn4);
                    Button btn5 = (Button) getView().findViewById(R.id.btn5);
                    if (isExercise) {
                        btn4.setVisibility(View.VISIBLE);
                        btn5.setVisibility(View.VISIBLE);
                    } else {
                        btn4.setVisibility(View.GONE);
                        btn5.setVisibility(View.GONE);
                    }
                } else {
                    answerBtns.setVisibility(View.GONE);
                }

                // 比赛结果展示区显示/隐藏
                ViewGroup gameResult = (ViewGroup) getView().findViewById(R.id.gameResult);
                if (isShowingResult) {
                    gameResult.setVisibility(View.VISIBLE);
                } else {
                    gameResult.setVisibility(View.GONE);
                }

                // 连接中断告警
                TextView connectionBrokenWarn = (TextView) getView().findViewById(R.id.connectionBrokenWarn);
                if (socket.connected()) {
                    connectionBrokenWarn.setVisibility(View.GONE);
                } else {
                    connectionBrokenWarn.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void initGame() {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                loggedInUser = ((MainActivity) activity).getAppContext().getLoggedInUser();
            }
        });

        initSocket();

        // player A
        playerA.field = (ViewGroup) getView().findViewById(R.id.playerAField);
        playerA.field.getLayoutParams().height = playerA.playGroundHeight;
        playerA.info = (ViewGroup) getView().findViewById(R.id.playerAInfo);
        playerA.info.getLayoutParams().height = playerA.playGroundHeight;
        playerA.droppingWordView = (TextView) getView().findViewById(R.id.playerADroppingWord);
        playerA.droppingWordView.setY(0);
        playerA.droppingWordView.setText("");
        playerA.deadWordsArea = (ViewGroup) getView().findViewById(R.id.playerADeadWordsArea);
        playerA.jacksArea = (View) getView().findViewById(R.id.playerAJacksArea);
        playerA.droppingWordView.setHeight(playerA.wordDivHeight);
        playerA.info.setVisibility(View.VISIBLE);
        playerA.field.setVisibility(View.GONE);
        renderProps();

        // player B
        playerB.field = (ViewGroup) getView().findViewById(R.id.playerBField);
        playerB.field.getLayoutParams().height = playerB.playGroundHeight;
        playerB.info = (ViewGroup) getView().findViewById(R.id.playerBInfo);
        playerB.info.getLayoutParams().height = playerB.playGroundHeight;
        playerB.droppingWordView = (TextView) getView().findViewById(R.id.playerBDroppingWord);
        playerB.droppingWordView.setY(0);
        playerB.droppingWordView.setText("");
        playerB.deadWordsArea = (ViewGroup) getView().findViewById(R.id.playerBDeadWordsArea);
        playerB.jacksArea = (View) getView().findViewById(R.id.playerBJacksArea);
        playerB.droppingWordView.setHeight(playerB.wordDivHeight);
        playerB.info.setVisibility(View.VISIBLE);
        playerB.field.setVisibility(View.GONE);

        // [开始比赛]按钮
        View btnStartGame = getView().findViewById(R.id.btnStartGame);
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMatch();
            }
        });

        // [邀请对手]按钮
        View btnInvite = getView().findViewById(R.id.btnInvite);
        btnInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showIdleUsers();
            }
        });

        // [换房间]按钮
        View btnChangeRoom = getView().findViewById(R.id.btnChangeRoom);
        btnChangeRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeRoom();
            }
        });

        // [单人练习]按钮
        View btnExercise = getView().findViewById(R.id.btnExercise);
        btnExercise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exercise();
            }
        });

        // [离开]按钮
        View btnExit = getView().findViewById(R.id.btnExit);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });

        // [answer1]按钮
        getBtn1().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAnswer(1);
            }
        });

        // [answer2]按钮
        getBtn2().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAnswer(2);
            }
        });

        // [answer3]按钮
        getBtn3().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAnswer(3);
            }
        });

        // [answer4]按钮
        getBtn4().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAnswer(4);
            }
        });

        // [answer5]按钮
        getBtn5().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAnswer(5);
            }
        });

        // [加]道具
        ImageButton inc = (ImageButton) getView().findViewById(R.id.inc);
        inc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useProps(0);
            }
        });

        // [减]道具
        ImageButton dec = (ImageButton) getView().findViewById(R.id.dec);
        dec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useProps(1);
            }
        });

        showingResultTimer = new Timer();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                gameloop();
            }
        }, 0, timeInterval);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateUI();
            }
        }, 0, 100);

        tryEnterGameHall();
    }

    private void useProps(int propsType) {
        if (propsType == 0) { // 加一行
            sendUserCmd("USE_PROPS", new Object[]{propsType});
        } else { // 减一行
            if (playerA.jacksArea.getLayoutParams().height > 0 || playerA.deadWordsArea.getChildCount() > 0) { // 避免浪费道具
                sendUserCmd("USE_PROPS", new Object[]{propsType});
            }
        }
    }

    private Button getBtn1() {
        return (Button) getView().findViewById(R.id.btn1);
    }

    private Button getBtn2() {
        return (Button) getView().findViewById(R.id.btn2);
    }

    private Button getBtn3() {
        return (Button) getView().findViewById(R.id.btn3);
    }

    private Button getBtn4() {
        return (Button) getView().findViewById(R.id.btn4);
    }

    private Button getBtn5() {
        return (Button) getView().findViewById(R.id.btn5);
    }

    private void onClickAnswer(int btnIndex) {
        if (!isPlaying || playerA.currWord == null) {
            return;
        }

        if (btnIndex == 5) { // 结束练习
            sendGameOverCmd(playerA);
        } else if (btnIndex == this.playerA.correctIndex) { // 选对了
            playerA.droppingWordView.setY(0); // 下落中的单词回到最上方

            sendUserCmd("GET_NEXT_WORD", new Object[]{playerA.wordIndex++, "true", playerA.currWord.getSpell()});
        } else { // 选错了
            dropWord2Bottom(this.playerA);
            sendUserCmd("GET_NEXT_WORD", new Object[]{playerA.wordIndex++, "false", playerA.currWord.getSpell()});
        }
        playerA.currWord = null;

        updateUI();
    }

    @Override
    public void onDestroyView() {
        timer.cancel();
        timer = null;
        showingResultTimer.cancel();
        showingResultTimer = null;
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.getAppContext().unregisterSocketStatusListener(RussiaFragment.this);
            }
        });
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add("关闭").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals("关闭")) {

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentSwitched(MyFragment from, MyFragment to) {
        if (from == this) {
            sendUserCmd("LEAVE_HALL", new Object[]{});
        }
    }

    String gameState = "";
    volatile boolean isShowingResult = false;
    String gameResultHint1 = "";
    String gameResultHint2 = "";
    String gameResultHint3 = "";
    int roomId = 0;
    List<String> msgs = new LinkedList<>();
    boolean isExercise = false;
    String wordSoundFile = "";
    List<UserVo> idleUsers = new ArrayList<>();

    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    private class Player {
        protected Player(String code) {
            this.code = code;
        }

        Integer userId;
        boolean started = false; // 是否已经点击了【开始比赛】按钮
        String nickName = "";
        int score;
        int cowDung;
        int winCount;
        int lostCount;
        int dropSpeed = 2;
        WordVo currWord;
        int wordDivHeight = 40;
        int wordIndex = 0;
        int correctCount = 0;
        int playGroundHeight = 400;
        int correctIndex = -1; // 正确答案序号
        String[] otherWordMeanings = {"", ""}; // 所有备选答案的内容
        int[] props = new int[]{0, 0}; // 每种道具的数量
        String code;
        UserGameInfo gameInfo;
        int scoreAdjust; // 最近一局比赛导致的积分调整量
        int cowdungAdjust;// 最近一局比赛导致的牛粪调整量

        ViewGroup field;
        ViewGroup info;
        TextView droppingWordView;
        List<WordVo> deadWords = new LinkedList<>(); // 死亡单词（注意，当使用【减一行】道具时，相应的死亡单词并不会删除，而仅仅是在界面上删除）
        ViewGroup deadWordsArea;
        View jacksArea;
    }

    private Timer timer;
    private Timer showingResultTimer; // 显示比赛结果定时器，若干秒后结束显示
    private static final int timeInterval = 200;  // 游戏主循环的时间间隔
    private boolean isPlaying;
    private final Player playerA = new Player("A");
    private final Player playerB = new Player("B");

    private void gameloop() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    moveWord(playerA);
                    if (!isExercise) {
                        moveWord(playerB);
                    }
                }
            }
        });
    }

    private void startGame() {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                isPlaying = true;
                                resetProps();
                                initGameForPlayer(playerA);
                                initGameForPlayer(playerB);

                                showingResultTimer.cancel();
                                sendUserCmd("GET_NEXT_WORD", new Object[]{playerA.wordIndex++, "", ""});
                            }
                        }
                );
            }
        });
    }

    private void startMatch() {
        this.sendUserCmd("START_GAME", new Object[]{});
    }

    private void showIdleUsers() {
        socket.emit("getIdleUsers", 5);
    }

    private void changeRoom() {
        getMainActivity().switchToRussiaFragment(RussiaFragment.this, hallName, roomId);
    }

    private void exit() {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.switchToMeFragment(RussiaFragment.this);
            }
        });
    }

    private void exercise() {
        sendUserCmd("START_EXERCISE", new Object[]{});
    }

    private void initGameForPlayer(final Player player) {
        if (player == playerA || (player == playerB && !isExercise)) {
            player.info.setVisibility(View.GONE);
            player.field.setVisibility(View.VISIBLE);
        }

        clearDeadWords(player);
        player.wordIndex = 0;
        player.correctCount = 0;
        player.droppingWordView.setY(0);
        player.jacksArea.getLayoutParams().height = 0;
        if (player == playerA) {
            renderScoreAdjust(player, true);
        }
        player.started = false;
    }

    /**
     * 道具清零
     */
    private void resetProps() {
        for (int i = 0; i < playerA.props.length; i++) {
            playerA.props[i] = 0;
        }
        renderProps();
    }

    private int deadTopOfPlayer(Player player) {
        return player.deadWordsArea.getTop();
        //return player.playGroundHeight - player.bottomHeight - player.wordDivHeight * player.deadWords.size();
    }

    private void dropWord2Bottom(final Player player) {
        player.deadWords.add(player.currWord);
        player.droppingWordView.setY(0);
        final WordVo deadWord = player.currWord;

        addDeadWord(deadWord, player);
        player.droppingWordView.setText("");
    }

    /**
     * 把View Group中的底部的那个Item移动到顶部
     *
     * @param viewGroup
     */
    private static void moveBottonItem2Top(ViewGroup viewGroup) {
        if (viewGroup.getChildCount() < 2) {
            return;
        }

        for (int k = 0; k <= viewGroup.getChildCount() - 2; k++) {
            View item = viewGroup.getChildAt(0);
            viewGroup.removeViewAt(0);
            viewGroup.addView(item);
        }
    }

    private void removeDeadWordAtBottom(Player player) {
        player.deadWordsArea.removeViewAt(player.deadWordsArea.getChildCount() - 1);
    }

    private void addDeadWord(final WordVo deadWord, final Player player) {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                TextView textView = new TextView(activity);
                textView.setText(deadWord.getSpell());
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                textView.setLayoutParams(layoutParams);
                textView.setTextColor(Color.RED);
                textView.setTextSize(COMPLEX_UNIT_SP, 16);
                textView.setHeight(player.wordDivHeight);
                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                player.deadWordsArea.addView(textView);

                moveBottonItem2Top(player.deadWordsArea);
            }
        });
    }

    private void clearDeadWords(final Player player) {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        player.deadWords.clear();
                        player.deadWordsArea.removeAllViews();
                    }
                });
            }
        });
    }

    private void sendGameOverCmd(Player player) {
        this.sendUserCmd("GAME_OVER", new Object[]{player.code});
    }

    private class UserCmd {
        UserCmd(Integer userId, String system, String cmd, Object[] args) {
            this.userId = userId;
            this.system = system;
            this.cmd = cmd;
            this.args = args;
        }

        Integer userId;
        String system;
        String cmd;
        Object[] args;
    }

    /**
     * 申请进入游戏大厅
     */
    private void tryEnterGameHall() {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                if (activity.isUserReportedToSocketServer()) {
                    sendUserCmd("ENTER_GAME_HALL", new Object[]{hallName, exceptRoom});
                }
            }
        });
    }

    private void sendUserCmd(final String cmd, final Object[] args) {
        JSONArray argsArray = new JSONArray();
        for (int i = 0; i < args.length; i++) {
            argsArray.put(args[i]);
        }

        JSONObject cmdObject = new JSONObject();
        try {
            cmdObject.put("userId", loggedInUser.getId());
            cmdObject.put("system", "russia");
            cmdObject.put("cmd", cmd);
            cmdObject.put("args", argsArray);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("sendUserCmd", "系统异常");
        }
        socket.emit("userCmd", cmdObject);
    }

    private boolean isPlayGroundFull(Player player) {
        return deadTopOfPlayer(player) <= player.wordDivHeight;
    }

    private void onPlayGroudFull(final Player player) {
        this.sendGameOverCmd(player);
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        player.droppingWordView.setText("");
                    }
                });
            }
        });
    }

    private void moveWord(Player player) {
        if (player.currWord == null) {
            return;
        }

        // 下落
        TextView droppingWordView = player.droppingWordView;
        float wordBottom = droppingWordView.getY() + droppingWordView.getHeight();
        float gap = deadTopOfPlayer(player) - wordBottom;
        float delta = Math.min(player.dropSpeed, gap);
        droppingWordView.setY(droppingWordView.getY() + delta);

        // 碰撞检测
        if (droppingWordView.getY() + droppingWordView.getHeight() >= this.deadTopOfPlayer(player)) {
            if (player == this.playerA) {
                // 触到底部则单词死亡
                dropWord2Bottom(player);

                // 达到顶部则游戏结束(己方窗口达到顶部)
                if (isPlayGroundFull(player)) {
                    onPlayGroudFull(player);
                    return;
                }

                // 取下一个单词
                this.sendUserCmd("GET_NEXT_WORD", new Object[]{player.wordIndex++, "false", player.currWord.getSpell()});
                player.currWord = null;
            } else {
                // 达到顶部则游戏结束(对方窗口达到顶部)
                if (isPlayGroundFull(player)) {
                    onPlayGroudFull(player);
                    return;
                }
            }
        }
    }
}