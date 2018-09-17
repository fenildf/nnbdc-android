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

import com.github.nkzawa.socketio.client.Socket;
import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import beidanci.vo.UserVo;
import beidanci.vo.WordVo;


public class RussiaFragment extends MyFragment {
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private Socket socket;
    private String hallName;
    private String exceptRoom;

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

    private void initGame() {
        socket = getMainActivity().getAppContext().getSocket();

        ViewGroup field = (ViewGroup) getView().findViewById(R.id.myField);
        field.setMinimumHeight(playerA.playGroundHeight);
        field.setBackgroundColor(Color.BLUE);
        playerA.field = field;
        playerA.wordView = (TextView) getView().findViewById(R.id.myWordSpell);

        field = (LinearLayout) getView().findViewById(R.id.hisField);
        field.setMinimumHeight(playerB.playGroundHeight);
        field.setBackgroundColor(Color.RED);
        playerB.field = field;
        playerB.wordView = (TextView) getView().findViewById(R.id.hisWordSpell);

        View btnStartGame = getView().findViewById(R.id.btnStartGame);
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
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
    public void onHiddenChanged(boolean hidden) {
        if(hidden){
            sendUserCmd("LEAVE_HALL", new Object[]{});
        }
        super.onHiddenChanged(hidden);
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
        List<WordVo> deadWords = new LinkedList<>();
        WordVo currWord;
        int wordDivHeight = 20;
        int currWordTop = 0;
        int bottomTop = 200; // 千斤顶的顶端位置
        int bottomHeight = 0;
        int wordIndex = 0;
        int correctCount = 0;
        int playGroundHeight = 400;
        int correctIndex = -1; // 正确答案序号
        String[] otherWordMeanings; // 所有备选答案的内容
        int[] props = {0, 0}; // 每种道具的数量
        String code;

        TextView wordView;
        ViewGroup field;
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
        return player.playGroundHeight - player.bottomHeight - player.wordDivHeight * player.deadWords.size();
    }

    private void dropWord2Bottom(Player player) {
        player.deadWords.add(player.currWord);
        player.currWordTop = 0;
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
        ToastUtil.showToast(getMainActivity(), "send user cmd:" + cmd);
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