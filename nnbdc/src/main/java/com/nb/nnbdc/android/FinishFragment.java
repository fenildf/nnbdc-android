package com.nb.nnbdc.android;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.task.GetLoggedInUserTask;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import beidanci.vo.Result;
import beidanci.vo.UserVo;

public class FinishFragment extends MyFragment {
    private DakaTask dakaTask;
    private ThrowDiceAndSaveTask throwDiceAndSaveTask;
    private GetWrongWordsTask getWrongWordsTask;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_finish, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getView().findViewById(R.id.returnBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToDefaultPage();
            }
        });
        getView().findViewById(R.id.exitBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });

        //打卡
        daka();
    }

    /**
     * 获取在学习过程中回答错误的单词
     */
    public class GetWrongWordsTask extends MyAsyncTask<Void, Void, JSONArray> {
        protected GetWrongWordsTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected JSONArray doInBackground(Void... params) {
            try {

                //获取在学习过程中回答错误的单词
                HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(getString(R.string.service_url) + "/getAnswerWrongWords.do", null, "GET", 5000);
                JSONArray wrongWords = new JSONArray(result);

                return wrongWords;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray wrongWords) {
            super.onPostExecute(wrongWords);
            if (getView() == null || getActivity() == null) return;
            if (wrongWords == null) {
                ToastUtil.showToast(getActivity(), "系统异常");
                return;
            }
            LinearLayout wordsLayout = (LinearLayout) getView().findViewById(R.id.wordsLayout);
            wordsLayout.removeAllViews();
            for (int i = 0; i < wrongWords.length(); i++) {
                try {
                    final JSONObject word = wrongWords.getJSONObject(i);
                    final String spell = word.getString("spell");

                    //容器layout
                    LinearLayout reivewItemLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.review_item, null);
                    wordsLayout.addView(reivewItemLayout);
                    reivewItemLayout.setId(Util.generateViewId());

                    //释义
                    TextView meaningView = (TextView) reivewItemLayout.getChildAt(0);
                    meaningView.setId(Util.generateViewId());
                    setupMeaningTextView(meaningView, word, false, null);

                    //拼写
                    EditText spellView = (EditText) reivewItemLayout.getChildAt(1);
                    spellView.setText(spell);
                    spellView.setId(Util.generateViewId());
                    spellView.setTag(meaningView); //与释义文本框绑定（释义文本框记录下了正确的拼写）
                    spellView.setEnabled(false); //只有当点击了“拼写”按钮之后才允许输入
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            getWrongWordsTask = null;
        }
    }

    private void setupMeaningTextView(TextView meaningView, JSONObject word, boolean makeHintBtn, ClickableSpan hintBtnHandler) throws JSONException {
        meaningView.setText(Util.makeMeaningItemsStr(word.getJSONArray("meaningItems")) + " ");
        final String spell = word.getString("spell");
        meaningView.setId(Util.generateViewId());

        //发音按钮
        SpannableString spanString = Util.makeImageBtnSpanString(FinishFragment.this.getActivity(), R.drawable.speaker, new ClickableSpan() {
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
            }

            @Override
            public void onClick(View widget) {
                Util.downloadPronounceAndPlay(spell, null, mediaPlayer, getString(R.string.sound_base_url));
            }
        });
        meaningView.append(spanString);

        //拼写提示按钮
        if (makeHintBtn) {
            spanString = Util.makeImageBtnSpanString(FinishFragment.this.getActivity(), R.drawable.tip, hintBtnHandler);
            meaningView.append("    ");
            meaningView.append(spanString);
        }

        meaningView.setMovementMethod(LinkMovementMethod.getInstance());//开始响应点击事件

        //点击文字也能发音
        meaningView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.downloadPronounceAndPlay(spell, null, mediaPlayer, getString(R.string.sound_base_url));
            }
        });
    }

    /**
     * 打卡
     */
    private void daka() {
        dakaTask = new DakaTask(getMainActivity());
        dakaTask.execute((Void) null);
    }

    /**
     * 打卡
     */
    public class DakaTask extends MyAsyncTask<Void, Void, Result> {
        protected DakaTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected Result doInBackground(Void... params) {
            try {
                //打卡
                return getHttpClient().saveDakaRecord();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Result result) {
            super.onPostExecute(result);
            if (getView() == null || getActivity() == null) return;
            if (result == null) {
                showToast("系统异常");
                return;
            }

            final TextView cowdungText = (TextView) getView().findViewById(R.id.cowDungText);
            if (result.isSuccess()) {
                //显示打卡后积分相关信息
                GetLoggedInUserTask.getLoggedInUser(getMainActivity(), new GetLoggedInUserTask.CallBack() {
                    @Override
                    public void onSuccess(UserVo user) {
                        Integer dakaScore = (Integer) result.getData();
                        cowdungText.append(String.format("获得打卡积分：%d\n已连续打卡%d天，明天继续打卡，将获得%d点积分加成",
                                dakaScore, user.getContinuousDakaDayCount(), user.getContinuousDakaDayCount()));
                    }

                    @Override
                    public void onFailed() {

                    }
                });


                //掷骰子
                throwDiceAndSaveTask = new ThrowDiceAndSaveTask(getMainActivity());
                throwDiceAndSaveTask.execute((Void) null);
            } else {
                cowdungText.setText(result.getMsg());
                showWrongWords();
            }

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            dakaTask = null;
        }
    }

    /**
     * 掷骰子
     */
    public class ThrowDiceAndSaveTask extends MyAsyncTask<Void, Void, JSONObject> {
        protected ThrowDiceAndSaveTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                //掷骰子
                HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(getString(R.string.service_url) + "/throwDiceAndSave.do", null, "GET", 5000);
                return new JSONObject(result);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (getView() == null || getActivity() == null) return;
            super.onPostExecute(result);
            if (result == null) {
                showToast("系统异常");
                return;
            }

            try {
                if (result.getBoolean("success")) {
                    int cowdung = result.getInt("data");
                    TextView cowdungText = (TextView) getView().findViewById(R.id.cowDungText);
                    if (cowdung != 20) {
                        cowdungText.append(String.format("\n\n恭喜！你得到%d个牛粪", cowdung));
                    } else {
                        cowdungText.append(String.format("\n\n运气太好了！你得到20个牛粪！翻倍，你实际得到40个牛粪！"));

                        //播放欢呼声
                        MediaPlayer mediaPlayer = MediaPlayer.create(getActivity(), R.raw.cheer);// 得到声音资源
                        mediaPlayer.start();// 播放声音
                    }

                    showWrongWords();
                } else {
                    ToastUtil.showToast(getActivity(), result.getString("msg"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            dakaTask = null;
        }
    }

    private void showWrongWords() {
        getWrongWordsTask = new GetWrongWordsTask(getMainActivity());
        getWrongWordsTask.execute((Void) null);
    }


    /**
     * 返回到默认页面
     */
    public void returnToDefaultPage() {
        ((MainActivity) getActivity()).switchToMeFragment();
    }

    /**
     * 退出程序
     */
    public void exit() {
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
        System.exit(0);
    }
}
