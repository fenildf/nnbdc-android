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
import com.google.gson.reflect.TypeToken;
import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import beidanci.vo.UserVo;
import beidanci.vo.WordVo;

import static android.util.TypedValue.COMPLEX_UNIT_SP;


public class RussiaFragment extends MyFragment {
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private Socket socket;
    private String hallName;
    private String exceptRoom;

    UserVo loggedInUser;

    /**
     * 点击时不允许显示详情的单词（避免泄露答案）
     */
    String forbiddenWordForDetail;

    boolean forbiddenPopupDetailForAllWords = false;

    public RussiaFragment() {
    }

    public RussiaFragment(String hallName, String exceptRoom) {
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_russia, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initGame();
    }

    private void initSocket() {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                socket = activity.getAppContext().getSocket();
                socket.off("sysCmd");
                socket.on("sysCmd", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
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
                socket.off("idleUsers");
                socket.on("idleUsers", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        idleUsers = (List<UserVo>) args[0];
                    }
                });
                socket.off("userStarted");
                socket.on("userStarted", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        int userId = (int) args[0];
                        if (userId == playerA.userId) {
                             playerA.started = true;
                        } else {
                            playerB.started = true;
                        }
                    }
                });
                socket.off("giveProps");
                socket.on("giveProps", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        if (!isExercise) {
                            try {
                                JSONArray params = (JSONArray) args[0];
                                int propsIndex = (Integer) params.get(0);
                                int propsCount = (int) params.get(1);
                                playerA.props[propsIndex] = propsCount;
                                renderProps();
                            } catch (JSONException e) {
                                Log.e("", e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                });
                socket.off("noEnoughCowDung");
                socket.on("noEnoughCowDung", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        final int minCount = (int) args[0];
                        getAvailableActivity(new IActivityEnabledListener() {
                            @Override
                            public void onActivityEnabled(MainActivity activity) {
                                ToastUtil.showToast(activity, "开始游戏需要至少" + minCount + "个牛粪");
                            }
                        });
                    }
                });
                socket.off("enterRoom");
                socket.on("enterRoom", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        try {
                            JSONArray params = (JSONArray) args[0];
                            int userId = (int) params.get(0);
                            String nickName = (String) params.get(1);
                            Player player = userId == loggedInUser.getId() ? playerA : playerB;
                            player.userId = userId;

                            //播放开门声
                            playSound(R.raw.enterroom);

                            appendMsg(0, "牛牛", nickName + "进来了");
                        } catch (JSONException e) {
                            Log.e("", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
                socket.off("propsUsed");
                socket.on("propsUsed", new Emitter.Listener()

                {
                    @Override
                    public void call(Object... args) {
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
                socket.off("wordA");
                socket.on("wordA", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
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
                            getAvailableActivity(new IActivityEnabledListener() {
                                @Override
                                public void onActivityEnabled(MainActivity activity) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            renderPlayerAWord();
                                            updateUI();
                                        }
                                    });

                                    //下载单词发音并自动发音
                                    Util.downloadPronounceAndPlay(playerA.currWord.getSpell(), null, mediaPlayer, getString(R.string.sound_base_url));
                                }
                            });

                        } catch (JSONException e) {
                            Log.e("", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
                socket.off("wordB");
                socket.on("wordB", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
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
                                playerB.currWordTop = 0;
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
                                            playerB.wordView.setText(playerB.currWord.getSpell());
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
                socket.off("loser");
                socket.on("loser", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        isPlaying = false;
                        isShowingResult = true;
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                isShowingResult = false;
                            }
                        }, 4000);

                        Integer loserId = (Integer) args[0];
                        if (loserId == loggedInUser.getId()) {
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
                            playSound(R.raw.victory);
                        }

                        // 游戏已经分出胜负，允许查询单词的意思了
                        forbiddenPopupDetailForAllWords = false;
                        forbiddenWordForDetail = "";
                    }
                });
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

                        view = (TextView) getView().findViewById(R.id.minus_count);
                        view.setText(String.valueOf(playerA.props[1]));
                    }
                });
            }
        });
    }

    private void playSound(final int soundId) {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                MediaPlayer mediaPlayer = MediaPlayer.create(activity, soundId);// 得到声音资源
                mediaPlayer.start();
            }
        });
    }

    private void renderPlayerAWord() {
        playerA.wordView.setText(playerA.currWord.getSpell());
        renderAnswerBtns();
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
                            } else if (player.deadWords.size() > 0) {
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
     * 显示/隐藏 控制按钮
     */
    private void updateUI() {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (getView() == null) {
                            return;
                        }
                        ViewGroup controlBtns = (ViewGroup) getView().findViewById(R.id.controlBtns);
                        if (isPlaying || isExercise || isShowingResult) {
                            controlBtns.setVisibility(View.GONE);
                        } else {
                            controlBtns.setVisibility(View.VISIBLE);
                        }

                        ViewGroup answerBtns = (ViewGroup) getView().findViewById(R.id.answerBtns);
                        if ((isPlaying || isExercise) && playerA.currWord != null) {
                            answerBtns.setVisibility(View.VISIBLE);
                        } else {
                            answerBtns.setVisibility(View.GONE);
                        }
                    }
                });
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

        ViewGroup field = (ViewGroup) getView().findViewById(R.id.myField);
        field.getLayoutParams().height = playerA.playGroundHeight;
        playerA.field = field;
        playerA.wordView = (TextView) getView().findViewById(R.id.myWordSpell);
        playerA.deadWordsArea = (ViewGroup) getView().findViewById(R.id.myDeadWordsArea);
        playerA.jacksArea = (View) getView().findViewById(R.id.myJacksArea);
        playerA.wordView.setHeight(playerA.wordDivHeight);

        field = (ViewGroup) getView().findViewById(R.id.hisField);
        field.getLayoutParams().height = playerB.playGroundHeight;
        playerB.field = field;
        playerB.wordView = (TextView) getView().findViewById(R.id.hisWordSpell);
        playerB.deadWordsArea = (ViewGroup) getView().findViewById(R.id.hisDeadWordsArea);
        playerB.jacksArea = (View) getView().findViewById(R.id.hisJacksArea);
        playerB.wordView.setHeight(playerB.wordDivHeight);

        // [开始比赛]按钮
        View btnStartGame = getView().findViewById(R.id.btnStartGame);
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMatch();
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
            if (playerA.jacksArea.getLayoutParams().height > 0 || playerA.deadWords.size() > 0) { // 避免浪费道具
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

    private void onClickAnswer(int btnIndex) {
        if (!isPlaying || playerA.currWord == null) {
            return;
        }

        if (btnIndex == 5) {
            isPlaying = false;
            sendGameOverCmd(playerA);
        } else if (btnIndex == this.playerA.correctIndex) { // 选对了
            playerA.currWordTop = 0;
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
    boolean isShowingResult = false;
    String gameResultHint1 = "";
    String gameResultHint2 = "";
    String gameResultHint3 = "";
    int roomId = 0;
    List<String> msgs = new LinkedList<>();
    boolean isExercise = false;
    String wordSoundFile = "";
    boolean isInviting = false; // 是否正在邀请其他用户
    List<UserVo> idleUsers;

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
        List<WordVo> deadWords = new LinkedList<>();
        WordVo currWord;
        int wordDivHeight = 40;
        int currWordTop = 0;
        int wordIndex = 0;
        int correctCount = 0;
        int playGroundHeight = 400;
        int correctIndex = -1; // 正确答案序号
        String[] otherWordMeanings = {"", ""}; // 所有备选答案的内容
        int[] props = new int[]{0, 0}; // 每种道具的数量
        String code;

        ViewGroup field;
        TextView wordView;
        ViewGroup deadWordsArea;
        View jacksArea;
    }

    private Timer timer;
    private static final int timeInterval = 200;  // 游戏主循环的时间间隔
    private boolean isPlaying;
    private final Player playerA = new Player("A");
    private final Player playerB = new Player("B");

    private void gameloop() {
        if (this.isPlaying) {
            this.moveWord(playerA);
            this.moveWord(playerB);
        }
    }

    private void startGame() {
        this.isPlaying = true;
        this.resetProps();
        this.initGameForPlayer(this.playerA);
        this.initGameForPlayer(this.playerB);

        this.sendUserCmd("GET_NEXT_WORD", new Object[]{this.playerA.wordIndex++, "", ""});
    }

    private void startMatch() {
        this.sendUserCmd("START_GAME", new Object[]{});
    }

    private void initGameForPlayer(Player player) {
        clearDeadWords(player);
        player.wordIndex = 0;
        player.correctCount = 0;
        player.currWordTop = 0;
        player.jacksArea.getLayoutParams().height=0;
        player.started = false;
    }

    /**
     * 道具清零
     */
    private void resetProps() {
        for (int i = 0; i < playerA.props.length; i++) {
            playerA.props[i] = 0;
        }
    }

    private int deadTopOfPlayer(Player player) {
        return player.deadWordsArea.getTop();
        //return player.playGroundHeight - player.bottomHeight - player.wordDivHeight * player.deadWords.size();
    }

    private void dropWord2Bottom(final Player player) {
        player.deadWords.add(player.currWord);
        player.currWordTop = 0;

        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addDeadWord(player.currWord, player);
                        player.wordView.setText("");
                    }
                });
            }
        });
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
        this.isPlaying = false;
        this.sendGameOverCmd(player);
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(MainActivity activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        player.wordView.setText("");
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
        int wordBottom = player.currWordTop + player.wordDivHeight;
        int gap = deadTopOfPlayer(player) - wordBottom;
        int delta = Math.min(player.dropSpeed, gap);
        player.currWordTop += delta;
        wordBottom = player.currWordTop + player.wordDivHeight;
        TextView wordView = player.wordView;
        wordView.setY(player.currWordTop);

        // 碰撞检测
        if (wordBottom >= this.deadTopOfPlayer(player)) {
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