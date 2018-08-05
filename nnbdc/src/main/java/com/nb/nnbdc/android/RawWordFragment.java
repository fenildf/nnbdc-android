package com.nb.nnbdc.android;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.MyScrollView;
import com.nb.nnbdc.android.util.StringUtils;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;

import beidanci.vo.PagedData;
import beidanci.vo.PagedResults;
import beidanci.vo.RawWordVo;
import beidanci.vo.Result;


public class RawWordFragment extends MyFragment {
    private LoadRawWordTask loadRawWordTask;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private int pageSize = 10;
    private int pageIndex = 1;
    private boolean isQuerying = false;
    private boolean lastPageReached = false;

    /**
     * 最后一个已加载的分页
     */
    private long lastLoadedPageIndex = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_raw_word, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        doQuery(true);
        setupWordsScrollView();
    }


    public class DeleteRawWordTask extends MyAsyncTask<Void, Void, Result> {
        private int wordWordId;
        private LinearLayout rawWordLayout;

        public DeleteRawWordTask(int wordWordId, LinearLayout rawWordLayout, MyActivity myActivity) {
            super(myActivity);
            this.wordWordId = wordWordId;
            this.rawWordLayout = rawWordLayout;
        }

        @Override
        protected Result doInBackground(Void... params) {
            try {
                return getHttpClient().deleteRawWord(wordWordId);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Result ajaxResult) {
            super.onPostExecute(ajaxResult);
            if (getView() == null || getActivity() == null) return;
            if (ajaxResult == null) {
                ToastUtil.showToast(getActivity(), "系统异常");
                return;
            }

            if (ajaxResult.isSuccess()) {
                ((LinearLayout) rawWordLayout.getParent()).removeView(rawWordLayout);
            } else {
                ToastUtil.showToast(getActivity(), ajaxResult.getMsg());
            }
        }
    }


    public class LoadRawWordTask extends MyAsyncTask<Void, Void, PagedResults<RawWordVo>> {
        protected LoadRawWordTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected PagedResults<RawWordVo> doInBackground(Void... params) {
            try {
                return getHttpClient().getRawWordsForAPage(pageIndex++, pageSize);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(PagedResults<RawWordVo> result) {
            super.onPostExecute(result);
            isQuerying = false;
            if (getView() == null || getActivity() == null) return;
            if (result == null) {
                ToastUtil.showToast(getActivity(), "系统异常");
                return;
            }

            //渲染每一个生词
            if (getActivity() == null)
                return;
            LinearLayout wordsListLayout = (LinearLayout) getActivity().findViewById(R.id.wordsList);
            for (RawWordVo rawWordVO : result.getRows()) {
                renderARawWord(wordsListLayout, rawWordVO);
            }

            //显示滚动状态
            TextView scrollStatus = (TextView) getView().findViewById(R.id.scrollStatus);
            if (result.getTotal() <= pageSize * (pageIndex - 1)) {
                scrollStatus.setText("已到最后一页");
                scrollStatus.setBackgroundColor(Color.parseColor("#404040"));
                lastPageReached = true;
            } else {
                scrollStatus.setText("正在加载下一页...");
                scrollStatus.setBackgroundColor(Color.parseColor("#004400"));
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            loadRawWordTask = null;
        }
    }

    private void renderARawWord(LinearLayout wordsListLayout, final RawWordVo rawWord) {
        //容器layout
        final LinearLayout rawWordLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.raw_word_item, null);
        wordsListLayout.addView(rawWordLayout);

        //单词拼写
        TextView view = (TextView) rawWordLayout.findViewById(R.id.spell);
        view.setText(rawWord.getWord().getSpell());
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //下载单词发音并自动发音
                Util.downloadPronounceAndPlay(rawWord.getWord().getSpell(), null, mediaPlayer, getString(R.string.sound_base_url));
            }
        };
        view.setOnClickListener(clickListener);

        //音标
        view = (TextView) rawWordLayout.findViewById(R.id.pronounce);
        String pronounce = Util.getWordDefaultPronounce(rawWord.getWord());
        view.setText(StringUtils.isEmpty(pronounce) ? "" : "/" + pronounce + "/");
        view.setOnClickListener(clickListener);

        //单词释义
        view = (TextView) rawWordLayout.findViewById(R.id.meaningStr);
        view.setText(rawWord.getWord().getMeaningStr());
        view.setOnClickListener(clickListener);

        //删除按钮
        Button btnDelete = (Button) rawWordLayout.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteRawWordTask task = new DeleteRawWordTask(rawWord.getId(), rawWordLayout, getMainActivity());
                task.execute((Void) null);
            }
        });
    }

    private boolean isScrollAtBottom() {
        final ScrollView view = (ScrollView) getActivity().findViewById(R.id.rawWordsScrollView);
        return view.getChildAt(0).getMeasuredHeight() <= view.getHeight() + view.getScrollY();
    }


    /**
     * 设置滚动事件处理
     */
    private void setupWordsScrollView() {
        //在滚动事件发生时，检测是否滚动到了底部
        final MyScrollView view = (MyScrollView) getActivity().findViewById(R.id.rawWordsScrollView);

        view.setOnScrollListener(new MyScrollView.OnScrollListener() {
            @Override
            public void onScroll(int scrollY) {
                if (isScrollAtBottom()) {
                    doQuery(false);
                }
            }
        });
    }

    private void clearQueryResult() {
        //清空当前查询结果
        LinearLayout wordsListLayout = (LinearLayout) getView().findViewById(R.id.wordsList);
        wordsListLayout.removeAllViews();

        pageIndex = 1;
        lastPageReached = false;
    }

    /**
     * 根据用户输入的条件查询商品
     */
    private void doQuery(boolean clearCurrent) {
        if (isQuerying) {
            return;
        }

        //清除当前的查询结果
        if (clearCurrent) {
            clearQueryResult();
        }

        //查询
        if (!lastPageReached) {
            isQuerying = true;
            loadRawWordTask = new LoadRawWordTask(getMainActivity());
            loadRawWordTask.execute((Void) null);
        }
    }
}
