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
import android.widget.LinearLayout;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import beidanci.vo.SearchWordResult;
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
        socket = getMainActivity().getAppContext().getSocket();
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
        socket.on("idleUsers", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                idleUsers = (List<UserVo>) args[0];
            }
        });
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
        socket.on("giveProps", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (!isExercise) {
                    try {
                        JSONArray params = (JSONArray) args[0];
                        int propsIndex = (Integer) params.get(0);
                        int propsCount = (int) params.get(1);
                        playerA.props[propsIndex] = propsCount;
                    } catch (JSONException e) {
                        Log.e("", e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });
        socket.on("noEnoughCowDung", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                ToastUtil.showToast(getMainActivity(), "开始游戏需要至少" + (int) args[0] + "个牛粪");
            }
        });
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
                    if (getMainActivity() != null) {
                        MediaPlayer mediaPlayer = MediaPlayer.create(getMainActivity(), R.raw.enterroom);// 得到声音资源
                        mediaPlayer.start();
                    }

                    appendMsg(0, "牛牛", nickName + "进来了");
                } catch (JSONException e) {
                    Log.e("", e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        socket.on("propsUsed", new Emitter.Listener()

        {
            @Override
            public void call(Object... args) {
                int userId = (int) args[0];
                int propsIndex = (int) args[1];
                int currNumber = (int) args[2];
                String nickName = (String) args[3];
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
            }
        });
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

                    // 更新界面上的单词显示
                    getMainActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playerA.wordView.setText(playerA.currWord.getSpell());
                        }
                    });

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

                    //下载单词发音并自动发音
                    Util.downloadPronounceAndPlay(playerA.currWord.getSpell(), null, mediaPlayer, getString(R.string.sound_base_url));
                } catch (JSONException e) {
                    Log.e("", e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void liftDeadWords(Player player, int delta) {
        if (delta >= 0) {
            player.bottomHeight += delta;
        } else {
            if (player.bottomHeight > 0) {
                player.bottomHeight += delta;
            } else if (player.deadWords.size() > 0) {
                player.deadWords.remove(0);
            }
        }
    }

    private void appendMsg(int senderId /* 发送者ID，为0表示系统 */, String senderNickName, String msg) {

    }

    private void initGame() {
        loggedInUser = getMainActivity().getAppContext().getLoggedInUser();
        initSocket();

        ViewGroup field = (ViewGroup) getView().findViewById(R.id.myField);
        field.getLayoutParams().height = playerA.playGroundHeight;
        field.setBackgroundColor(Color.BLUE);
        playerA.field = field;
        playerA.wordView = (TextView) getView().findViewById(R.id.myWordSpell);
        playerA.deadWordsArea = (ViewGroup) getView().findViewById(R.id.myDeadWordsArea);
        playerA.jacksArea = (ViewGroup) getView().findViewById(R.id.myJacksArea);
        playerA.wordView.setHeight(playerA.wordDivHeight);
        playerA.wordView.setBackgroundColor(Color.GREEN);

        field = (LinearLayout) getView().findViewById(R.id.hisField);
        field.getLayoutParams().height = playerB.playGroundHeight;
        field.setBackgroundColor(Color.RED);
        playerB.field = field;
        playerB.wordView = (TextView) getView().findViewById(R.id.hisWordSpell);
        playerB.deadWordsArea = (ViewGroup) getView().findViewById(R.id.hisDeadWordsArea);
        playerB.jacksArea = (ViewGroup) getView().findViewById(R.id.hisJacksArea);
        playerB.wordView.setHeight(playerB.wordDivHeight);

        View btnStartGame = getView().findViewById(R.id.btnStartGame);
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMatch();
            }
        });

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                gameloop();
            }
        }, 0, timeInterval);

        tryEnterGameHall();
    }

    @Override
    public void onDestroyView() {
        timer.cancel();
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
        int bottomTop = 400; // 千斤顶的顶端位置
        int bottomHeight = 0;
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
        ViewGroup jacksArea;
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
        player.deadWords.clear();
        player.wordIndex = 0;
        player.correctCount = 0;
        player.currWordTop = 0;
        player.bottomTop = player.playGroundHeight;
        player.bottomHeight = 0;
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

    private void dropWord2Bottom(Player player) {
        player.deadWords.add(player.currWord);
        player.currWordTop = 0;

        addDeadWord(player.currWord, player);
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

        for (int k =  0; k <= viewGroup.getChildCount()-2; k++) {
            View item = viewGroup.getChildAt(0);
            viewGroup.removeViewAt(0);
            viewGroup.addView(item);
        }
    }

    private void addDeadWord(final WordVo deadWord, final Player player) {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = new TextView(getMainActivity());
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

    private UserVo getUser() {
        if (getMainActivity() == null) {
            return null;
        }
        return getMainActivity().getAppContext().getLoggedInUser();
    }

    /**
     * 申请进入游戏大厅
     */
    private void tryEnterGameHall() {
        if (getMainActivity().isUserReportedToSocketServer()) {
            this.sendUserCmd("ENTER_GAME_HALL", new Object[]{hallName, exceptRoom});
        }
    }

    private void sendUserCmd(final String cmd, final Object[] args) {
        //socket.emit("userCmd", new UserCmd(getUser().getId(), "russia", cmd, args));
        JSONArray argsArray = new JSONArray();
        for (int i = 0; i < args.length; i++) {
            argsArray.put(args[i]);
        }

        JSONObject cmdObject = new JSONObject();
        try {
            cmdObject.put("userId", getUser().getId());
            cmdObject.put("system", "russia");
            cmdObject.put("cmd", cmd);
            cmdObject.put("args", argsArray);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("sendUserCmd", "系统异常");
        }
        socket.emit("userCmd", cmdObject);
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
                if (deadTopOfPlayer(player) <= 15) {
                    this.isPlaying = false;
                    this.sendGameOverCmd(player);
                    return;
                }

                // 取下一个单词
                this.sendUserCmd("GET_NEXT_WORD", new Object[]{player.wordIndex++, "false", player.currWord.getSpell()});
            } else {
                // 达到顶部则游戏结束(对方窗口达到顶部)
                if (this.deadTopOfPlayer(player) <= 15) {
                    this.isPlaying = false;
                    this.sendGameOverCmd(player);
                    return;
                }
            }
        }

    }
}