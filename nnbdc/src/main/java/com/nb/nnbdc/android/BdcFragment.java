package com.nb.nnbdc.android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.ImmUtils;
import com.nb.nnbdc.android.util.LinkTouchMovementMethod;
import com.nb.nnbdc.android.util.MyProgress;
import com.nb.nnbdc.android.util.StringUtils;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;
import com.nb.nnbdc.android.util.WordDetailUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import beidanci.vo.GetWordResult;
import beidanci.vo.SentenceVo;
import beidanci.vo.UserVo;
import beidanci.vo.WordVo;

public class BdcFragment extends MyFragment {
    private LoadAWordTask loadAWordTask;
    private ContinueAtLastBreakPointTask continueAtLastBreakPointTask;
    private SwitchAutoPlaySentenceFlagTask switchAutoPlaySentenceFlagTask;


    /**
     * 正确答案的索引号
     */
    private int correctAnswerIndex;

    /**
     * 当前单词
     */
    private String currentSpell;

    /**
     * 当前单词是否回答正确
     */
    private boolean isAnswerCorrect;

    /**
     * 当前单词是否已经掌握
     */
    private boolean isWordMastered;

    /**
     * 当前正在学习的单词
     */
    private GetWordResult currentGetWordResult;

    /**
     * 当前正在学习的单词的第一个例句
     */
    private String currentEnglishDigest;

    private MediaPlayer mediaPlayer = new MediaPlayer();

    private String fromPage;

    /**
     * 是否正在显示一个单词的内容？
     */
    private boolean isShowingAWord = false;

    public void setFromPage(String fromPage) {
        this.fromPage = fromPage;
    }

    public boolean isShowingAWord() {
        return isShowingAWord;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //去除title
        // requestWindowFeature(Window.FEATURE_NO_TITLE);

        //全屏
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bdc, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentGetWordResult = null;
        isAnswerCorrect = false;
        isWordMastered = false;

        continueAtLastBreakPointTask = new ContinueAtLastBreakPointTask(getMainActivity());
        continueAtLastBreakPointTask.execute((Void) null);

        //答案点击事件处理
        View.OnClickListener answerBtnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    answerBtnClicked(v);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        getView().findViewById(R.id.answer1).setOnClickListener(answerBtnClickListener);
        getView().findViewById(R.id.answer2).setOnClickListener(answerBtnClickListener);
        getView().findViewById(R.id.answer3).setOnClickListener(answerBtnClickListener);
        getView().findViewById(R.id.answer5).setOnClickListener(answerBtnClickListener);
        getView().findViewById(R.id.answer6).setOnClickListener(answerBtnClickListener);

        //【下一个单词】
        getView().findViewById(R.id.nextWordBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadAWord();
            }
        });

        //单词发音按钮
        getView().findViewById(R.id.playPronounce).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPronounce();
            }
        });

        //点击单词拼写自动发音
        getView().findViewById(R.id.spell).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPronounce();
            }
        });

        //点击单词音标自动发音
        getView().findViewById(R.id.pronounce).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPronounce();
            }
        });

        //句子发音按钮
        getView().findViewById(R.id.playSentence).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSentence();
            }
        });

        //句子自动发音切换按钮
        getView().findViewById(R.id.autoPlaySentenceFlag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchAutoPlaySentenceFlag();
            }
        });

        //单词已掌握按钮
        getView().findViewById(R.id.wordMastered).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAnswerCorrect = true;
                isWordMastered = true;
                loadAWord();
                ToastUtil.showToast(getActivity(), String.format("[%s]已掌握，不再学习", currentSpell));
            }
        });

        //单词拼写提示按钮
        getView().findViewById(R.id.btnSpellTip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditText) getView().findViewById(R.id.edtSpell)).setText(currentSpell);
            }
        });
    }

    private void loadAWord() {
        getView().findViewById(R.id.meaningsLabel).setVisibility(View.GONE);
        getView().findViewById(R.id.meanings).setVisibility(View.GONE);
        getView().findViewById(R.id.sentencesLabel).setVisibility(View.GONE);
        getView().findViewById(R.id.sentences).setVisibility(View.GONE);
        getView().findViewById(R.id.nextWordBtn).setVisibility(View.GONE);
        getView().findViewById(R.id.hint).setVisibility(View.GONE);
        getView().findViewById(R.id.spell).setVisibility(View.GONE);
        getView().findViewById(R.id.pronounce).setVisibility(View.GONE);
        getView().findViewById(R.id.autoPlaySentenceFlag).setVisibility(View.GONE);
        getView().findViewById(R.id.playSentence).setVisibility(View.GONE);
        getView().findViewById(R.id.playPronounce).setVisibility(View.GONE);
        getView().findViewById(R.id.wordMastered).setVisibility(View.GONE);
        getView().findViewById(R.id.spellExercise).setVisibility(View.GONE);

        ((MyActivity) getActivity()).showProgress(true);
        loadAWordTask = new LoadAWordTask(getMainActivity());
        loadAWordTask.execute((Void) null);
    }

    @Override
    public void onFragmentSwitched(MyFragment from, MyFragment to) {

    }

    /**
     * 获取下一个单词
     */
    public class LoadAWordTask extends MyAsyncTask<Void, Void, GetWordResult> {
        protected LoadAWordTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected GetWordResult doInBackground(Void... params) {
            try {
                //如果是从阶段复习Activity跳转来的，则第一次从服务端取单词时，通知服务端进入下一个学习阶段
                boolean shouldEnterNextStage = false;
                if (fromPage != null && fromPage.equals(StageReviewFragment.class.getSimpleName())) {
                    shouldEnterNextStage = true;
                    fromPage = null;
                }
                return getHttpClient().getNextWord(isAnswerCorrect, isWordMastered, shouldEnterNextStage);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private TextView findAnswerBtnByIndex(int btnIndex) {
            if (btnIndex == 1) {
                return (TextView) getView().findViewById(R.id.answer1);
            }
            if (btnIndex == 2) {
                return (TextView) getView().findViewById(R.id.answer2);
            }
            if (btnIndex == 3) {
                return (TextView) getView().findViewById(R.id.answer3);
            }
            if (btnIndex == 5) {
                return (TextView) getView().findViewById(R.id.answer5);
            }
            if (btnIndex == 6) {
                return (TextView) getView().findViewById(R.id.answer6);
            }
            return null;
        }

        @Override
        protected void onPostExecute(final GetWordResult result) {
            super.onPostExecute(result);
            if (getView() == null || getActivity() == null) return;
            if (result == null) {
                ToastUtil.showToast(getActivity(), "系统异常");
                return;
            }

            loadAWordTask = null;
            ((MyActivity) getActivity()).showProgress(false);

            //隐藏/显示某些界面元素
            getView().findViewById(R.id.answers).setVisibility(View.VISIBLE);

            if (result.isFinished()) {
                ((MainActivity) getActivity()).switchToFinishFragment(BdcFragment.this);
                return;
            } else if (result.isNoWord()) {
                ((MainActivity) getActivity()).switchToSelectBookFragment(BdcFragment.this);
                return;
            } else if (result.getShouldEnterReviewMode()) {
                ((MainActivity) getActivity()).switchToStageReviewFragment(BdcFragment.this);
                return;
            }

            currentGetWordResult = result;
            isWordMastered = false;

            //进度条
            MyProgress myProgress = (MyProgress) getView().findViewById(R.id.progress);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, Util.dp2px(12, BdcFragment.this.getActivity()));
            myProgress.setLayoutParams(lp);
            myProgress.setTextSize(24);
            myProgress.setPrecision(0);
            int[] progress = result.getProgress();
            myProgress.setMax(progress[1]);
            myProgress.setProgress(progress[0]);

            //单词掌握度
            MyProgress masterGrade = (MyProgress) getView().findViewById(R.id.masterGrade);
            lp = new LinearLayout.LayoutParams(
                    Util.dp2px(120, BdcFragment.this.getActivity()), Util.dp2px(12, BdcFragment.this.getActivity()));
            masterGrade.setLayoutParams(lp);
            masterGrade.setTextSize(24);
            masterGrade.setShowProgressValue(false);
            masterGrade.setPrecision(0);
            masterGrade.setMax(5);
            masterGrade.setProgress(5 - result.getLearningWord().getLifeValue());
            TextView nextStudyDay = (TextView) getView().findViewById(R.id.nextStudyDay);
            int nextDay = Util.calcuNextStudyDayByLifeValue(result.getLearningWord().getLifeValue());
            nextStudyDay.setText(nextDay > 0 ? (nextDay + "天后再次学习") : "今后不再学习");

            //如果单词已经标记为掌握，跳过这个单词
            if (currentGetWordResult.getLearningWord().getLifeValue() == 0) {
                isAnswerCorrect = true;
                loadAWord();
                return;
            }

            isShowingAWord = true;

            //显示单词拼写及音标
            WordVo word = currentGetWordResult.getLearningWord().getWord();
            currentSpell = word.getSpell();
            ((TextView) getView().findViewById(R.id.spell)).setText(currentSpell);
            String pronounce = Util.getWordDefaultPronounce(word);
            ((TextView) getView().findViewById(R.id.pronounce)).setText(StringUtils.isEmpty(pronounce) ? "" : String.format("/ %s /", pronounce));

            //显示单词发音按钮
            ImageView btn = (ImageView) getView().findViewById(R.id.playPronounce);
            btn.setVisibility(View.VISIBLE);

            //显示单词已掌握按钮
            btn = (ImageView) getView().findViewById(R.id.wordMastered);
            btn.setVisibility(View.VISIBLE);

            //根据单词的学习模式显示相应内容
            int learningMode = result.getLearningMode();
            List<SentenceVo> sentences = word.getSentences();
            if (learningMode == 0 && (sentences.size() == 0)) {//如果没有例句则只好使用拼写
                learningMode = 1;
            }
            if (learningMode == 0 && Util.isPhrase(currentSpell)) {//如果是短语则只好使用拼写（因为短语在例句中往往没有直接的匹配）
                learningMode = 1;
            }
            if (learningMode == 0) {//根据句子
                getView().findViewById(R.id.spell).setVisibility(View.GONE);
                getView().findViewById(R.id.pronounce).setVisibility(View.GONE);
                String sentence = sentences.get(0).getEnglish();
                currentEnglishDigest = sentences.get(0).getEnglishDigest();
                TextView sentenceView = (TextView) getView().findViewById(R.id.sentence);

                Util.makeClickableEnglishView(sentenceView, sentence, mediaPlayer, getString(R.string.sound_base_url), (MyActivity) BdcFragment.this.getActivity(), currentSpell, -1, -1);
                sentenceView.setVisibility(View.VISIBLE);

                //显示“是否自动播放句子发音”的标志按钮
                btn = (ImageView) getView().findViewById(R.id.autoPlaySentenceFlag);
                btn.setVisibility(View.VISIBLE);
                boolean isAutoPlaySentence = Util.isAutoPlaySentence(BdcFragment.this.getActivity());
                btn.setImageResource(isAutoPlaySentence ? R.drawable.right_arrow_green : R.drawable.right_arrow_gray);

                //显示播放句子发音按钮
                btn = (ImageView) getView().findViewById(R.id.playSentence);
                btn.setVisibility(View.VISIBLE);

                //下载单词发音并自动发音, 如果“是否自动播放句子发音”的标志为true，则播放完单词发音之后，再自动播放句子发音
                MediaPlayer.OnCompletionListener listener = null;
                if (isAutoPlaySentence) {
                    listener = new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            Util.downloadSentenceSoundAndPlay(currentEnglishDigest, BdcFragment.this, mediaPlayer, null);
                        }
                    };
                }
                Util.downloadPronounceAndPlay(currentSpell, listener, mediaPlayer, getString(R.string.sound_base_url));
            } else if (learningMode == 1) {//根据拼写
                getView().findViewById(R.id.sentence).setVisibility(View.GONE);
                getView().findViewById(R.id.spell).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.pronounce).setVisibility(View.VISIBLE);

                //下载单词发音并自动发音
                Util.downloadPronounceAndPlay(currentSpell, null, mediaPlayer, getString(R.string.sound_base_url));
            } else if (learningMode == 2) {//根据发音
                getView().findViewById(R.id.sentence).setVisibility(View.GONE);
                getView().findViewById(R.id.spell).setVisibility(View.GONE);
                getView().findViewById(R.id.pronounce).setVisibility(View.GONE);
                TextView hintView = (TextView) getView().findViewById(R.id.hint);
                hintView.setText("请根据发音选择正确答案");
                hintView.setVisibility(View.VISIBLE);

                //拼写练习
                ((EditText) getView().findViewById(R.id.edtSpell)).setText("");
                getView().findViewById(R.id.spellExercise).setVisibility(View.VISIBLE);

                //绑定拼写编辑框输入事件处理函数
                EditText spellView = (EditText) getView().findViewById(R.id.edtSpell);
                SpellChangeWatcher watcher = new SpellChangeWatcher(spellView);
                spellView.addTextChangedListener(watcher);

                //下载单词发音并自动发音
                Util.downloadPronounceAndPlay(currentSpell, null, mediaPlayer, getString(R.string.sound_base_url));
            }

            //为正确答案随机选择一个索引号（1～3）
            correctAnswerIndex = (int) Math.ceil((3.0 * Math.random()));
            if (correctAnswerIndex == 0) {
                correctAnswerIndex = 1;
            }
            if (correctAnswerIndex == 4) {
                correctAnswerIndex = 3;
            }

            //显示备选答案
            findAnswerBtnByIndex(correctAnswerIndex).setText(Util.makeWordMeaningStr(word, 70));
            WordVo[] otherWords = result.getOtherWords();
            int index = 1;
            for (int i = 0; i <= 1; i++) {
                if (index == correctAnswerIndex) {
                    index++;
                }
                findAnswerBtnByIndex(index).setText(Util.makeWordMeaningStr(otherWords[i], 70));
                index++;
            }
            findAnswerBtnByIndex(5).setText("不认识");
            findAnswerBtnByIndex(6).setText("认识，再学学");

            // 英文短描述 (这个信息在查看单词详情时也会渲染，但因为需要提前看到，所以在这里提前渲染)
            TextView shortDescView = (TextView) getView().findViewById(R.id.shortDesc);
            Util.makeClickableEnglishView(shortDescView, word.getShortDesc(), mediaPlayer, getString(R.string.sound_base_url), (MyActivity) BdcFragment.this.getActivity(), currentSpell, Color.parseColor("#777777"), Color.parseColor("#999999"));
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            loadAWordTask = null;
            ((MyActivity) getActivity()).showProgress(false);
        }
    }

    /**
     * 拼写编辑框内容变化的监听器
     */
    private class SpellChangeWatcher implements TextWatcher {
        /**
         * 被监听的目标编辑框
         */
        private EditText spellText;

        private SpellChangeWatcher(EditText spellText) {
            this.spellText = spellText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String actual = s.toString();
            if (actual.equalsIgnoreCase(currentSpell)) { //拼写输入正确
                //把文字变成绿色
                spellText.setTextColor(Color.parseColor("#00BB00"));

                //播放单词发音
                Util.downloadPronounceAndPlay(currentSpell, null, mediaPlayer, getString(R.string.sound_base_url));

                //隐藏输入法
                ImmUtils.closeImmWindow(getActivity(), spellText);
            } else {
                spellText.setTextColor(getResources().getColor(R.color.currentWord));
            }
        }
    }

    /**
     * 接续上次学习的中断点
     */
    public class ContinueAtLastBreakPointTask extends MyAsyncTask<Void, Void, JSONObject> {
        protected ContinueAtLastBreakPointTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                String serviceUrl = getString(R.string.service_url) + "/continueAtTheLastBreakPoint.do";
                HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(serviceUrl, null, "GET", 5000);
                return new JSONObject(result);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final JSONObject result) {
            super.onPostExecute(result);
            if (getView() == null || getActivity() == null) return;
            if (result == null) {
                ToastUtil.showToast(getActivity(), "系统异常");
                return;
            }
            continueAtLastBreakPointTask = null;
            loadAWord();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            continueAtLastBreakPointTask = null;
        }
    }

    private void displayWordDetail() throws JSONException {
        //隐藏/显示某些界面元素
        View rootView = getView();
        rootView.findViewById(R.id.spell).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.pronounce).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.nextWordBtn).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.sentencesLabel).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.sentences).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.meaningsLabel).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.meanings).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.sentence).setVisibility(View.GONE);
        rootView.findViewById(R.id.answers).setVisibility(View.GONE);
        rootView.findViewById(R.id.hint).setVisibility(View.GONE);
        rootView.findViewById(R.id.autoPlaySentenceFlag).setVisibility(View.GONE);
        rootView.findViewById(R.id.playSentence).setVisibility(View.GONE);

        WordVo word = currentGetWordResult.getLearningWord().getWord();
        word.setSentences(currentGetWordResult.getSentences());
        WordDetailUtil.renderWordDetail(word, this, mediaPlayer);


    }


    public void answerBtnClicked(View src) throws JSONException {
        int selectedAnswerIndex = Integer.parseInt((String) src.getTag());
        isAnswerCorrect = selectedAnswerIndex == correctAnswerIndex;
        if (isAnswerCorrect) {
            loadAWord();
        } else if (selectedAnswerIndex == 6) {//认识，再学学
            isAnswerCorrect = true;
            displayWordDetail();
        } else {//不认识或答案错误
            displayWordDetail();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.selectDict:
                ((MainActivity) getActivity()).switchToSelectBookFragment(BdcFragment.this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 切换是否自动播放句子发音的标志
     */
    public void switchAutoPlaySentenceFlag() {
        switchAutoPlaySentenceFlagTask = new SwitchAutoPlaySentenceFlagTask(getMainActivity());
        switchAutoPlaySentenceFlagTask.execute((Void) null);
    }

    public class SwitchAutoPlaySentenceFlagTask extends MyAsyncTask<Void, Void, Void> {

        protected SwitchAutoPlaySentenceFlagTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            String serviceUrl = getString(R.string.service_url) + "/switchAutoPlaySentenceFlag.do";
            HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
            try {
                httpClient.sendAjax(serviceUrl, null, "POST", 5000);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (getView() == null || getActivity() == null) return;
            switchAutoPlaySentenceFlagTask = null;

            //更新本地存储
            boolean autoPlaySentence;
            try {
                UserVo user = Util.getCachedLoggedInUser(getMainActivity());
                autoPlaySentence = !user.getAutoPlaySentence();
                user.setAutoPlaySentence(autoPlaySentence);
                Util.saveLoggedInUserToCache(user, getMainActivity());

            } catch (JSONException e) {
                autoPlaySentence = false;
                e.printStackTrace();
            }


            //切换句子自动发音模式后，重新加载
            ((MainActivity) getActivity()).switchBdcFragment(BdcFragment.this, null, true);
            ToastUtil.showToast(getActivity(), autoPlaySentence ? "句子自动发音" : "句子手动发音");
        }

    }

    /**
     * 播放单词发音按钮处理函数
     */
    public void playPronounce() {
        //下载单词发音并自动发音
        Util.downloadPronounceAndPlay(currentSpell, null, mediaPlayer, getString(R.string.sound_base_url));
    }

    /**
     * 播放句子发音按钮处理函数
     */
    public void playSentence() {
        Util.downloadSentenceSoundAndPlay(currentEnglishDigest, this, mediaPlayer, null);
    }
}