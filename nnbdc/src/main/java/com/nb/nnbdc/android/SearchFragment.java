package com.nb.nnbdc.android;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.ImmUtils;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;
import com.nb.nnbdc.android.util.WordDetailUtil;
import com.nb.nnbdc.android.util.WordStore;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import beidanci.vo.SearchWordResult;
import beidanci.vo.WordVo;


public class SearchFragment extends MyFragment {
    private MediaPlayer mediaPlayer = new MediaPlayer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //触发加载本地词库
        WordStore.getInstance(getMainActivity());

        //绑定搜索编辑框的文本变化事件处理
        EditText editSpell = (EditText) getView().findViewById(R.id.editSpell);
        editSpell.addTextChangedListener(new SpellChangeWatcher(editSpell));

        //绑定搜索按钮的点击事件处理
        View btnSearch = getView().findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPreciseSearch();
            }
        });

        doFuzzySearch();
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

    /**
     * 搜索编辑框内容变化的监听器
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
            doFuzzySearch();
        }
    }

    private void doFuzzySearch() {
        showWordDetailScrollView(false);
        showFuzzyWordsScrollView(true);

        //清空现有搜索结果
        LinearLayout wordsLayout = (LinearLayout) getView().findViewById(R.id.words);
        wordsLayout.removeAllViews();

        //渲染模糊匹配的搜索结果
        WordStore wordStore = WordStore.getInstance(getMainActivity());
        for (final WordVo wordVO : wordStore.searchWord(((EditText) getView().findViewById(R.id.editSpell)).getText().toString())) {
            LinearLayout wordLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.search_word_item, null);
            ((TextView) wordLayout.findViewById(R.id.spell)).setText(wordVO.getSpell());
            ((TextView) wordLayout.findViewById(R.id.meaningStr)).setText(wordVO.getMeaningStr());
            wordsLayout.addView(wordLayout);

            //当单词被点击时，进行精确查询，相当于点击了查询按钮
            wordLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText spellView = (EditText) getView().findViewById(R.id.editSpell);
                    spellView.setText(wordVO.getSpell());
                    doPreciseSearch();
                }
            });
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        EditText spellView = (EditText) getView().findViewById(R.id.editSpell);
        if (hidden) {
            spellView.clearFocus();
            closeImmWindow();
        } else {
            spellView.setText(""); //重置搜索内容
            spellView.requestFocus();
            ImmUtils.openImmWindow(getActivity(), getView().findViewById(R.id.editSpell));

        }
    }

    private void doPreciseSearch() {
        String spell = ((EditText) getView().findViewById(R.id.editSpell)).getText().toString();
        SearchWordTask task = new SearchWordTask(spell);
        task.execute((Void) null);
    }

    private class SearchWordTask extends AsyncTask<Void, Void, SearchWordResult> {
        private String spell;

        public SearchWordTask(String spell) {
            this.spell = spell;
        }

        @Override
        protected SearchWordResult doInBackground(Void... params) {
            try {
                return getHttpClient().searchWord(spell);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(final SearchWordResult searchWordResult) {
            super.onPostExecute(searchWordResult);
            if (getView() == null || getActivity() == null) return;
            if (searchWordResult == null) {
                ToastUtil.showToast(getActivity(), "系统异常");
                return;
            }
            if (searchWordResult.getWord() == null) {
                ToastUtil.showToast(getActivity(), "查不到该单词");
                return;
            }
            try {
                //渲染单词和音标
                TextView view = (TextView) getView().findViewById(R.id.wordSpell);
                view.setText(searchWordResult.getWord().getSpell());
                view = (TextView) getView().findViewById(R.id.pronounce);
                view.setText(Util.makePronounceStr(Util.getWordDefaultPronounce(searchWordResult.getWord())));

                //单词发音按钮
                getView().findViewById(R.id.playPronounce).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Util.downloadPronounceAndPlay(searchWordResult.getWord().getSpell(), null, mediaPlayer, getString(R.string.sound_base_url));
                    }
                });
                Util.downloadPronounceAndPlay(searchWordResult.getWord().getSpell(), null, mediaPlayer, getString(R.string.sound_base_url));

                //加入生词本按钮
                ImageView addRawWordBtn = (ImageView) getView().findViewById(R.id.addRawWordBtn);
                addRawWordBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Util.addRawWord(searchWordResult.getWord().getSpell(), getMainActivity());
                    }
                });

                //渲染释义、例句等
                WordDetailUtil.renderWordDetail(searchWordResult.getWord(), SearchFragment.this, mediaPlayer);
                showFuzzyWordsScrollView(false);
                showWordDetailScrollView(true);
                closeImmWindow();
            } catch (JSONException e) {
                ToastUtil.showToast(getMainActivity(), e.getMessage());
            }
        }
    }

    private void closeImmWindow() {
        ImmUtils.closeImmWindow(getActivity(), getView().findViewById(R.id.editSpell));
    }

    private void showFuzzyWordsScrollView(boolean show) {
        View view = getView().findViewById(R.id.fuzzyWordsScrollView);
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showWordDetailScrollView(boolean show) {
        View view = getView().findViewById(R.id.wordDetailScrollView);
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
