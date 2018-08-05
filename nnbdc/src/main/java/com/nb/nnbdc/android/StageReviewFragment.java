package com.nb.nnbdc.android;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StageReviewFragment extends MyFragment {
    private GetReviewWordsTask getReviewWordsTask;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    /**
     * 保存动态创建的单词拼写编辑框
     */
    private List<EditText> spellViews = new ArrayList<>();

    /**
     * 保存动态创建的单词释义文本框
     */
    private List<TextView> meaningViews = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stage_review, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getReviewWordsTask = new GetReviewWordsTask();
        getReviewWordsTask.execute((Void) null);

        getView().findViewById(R.id.startSpellExerciseBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpellExercise();
            }
        });

        getView().findViewById(R.id.continueBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                continueStudy();
            }
        });
    }

    /**
     * 获取本阶段待复习单词
     */
    public class GetReviewWordsTask extends AsyncTask<Void, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(Void... params) {
            try {
                //获取用户选择的所有单词书
                HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(getString(R.string.service_url) + "/getCurrentStageCache.do", null, "GET", 5000);
                JSONArray leaningWords = new JSONArray(result);

                return leaningWords;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray leaningWords) {
            if (getView() == null || getActivity() == null) return;
            if (leaningWords == null) {
                ToastUtil.showToast(getActivity(), "系统异常");
                return;
            }
            LinearLayout reviewLayout = (LinearLayout) getView().findViewById(R.id.reviewLayout);
            reviewLayout.removeAllViews();
            spellViews.clear();
            for (int i = 0; i < leaningWords.length(); i++) {
                try {
                    final JSONObject word = leaningWords.getJSONObject(i).getJSONObject("word");
                    final String spell = word.getString("spell");

                    //容器layout
                    LinearLayout reivewItemLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.review_item, null);
                    reviewLayout.addView(reivewItemLayout);
                    reivewItemLayout.setId(Util.generateViewId());

                    //释义
                    TextView meaningView = (TextView) reivewItemLayout.getChildAt(0);
                    meaningView.setId(Util.generateViewId());
                    setupMeaningTextView(meaningView, word, false, null);
                    meaningView.setTag(word);
                    meaningViews.add(meaningView);

                    //拼写
                    EditText spellView = (EditText) reivewItemLayout.getChildAt(1);
                    spellView.setText(spell);
                    spellView.setId(Util.generateViewId());
                    spellView.setTag(meaningView); //与释义文本框绑定（释义文本框记录下了正确的拼写）
                    spellView.setEnabled(false); //只有当点击了“拼写”按钮之后才允许输入
                    spellViews.add(spellView);

                    //绑定拼写编辑框输入事件处理函数
                    SpellChangeWatcher watcher = new SpellChangeWatcher(spellView);
                    spellView.addTextChangedListener(watcher);

                    //绑定拼写编辑框获得焦点事件处理函数
                    spellView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(final View v, boolean hasFocus) {
                            try {
                                if (hasFocus) {
                                    //去掉其他单词的拼写提示按钮
                                    for (TextView meaningView : meaningViews) {
                                        setupMeaningTextView(meaningView, (JSONObject) meaningView.getTag(), false, null);
                                    }

                                    //为本编辑框对应的释义文本框添加提示按钮
                                    final TextView meaningView = (TextView) v.getTag();
                                    setupMeaningTextView(meaningView, (JSONObject) meaningView.getTag(), true, new ClickableSpan() {
                                        @Override
                                        public void onClick(View widget) {
                                            //显示正确的拼写
                                            EditText spellView = (EditText) v;
                                            try {
                                                spellView.setText(((JSONObject) meaningView.getTag()).getString("spell"));
                                                spellView.setTextColor(Color.parseColor("#5555ff"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onCancelled() {
            getReviewWordsTask = null;
        }
    }

    private void setupMeaningTextView(TextView meaningView, JSONObject word, boolean makeHintBtn, ClickableSpan hintBtnHandler) throws JSONException {
        meaningView.setText(Util.makeMeaningItemsStr(word.getJSONArray("meaningItems")) + " ");
        final String spell = word.getString("spell");
        meaningView.setId(Util.generateViewId());

        //发音按钮
        SpannableString spanString = Util.makeImageBtnSpanString(StageReviewFragment.this.getActivity(), R.drawable.speaker, new ClickableSpan() {
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
            spanString = Util.makeImageBtnSpanString(StageReviewFragment.this.getActivity(), R.drawable.tip, hintBtnHandler);
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
            String expect = null;
            try {
                expect = ((JSONObject) ((TextView) spellText.getTag()).getTag()).getString("spell");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (actual.equalsIgnoreCase(expect)) { //拼写输入正确
                //把文字变成绿色
                spellText.setTextColor(Color.parseColor("#00BB00"));

                //播放单词发音
                Util.downloadPronounceAndPlay(expect, null, mediaPlayer, getString(R.string.sound_base_url));

                //跳到下一个编辑框
                int currIndex = getIndexOfSpellEditText(spellText);
                if (currIndex < 9) {
                    EditText next = spellViews.get(currIndex + 1);
                    next.requestFocus();
                }
            }
        }
    }

    /**
     * 获取指定的拼写编辑框在所有编辑框中的索引
     *
     * @param editText
     * @return
     */
    private int getIndexOfSpellEditText(EditText editText) {
        for (int i = 0; i < spellViews.size(); i++) {
            if (spellViews.get(i) == editText) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 开始拼写练习
     */
    public void startSpellExercise() {
        //清空所有拼写编辑框的内容，并使编辑框变为可编辑
        for (EditText spellText : spellViews) {
            spellText.setText("");
            spellText.setEnabled(true);
        }

        //把屏幕滚动到最上方
        ScrollView scrollView = (ScrollView) getView().findViewById(R.id.scrollView);
        scrollView.scrollTo(0, 0);

        //把光标定位到第一个拼写编辑框
        spellViews.get(0).requestFocus();
    }

    /**
     * 继续学习
     */
    public void continueStudy() {
        ((MainActivity) getActivity()).switchBdcFragment(StageReviewFragment.class.getSimpleName(), true);
    }
}
