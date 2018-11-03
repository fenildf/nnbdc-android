package com.nb.nnbdc.android.util;

import android.content.DialogInterface;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.MyActivity;
import com.nb.nnbdc.android.MyAsyncTask;
import com.nb.nnbdc.android.MyFragment;
import com.nb.nnbdc.android.dlg.InputSentenceChineseDialog;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import beidanci.vo.MeaningItemVo;
import beidanci.vo.Result;
import beidanci.vo.SentenceDiyItemVo;
import beidanci.vo.SentenceVo;
import beidanci.vo.WordVo;

/**
 * Created by Administrator on 2016/8/7.
 */
public class WordDetailUtil {
    private static class DeleteUgcChineseTask extends MyAsyncTask<Void, Void, Result> {
        private int sentenceId;
        private int ugcChineseId;
        private SentenceVo sentence;
        private MyFragment fragment;

        public DeleteUgcChineseTask(int sentenceId, int ugcChineseId, MyFragment myFragment) {
            super(myFragment.getMainActivity());
            this.sentenceId = sentenceId;
            this.ugcChineseId = ugcChineseId;
            this.fragment = myFragment;
        }

        @Override
        protected Result doInBackground(Void... params) {
            Result result = null;
            try {
                result = fragment.getHttpClient().deleteSentenceUgcChinese(ugcChineseId);
                sentence = fragment.getHttpClient().getSentence(sentenceId);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            if (fragment.getView() == null || fragment.getActivity() == null) return;
            if (result == null) {
                ToastUtil.showToast(fragment.getActivity(), "系统异常");
                return;
            }
            if (sentence != null) {
                LinearLayout chineseContainer = getChineseContainerLayout(sentenceId, fragment);
                try {
                    refreshSentenceUgcChineses(sentence, chineseContainer, fragment);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static LinearLayout getChineseContainerLayout(int sentenceId, MyFragment fragment) {
        LinearLayout sentencesLayout = (LinearLayout) fragment.getView().findViewById(R.id.sentences);
        LinearLayout sentenceLayout = (LinearLayout) sentencesLayout.findViewWithTag(sentenceId);
        LinearLayout englishAndChineseLayout = (LinearLayout) sentenceLayout.getChildAt(1);
        LinearLayout chineseContainer = (LinearLayout) englishAndChineseLayout.findViewById(R.id.chineseContainer);
        return chineseContainer;
    }

    private static class HandOrFootUgcChineseTask extends MyAsyncTask<Void, Void, Result> {
        private int sentenceId;
        private int ugcChineseId;
        private SentenceVo sentence;
        private String handOrFoot;
        private MyFragment fragment;

        public HandOrFootUgcChineseTask(int sentenceId, int ugcChineseId, String handOrFoot, MyFragment myFragment) {
            super(myFragment.getMainActivity());
            this.sentenceId = sentenceId;
            this.ugcChineseId = ugcChineseId;
            this.handOrFoot = handOrFoot;
            this.fragment = myFragment;
        }

        @Override
        protected Result doInBackground(Void... params) {
            Result result = null;
            try {
                result = handOrFoot.equals("hand") ? fragment.getHttpClient().handSentenceUgcChinese(ugcChineseId) : fragment.getHttpClient().footSentenceUgcChinese(ugcChineseId);
                sentence = fragment.getHttpClient().getSentence(sentenceId);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            if (fragment.getView() == null || fragment.getActivity() == null) return;
            if (result == null) {
                ToastUtil.showToast(fragment.getActivity(), "系统异常");
                return;
            }
            if (sentence != null) {
                LinearLayout chineseContainer = getChineseContainerLayout(sentenceId, fragment);
                try {
                    refreshSentenceUgcChineses(sentence, chineseContainer, fragment);
                } catch (JSONException e) {
                    ToastUtil.showToast(fragment.getActivity(), "系统异常:" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }


    private static void refreshSentenceUgcChineses(final SentenceVo sentence, LinearLayout chineseContainer, final MyFragment fragment) throws JSONException {
        chineseContainer.removeAllViews();
        for (final SentenceDiyItemVo diyItem : sentence.getSentenceDiyItems()) {
            LinearLayout ugcChineseItemLayout = (LinearLayout) fragment.getActivity().getLayoutInflater().inflate(R.layout.ugc_chinese_item, null);
            chineseContainer.addView(ugcChineseItemLayout);

            //UGC中文翻译
            TextView chineseView = (TextView) ugcChineseItemLayout.findViewById(R.id.chinese);
            chineseView.setText(diyItem.getContent());
            chineseView.setTextColor(fragment.getResources().getColor(R.color.dimTextColor));

            //作者
            TextView authorView = (TextView) ugcChineseItemLayout.findViewById(R.id.author);
            authorView.setText("(" + diyItem.getAuthor().getDisplayNickName() + ")");
            authorView.setTextColor(fragment.getResources().getColor(R.color.moreDimTextColor));

            //赞
            ImageView handImgView = (ImageView) ugcChineseItemLayout.findViewById(R.id.handImg);
            handImgView.setImageResource(R.drawable.hand);
            if (diyItem.getHasBeenVoted()) {
                handImgView.setAlpha(0.2f); //图片变灰
            } else {
                handImgView.setAlpha(0.5f); //图片变亮
            }
            TextView handCountView = (TextView) ugcChineseItemLayout.findViewById(R.id.handCount);
            handCountView.setText(String.valueOf(diyItem.getHandCount()));
            handCountView.setTextColor(fragment.getResources().getColor(R.color.moreDimTextColor));
            if (!diyItem.getHasBeenVoted()) {
                View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HandOrFootUgcChineseTask task = new HandOrFootUgcChineseTask(sentence.getId(), diyItem.getId(), "hand", fragment);
                        task.execute((Void) null);
                    }
                };
                handImgView.setOnClickListener(listener);
                handCountView.setOnClickListener(listener);
            }

            //踩
            ImageView footImgView = (ImageView) ugcChineseItemLayout.findViewById(R.id.footImg);
            footImgView.setImageResource(R.drawable.foot);
            if (diyItem.getHasBeenVoted()) {
                footImgView.setAlpha(0.2f); //图片变灰
            } else {
                footImgView.setAlpha(0.5f); //图片变亮
            }

            TextView footCountView = (TextView) ugcChineseItemLayout.findViewById(R.id.footCount);
            footCountView.setText(String.valueOf(diyItem.getFootCount()));
            footCountView.setTextColor(fragment.getResources().getColor(R.color.moreDimTextColor));
            if (!diyItem.getHasBeenVoted()) {
                View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HandOrFootUgcChineseTask task = new HandOrFootUgcChineseTask(sentence.getId(), diyItem.getId(), "foot", fragment);
                        task.execute((Void) null);
                    }
                };
                footImgView.setOnClickListener(listener);
                footCountView.setOnClickListener(listener);
            }

            //删除
            if (diyItem.getAuthor().equals(Util.getCachedLoggedInUser(fragment.getActivity()))) {
                ImageView deleteImgView = (ImageView) ugcChineseItemLayout.findViewById(R.id.deleteImg);
                deleteImgView.setImageResource(R.drawable.delete);
                deleteImgView.setAlpha(0.5f);
                View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DeleteUgcChineseTask task = new DeleteUgcChineseTask(sentence.getId(), diyItem.getId(), fragment);
                        task.execute((Void) null);
                    }
                };
                deleteImgView.setOnClickListener(listener);
            }
        }
    }

    public static void renderWordDetail(final WordVo word, final MyFragment fragment, final MediaPlayer mediaPlayer) throws JSONException {
        //单词释义
        LinearLayout meaningsLayout = (LinearLayout) fragment.getView().findViewById(R.id.meanings);
        meaningsLayout.removeAllViews();
        List<MeaningItemVo> meaningItems = word.getMeaningItems();
        for (int i = 0; i < meaningItems.size(); i++) {
            MeaningItemVo meaningItem = meaningItems.get(i);

            //容器layout
            LinearLayout meaningItemLayout = (LinearLayout) fragment.getActivity().getLayoutInflater().inflate(R.layout.meaning_item, null);
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

        // 英文短描述
        TextView shortDescView = (TextView)fragment.getView(). findViewById(R.id.shortDesc);
        Util.makeClickableEnglishView(shortDescView, word.getShortDesc(), mediaPlayer, fragment.getString(R.string.sound_base_url), (MyActivity) fragment.getActivity(), word.getSpell(), Color.parseColor("#777777"), Color.parseColor("#999999"));

        //单词例句
        LinearLayout sentencesLayout = (LinearLayout) fragment.getView().findViewById(R.id.sentences);
        sentencesLayout.removeAllViews();
        List<SentenceVo> sentences = word.getSentences();
        for (int i = 0; i < sentences.size(); i++) {
            final SentenceVo sentence = sentences.get(i);

            //整个例句容器layout
            LinearLayout sentenceLayout = (LinearLayout) fragment.getActivity().getLayoutInflater().inflate(R.layout.sentence, null);
            sentenceLayout.setTag(sentence.getId());
            sentencesLayout.addView(sentenceLayout);
            sentenceLayout.setId(Util.generateViewId());

            //序号
            TextView serialNoView = (TextView) sentenceLayout.getChildAt(0);
            serialNoView.setId(Util.generateViewId());
            serialNoView.setText(String.valueOf(i + 1) + ".");

            //英文及意思容器
            final LinearLayout englishAndChineseLayout = (LinearLayout) sentenceLayout.getChildAt(1);
            englishAndChineseLayout.setId(Util.generateViewId());

            //渲染英文及发音按钮
            TextView englishView = (TextView) englishAndChineseLayout.getChildAt(0);
            englishView.setId(Util.generateViewId());
            Editable content = Util.spanEnglishContent(sentence.getEnglish(), mediaPlayer, fragment.getString(R.string.sound_base_url), (MyActivity) fragment.getActivity(), word.getSpell(), -1, -1);

            //发音按钮(真人发音才显示该按钮)
            if (sentence.getTheType().equals("human_audio")) {
                final String englishDigest = sentence.getEnglishDigest();
                SpannableString spanString = Util.makeImageBtnSpanString(fragment.getActivity(), R.drawable.speaker2, new TouchableSpan(Color.GREEN, Color.YELLOW, Color.BLUE) {
                    @Override
                    public void updateDrawState(TextPaint ds) {
                        super.updateDrawState(ds);
                    }

                    @Override
                    public void onClick(View widget) {
                        Util.downloadSentenceSoundAndPlay(englishDigest, fragment, mediaPlayer, null);
                    }
                });

                content.append(spanString);
            }

            //添加UGC翻译按钮
            if (sentence.getChinese() == null) { //该例句没有官方翻译
                final String englishDigest = sentence.getEnglishDigest();
                SpannableString spanString = Util.makeImageBtnSpanString(fragment.getActivity(), R.drawable.add, new TouchableSpan(Color.GREEN, Color.YELLOW, Color.BLUE) {
                    @Override
                    public void updateDrawState(TextPaint ds) {
                        super.updateDrawState(ds);
                    }

                    @Override
                    public void onClick(View widget) {
                        final InputSentenceChineseDialog inputDialog = new InputSentenceChineseDialog(fragment.getActivity(), R.style.dialog, sentence);//创建Dialog并设置样式主题
                        inputDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                LinearLayout chineseContainer = (LinearLayout) englishAndChineseLayout.findViewById(R.id.chineseContainer);
                                try {
                                    SentenceVo sentence2 = inputDialog.getSentence();
                                    refreshSentenceUgcChineses(sentence2, chineseContainer, fragment);
                                } catch (Exception e) {
                                    ToastUtil.showToast(fragment.getActivity(), "系统异常：" + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
                        inputDialog.show();

                    }
                });

                content.append("  ");
                content.append(spanString);
            }

            englishView.setText(content, TextView.BufferType.SPANNABLE);
            englishView.setMovementMethod(new LinkTouchMovementMethod());//开始响应点击事件
            englishView.setOnLongClickListener(new View.OnLongClickListener() { //设置一个长按事件监听器，防止崩溃
                @Override
                public boolean onLongClick(View v) {
                    return true; //消费长按事件
                }
            });

            //意思
            LinearLayout chineseContainer = (LinearLayout) englishAndChineseLayout.findViewById(R.id.chineseContainer);
            if (sentence.getChinese() != null) { //例句自带官方中文翻译
                TextView chineseView = new TextView(fragment.getActivity());
                chineseView.setText(sentence.getChinese());
                chineseView.setTextColor(fragment.getResources().getColor(R.color.dimTextColor));
                chineseContainer.addView(chineseView);
            } else { //例句没有官方翻译，使用UGC翻译
                refreshSentenceUgcChineses(sentence, chineseContainer, fragment);
            }
        }
    }
}
