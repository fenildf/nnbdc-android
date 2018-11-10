package com.nb.nnbdc.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.nb.nnbdc.R;
import com.nb.nnbdc.android.MyActivity;
import com.nb.nnbdc.android.MyFragment;
import com.nb.nnbdc.android.dlg.SearchWordResultDialog;
import com.nb.nnbdc.android.task.AddRawWordTask;
import com.nb.nnbdc.android.task.DownloadAWordTask;
import com.nb.nnbdc.android.task.SearchWordTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import beidanci.util.Utils;
import beidanci.vo.GetWordResult;
import beidanci.vo.Result;
import beidanci.vo.SearchWordResult;
import beidanci.vo.UserVo;
import beidanci.vo.WordVo;


public class Util {
    /**
     * 本类专用于在android屏幕上显示弹出式提示信息
     *
     * @author Administrator
     */
    public static class HintHandler extends Handler {
        private WeakReference<Context> context;

        public HintHandler(Context activity) {
            context = new WeakReference<Context>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Context ctx = context.get();
            if (ctx != null) {
                Toast.makeText(ctx, msg.getData().getString("hint"), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 本类专用于跳到登录页面，重新登录
     *
     * @author Administrator
     */
    public static class ReLoginHandler extends Handler {
        private WeakReference<Context> context;

        public ReLoginHandler(MyActivity activity) {
            context = new WeakReference<Context>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MyActivity ctx = (MyActivity) context.get();
            if (ctx != null) {
                ctx.onReLoginRequired();
            }
        }
    }


    public static void showHintMsg(String hintMsg, HintHandler handler) {
        Message msg = Message.obtain();
        msg.setTarget(handler);
        Bundle bundle = new Bundle();
        bundle.putString("hint", hintMsg);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    public static void reLogin(ReLoginHandler handler) {
        Message msg = Message.obtain();
        msg.setTarget(handler);
        msg.sendToTarget();
    }

    public static boolean isMobileNO(String str) {
        Pattern p = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0-9]))\\d{8}$");
        Matcher m = p.matcher(str);
        return m.matches();
    }


    public static void setListViewHeightBasedOnChildren(ListView listView) {
        // 获取ListView对应的Adapter
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;

        for (int i = 0, len = listAdapter.getCount(); i < len; i++) { // listAdapter.getCount()返回数据项的数目
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0); // 计算子项View 的宽高
            totalHeight += listItem.getMeasuredHeight(); // 统计所有子项的总高度
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        // listView.getDividerHeight()获取子项间分隔符占用的高度
        // params.height最后得到整个ListView完整显示需要的高度
        listView.setLayoutParams(params);

    }

    public static boolean isStringEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static int dp2px(int dp, Context context) {
        final float scale = context.getResources().getDisplayMetrics().density;
        int px = (int) (dp * scale + 0.5f);
        return px;
    }

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * Generate an id value suitable for dynamically created view
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int generateViewId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            for (; ; ) {
                final int result = sNextGeneratedId.get();
                // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
                int newValue = result + 1;
                if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result;
                }
            }
        } else {
            return View.generateViewId();
        }

    }

    public static boolean isPhrase(String spell) {
        return spell.trim().indexOf(" ") != -1;
    }

    public static String encodeUrl(String url) {
        String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
        String urlEncoded = Uri.encode(url, ALLOWED_URI_CHARS);
        return urlEncoded;
    }

    /**
     * 下载指定的资源，并保存到本地指定的文件，如果本地目标文件已经存在，则不会重新下载
     *
     * @param fileUrl
     * @param localFileName
     * @return
     */
    public static boolean downloadFile(String fileUrl, String localFileName, boolean forceDownload) {
        File file = new File(localFileName);
        //如果目标文件已经存在，则忽略，不用重新下载
        if (file.exists() && !forceDownload) {
            return true;
        }
        try {
            // 构造URL
            String urlEncoded = encodeUrl(fileUrl);
            URL url = new URL(urlEncoded);
            // 打开连接
            URLConnection con = url.openConnection();
            con.setConnectTimeout(5000);
            // 输入流
            InputStream is = con.getInputStream();
            // 1K的数据缓冲
            byte[] bs = new byte[1024];
            // 读取到的数据长度
            int len;
            // 输出的文件流
            createDirIfNotExists(localFileName);
            OutputStream os = new FileOutputStream(localFileName);
            // 开始读取
            while ((len = is.read(bs)) != -1) {
                os.write(bs, 0, len);
            }
            // 完毕，关闭所有链接
            os.close();
            is.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 如果文件所在目录不存在，则自动创建
     *
     * @param filePathName
     */
    public static void createDirIfNotExists(String filePathName) {
        int index = filePathName.lastIndexOf("/");
        String dir = filePathName.substring(0, index);
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static String getFileNameOfWordSound(String spell) {
        spell = uniformSpellForFilename(spell);
        char firstChar = spell.substring(0, 1).toCharArray()[0];

        if (firstChar >= 'a' && firstChar <= 'z') {
            return firstChar + "/" + spell;
        } else {
            return "other" + "/" + spell;
        }
    }

    /**
     * 某些文件命中含有单词拼写（如单词的声音文件，例句声音文件），所以需要对单词的一些特殊字符做处理
     *
     * @param spell
     * @return
     */
    public static String uniformSpellForFilename(String spell) {
        spell = spell.replaceAll("\\?", "").toLowerCase();
        spell = uniformString(spell);
        return spell;
    }

    /**
     * 清楚字符串中多余的空格及制表符、回车等
     *
     * @return
     */
    public static String uniformString(String str) {
        str = str.replaceAll("\t", " ");
        str = str.replaceAll("\n", " ");
        str = replaceDoubleSpace(str);
        return str.trim();
    }

    public static String replaceDoubleSpace(String str) {
        while (str.indexOf("  ") != -1) {
            str = str.replaceAll("  ", " ");
        }
        return str;
    }

    /**
     * 关键字高亮显示
     *
     * @param target 需要高亮的关键字
     * @param text   需要显示的文字
     * @return spannable 处理完后的结果，记得不要toString()，否则没有效果
     */
    public static SpannableStringBuilder highlight(String text, String target) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(text);
        CharacterStyle span = null;

        Pattern p = Pattern.compile(target);
        Matcher m = p.matcher(text);
        while (m.find()) {
            span = new ForegroundColorSpan(Color.parseColor("#F2CA27"));
            spannable.setSpan(span, m.start(), m.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    public static void saveLoggedInUserToCache(UserVo user, Context context) {
        SharedPreferences settings = context.getSharedPreferences("loggedInUser", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("json", Util.toJson(user));
        editor.commit();
    }


    public static UserVo getCachedLoggedInUser(Context context) throws JSONException {
        SharedPreferences settings = context.getSharedPreferences("loggedInUser", Activity.MODE_PRIVATE);
        UserVo user = Util.getGsonBuilder().create().fromJson(settings.getString("json", null), UserVo.class);
        return user;
    }

    /**
     * 读取配置 -- 是否自动播放句子发音？
     */
    public static boolean isAutoPlaySentence(Context context) {
        try {
            UserVo user = getCachedLoggedInUser(context);
            return user.getAutoPlaySentence();
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据值, 设置spinner默认选中:
     *
     * @param spinner
     * @param value
     */
    public static void setSpinnerItemSelectedByValue(Spinner spinner, String value) {
        SpinnerAdapter apsAdapter = spinner.getAdapter(); //得到SpinnerAdapter对象
        int k = apsAdapter.getCount();
        for (int i = 0; i < k; i++) {
            if (value.equals(apsAdapter.getItem(i).toString())) {
                spinner.setSelection(i, true);// 默认选中项
                break;
            }
        }
    }

    /**
     * 把单词释义数组转换称一个字符串
     *
     * @param meaningItems
     * @return
     */
    public static String makeMeaningItemsStr(JSONArray meaningItems) throws JSONException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < meaningItems.length(); i++) {
            JSONObject meaningItem = meaningItems.getJSONObject(i);
            sb.append(StringUtils.isEmpty(meaningItem.getString("ciXing")) ? "" : meaningItem.getString("ciXing"));
            sb.append(meaningItem.getString("meaning"));
            if (i != meaningItems.length() - 1) {
                sb.append("；");
            }
        }
        return sb.toString();
    }

    /**
     * 下载单词发音并自动发音
     *
     * @param spell
     */
    public static void downloadPronounceAndPlay(final String spell, final MediaPlayer.OnCompletionListener completionListener/*单词发音播放完毕的监听器*/,
                                                final MediaPlayer mediaPlayer, String soundBaseUrl) {
        DownloadAWordTask task = new DownloadAWordTask(spell, null, soundBaseUrl, new DownloadAWordTask.Callback() {
            @Override
            public void onDownloaded(boolean succ) throws IOException {
                playLocalSound(mediaPlayer, completionListener, DownloadAWordTask.getPronounceFileOfWord(spell));
            }
        });
        task.execute((Void) null);
    }

    public static void playLocalSound(MediaPlayer mediaPlayer, MediaPlayer.OnCompletionListener completionListener, String pronounceFileOfWord) throws IOException {
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.setDataSource(pronounceFileOfWord);
        mediaPlayer.setOnCompletionListener(completionListener);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    public static void playSoundByResId(final int soundId, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediaPlayer mediaPlayer = MediaPlayer.create(activity, soundId);// 得到声音资源
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        mediaPlayer.release();
                    }
                });
                mediaPlayer.start();
            }
        });
    }

    public static SpannableString makeImageBtnSpanString(Context ctx, int imageResId, ClickableSpan clickableSpan) {
        Bitmap bitmap = BitmapFactory.decodeResource(ctx.getResources(), imageResId);
        ImageSpan imgSpan = new ImageSpan(ctx, bitmap);
        SpannableString spanString = new SpannableString("icon");
        spanString.setSpan(imgSpan, 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); //用图片替换文字内容（"icon"）
        spanString.setSpan(clickableSpan, 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanString;
    }

    public static void makeClickableEnglishView(TextView view, String english, final MediaPlayer mediaPlayer, final String soundBaseUrl, final MyActivity context, final String highlightWord, int defaultColor, int highlightColor) {
        if (english == null) {
            view.setText("");
        } else {
            Editable content = Util.spanEnglishContent(english, mediaPlayer, soundBaseUrl, (MyActivity) context, highlightWord, defaultColor, highlightColor);
            view.setText(content);
            view.setMovementMethod(new LinkTouchMovementMethod());//开始响应点击事件
            view.setOnLongClickListener(new View.OnLongClickListener() { //设置一个长按事件监听器，防止崩溃
                @Override
                public boolean onLongClick(View v) {
                    return true; //消费长按事件
                }
            });
        }
    }

    public static Editable spanEnglishContent(String english, final MediaPlayer mediaPlayer, final String soundBaseUrl, final MyActivity context, final String highlightWord, int defaultColor, int highlightColor) {
        final Editable result = new SpannableStringBuilder();
        String[] words = english.split(" ");
        for (final String word : words) {
            //使单词可点击
            SpannableString spanString = new SpannableString(word + " ");
            TouchableSpan clickableSpan = new TouchableSpan(defaultColor == -1 ? Color.parseColor("#aaaaaa") : defaultColor, Color.BLACK, Color.parseColor("#335555")) {

                @Override
                public void onClick(final View widget) {
                    ///确定被点击的span在屏幕上的位置 ----开始

                    final TextView parentTextView = (TextView) widget;
                    Rect parentTextViewRect = new Rect();

                    // Initialize values for the computing of clickedText position
                    final SpannableString completeText = (SpannableString) (parentTextView).getText();
                    Layout textViewLayout = parentTextView.getLayout();

                    double startOffsetOfClickedText = completeText.getSpanStart(this);
                    double endOffsetOfClickedText = completeText.getSpanEnd(this);
                    double startXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal((int) startOffsetOfClickedText);
                    double endXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal((int) endOffsetOfClickedText);

                    // Get the rectangle of the clicked text
                    int currentLineStartOffset = textViewLayout.getLineForOffset((int) startOffsetOfClickedText);
                    int currentLineEndOffset = textViewLayout.getLineForOffset((int) endOffsetOfClickedText);
                    boolean keywordIsInMultiLine = currentLineStartOffset != currentLineEndOffset;
                    textViewLayout.getLineBounds(currentLineStartOffset, parentTextViewRect);

                    // Update the rectangle position to his real position on screen
                    int[] parentTextViewLocation = {0, 0};
                    parentTextView.getLocationOnScreen(parentTextViewLocation);

                    double parentTextViewTopAndBottomOffset = (
                            parentTextViewLocation[1] -
                                    parentTextView.getScrollY() +
                                    parentTextView.getCompoundPaddingTop()
                    );
                    parentTextViewRect.top += parentTextViewTopAndBottomOffset;
                    parentTextViewRect.bottom += parentTextViewTopAndBottomOffset;

                    parentTextViewRect.left += (
                            parentTextViewLocation[0] +
                                    startXCoordinatesOfClickedText +
                                    parentTextView.getCompoundPaddingLeft() -
                                    parentTextView.getScrollX()
                    );
                    parentTextViewRect.right = (int) (
                            parentTextViewRect.left +
                                    endXCoordinatesOfClickedText -
                                    startXCoordinatesOfClickedText
                    );

                    int x_ = (parentTextViewRect.left + parentTextViewRect.right) / 2;
                    final int y = parentTextViewRect.bottom;
                    if (keywordIsInMultiLine) {
                        x_ = parentTextViewRect.left;
                    }
                    final int x = x_;

                    ///确定被点击的span在屏幕上的位置 ----结束

                    //查询并显示单词详细信息
                    SearchWordTask task = new SearchWordTask(context, word, context.getHttpClient(), new SearchWordTask.SearchWordListener() {
                        @Override
                        public void onComplete(SearchWordResult searchResult) {
                            if (searchResult == null) {
                                ToastUtil.showToast(context, "系统异常");
                            } else if (searchResult.getWord() != null) {
                                SearchWordResultDialog searchWordResultDialog = new SearchWordResultDialog(context, R.style.dialog, searchResult);//创建Dialog并设置样式主题
                                WindowManager.LayoutParams wmlp = searchWordResultDialog.getWindow().getAttributes();
                                int[] screenLocation = new int[2];
                                widget.getLocationOnScreen(screenLocation);
                                wmlp.gravity = Gravity.TOP | Gravity.LEFT;
                                wmlp.x = x;
                                wmlp.y = y - 50;

                                //当单词信息对话框关闭时，清除当前单词的高亮
                                searchWordResultDialog.setCanceledOnTouchOutside(true);//设置点击Dialog外部任意区域关闭Dialog
                                searchWordResultDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        Selection.removeSelection(completeText);
                                    }
                                });
                                searchWordResultDialog.show();
                                Util.downloadPronounceAndPlay(searchResult.getWord().getSpell(), null, mediaPlayer, soundBaseUrl);
                            } else {
                                ToastUtil.showToast(context, "查不到单词：" + word);
                            }
                        }
                    });
                    task.execute((Void) null);
                }
            };
            spanString.setSpan(clickableSpan, 0, word.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            //如果单词需要高亮，高亮之
            List<String> allPossibleForms = Utils.getAllPossibleFormsOfWord(highlightWord);
            for (String spell : allPossibleForms) {
                if (Utils.purifySpell(word).equalsIgnoreCase(spell)) {
                    CharacterStyle span = new ForegroundColorSpan(highlightColor == -1 ? Color.parseColor("#F2CA27") : highlightColor);
                    spanString.setSpan(span, 0, word.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                }
            }


            result.append(spanString);
        }
        return result;
    }

    /**
     * 生成指定范围内的随机整数
     *
     * @param min
     * @param max
     * @return
     */
    public static int genRandomNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt(max) % (max - min + 1) + min;
    }

    public static void main(String[] agrs) {
        for (int i = 0; i < 100; i++) {
            System.out.println(genRandomNumber(1, 20));
        }
    }

    public static int getVerCode(Context context) {
        int verCode = -1;
        try {
            verCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verCode;
    }

    public static String getVerName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

    public static GsonBuilder getGsonBuilder() {// Creates the json object which will manage the information received
        GsonBuilder builder = new GsonBuilder();

        // Register an adapter to manage the date types as long values
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });

        builder.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(Date date, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(date.getTime());
            }
        });
        return builder;
    }

    /**
     * 生产单词释义字符串，如果单词释义太长，则舍弃一部分，否则界面拥挤
     *
     * @param word
     * @param maxLen 允许的单词释义最大长度，超出此长度的内容会被舍弃
     * @return
     */
    public static String makeWordMeaningStr(WordVo word, int maxLen) {
        String meaningStr = word.getMeaningStr();
        String[] parts = meaningStr.split("；");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part).append("；");
            if (sb.toString().length() >= maxLen) {
                break;
            }
        }

        String str = sb.toString();
        if (str.length() > 0) {
            str = str.substring(0, str.length() - 1);
        }

        return str;
    }

    public static <T> Result<T> parseResult(String result) {
        Type objectType = new TypeToken<Result<T>>() {
        }.getType();
        result = preHandleResult(result);
        return Util.getGsonBuilder().create().fromJson(result, objectType);
    }

    public static String preHandleResult(String result) {
        return result == null ? null : result.replace("\"success\"", "\"isSuccess\"");
    }

    ;

    public static String getWordDefaultPronounce(WordVo word) {
        String pronounce = word.getPronounce();
        if (StringUtils.isEmpty(pronounce)) {
            pronounce = word.getAmericaPronounce();
        }
        if (StringUtils.isEmpty(pronounce)) {
            pronounce = word.getBritishPronounce();
        }
        return pronounce;
    }

    /**
     * 下载例句发音并自动播放
     *
     * @param englishDigest
     */
    public static void downloadSentenceSoundAndPlay(final String englishDigest, MyFragment fragment, final MediaPlayer mediaPlayer, final MediaPlayer.OnCompletionListener completionListener/*发音播放完毕的监听器*/) {
        List<String> sentences = new ArrayList<>();
        sentences.add(englishDigest);
        DownloadAWordTask task = new DownloadAWordTask(null, sentences, fragment.getString(R.string.sound_base_url), new DownloadAWordTask.Callback() {
            @Override
            public void onDownloaded(boolean succ) throws IOException {
                playLocalSound(mediaPlayer, completionListener, DownloadAWordTask.getSentenceSoundFile(englishDigest));
            }
        });
        task.execute((Void) null);
    }

    public static String makePronounceStr(String pronounce) {
        return StringUtils.isEmpty(pronounce) ? "" : "/" + pronounce + "/";
    }

    public static void addRawWord(String spell, MyActivity activity) {
        AddRawWordTask task = new AddRawWordTask(spell, activity);
        task.execute((Void) null);
    }

    /**
     * 根据单词的生命值计算下次学习该单词应在多少天之后
     *
     * @param lifeValue
     * @return
     */
    public static int calcuNextStudyDayByLifeValue(int lifeValue) {
        int nextDay = 0;
        if (lifeValue == 5) {
            nextDay = 1;
        } else if (lifeValue == 4) {
            nextDay = 2;
        } else if (lifeValue == 3) {
            nextDay = 3;
        } else if (lifeValue == 2) {
            nextDay = 8;
        }
        return nextDay;
    }

    public static String toJson(Object obj) {
        return Util.getGsonBuilder().create().toJson(obj);
    }
}
