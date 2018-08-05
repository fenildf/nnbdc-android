package com.nb.nnbdc.android.dlg;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.MyActivity;
import com.nb.nnbdc.android.util.Util;

import java.util.List;

import beidanci.vo.MeaningItemVo;
import beidanci.vo.SearchWordResult;

/**
 * Created by Administrator on 2016/5/15.
 */
public class SearchWordResultDialog extends AlertDialog {
    private SearchWordResult searchWordResult;
    private MyActivity activity;

    public SearchWordResultDialog(Context context, int theme, SearchWordResult searchWordResult) {
        super(context, theme);
        this.activity = (MyActivity) context;
        this.searchWordResult = searchWordResult;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_search_result);
        fillWithSearchResult();

        // 添加到生词本按钮
        ImageView addRawWordBtn = (ImageView) findViewById(R.id.addRawWordBtn);
        addRawWordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.addRawWord(searchWordResult.getWord().getSpell(), activity);
            }
        });

        // 关闭按钮
        ImageView closeBtn = (ImageView) findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });


    }


    /**
     * 用单词查询结果填充界面
     */
    private void fillWithSearchResult() {
        //单词拼写和音标
        TextView spellView = (TextView) findViewById(R.id.spell);
        spellView.setText(searchWordResult.getWord().getSpell());
        TextView pronounceView = (TextView) findViewById(R.id.pronounce);
        String pronounce = searchWordResult.getWord().getAmericaPronounce();
        pronounceView.setText(pronounce == null || pronounce.length() == 0 ? "" : "/" + pronounce + "/");

        //单词释义
        LinearLayout meaningsLayout = (LinearLayout) findViewById(R.id.meanings);
        meaningsLayout.removeAllViews();
        List<MeaningItemVo> meaningItems = searchWordResult.getWord().getMeaningItems();
        for (int i = 0; i < meaningItems.size(); i++) {
            MeaningItemVo meaningItem = meaningItems.get(i);

            //容器layout
            LinearLayout meaningItemLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.meaning_item, null);
            meaningsLayout.addView(meaningItemLayout);
            meaningItemLayout.setId(Util.generateViewId());

            //词性
            TextView ciXingView = (TextView) meaningItemLayout.getChildAt(0);
            ciXingView.setText(meaningItem.getCiXing());
            ciXingView.setId(Util.generateViewId());

            //意思
            TextView meaningView = (TextView) meaningItemLayout.getChildAt(1);
            meaningView.setText(meaningItem.getMeaning());
            meaningView.setId(Util.generateViewId());
        }
    }


}
