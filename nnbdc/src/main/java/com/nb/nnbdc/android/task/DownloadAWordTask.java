package com.nb.nnbdc.android.task;

/**
 * Created by Administrator on 2015/12/25.
 */

import android.os.AsyncTask;
import android.os.Environment;

import com.nb.nnbdc.android.util.Util;

import java.io.IOException;
import java.util.List;

/**
 * 下载一个单词相关的资源文件
 */
public class DownloadAWordTask extends AsyncTask<Void, Void, Boolean/*是否下载成功, 返回null表示所有单词都已下载完成*/> {
    /**
     * 要下载的单词
     */
    private String spell;

    /**
     * 要下载的例句声音文件
     */
    private List<String> sentenceSounds;

    /**
     * 下载完成后的回调
     */
    private Callback callback;

    /**
     * 牛牛服务器声音文件的根URL
     */
    private String soundBaseUrl;

    /**
     * 存放单词相关资源文件的根路径
     */
    private final static String localBasePath = Environment.getExternalStorageDirectory() + "/nnbdc/sound/";

    /**
     * 获取指定的单词对应的发音文件
     */
    public static String getPronounceFileOfWord(String spell) {
        return localBasePath + Util.getFileNameOfWordSound(spell) + ".mp3";
    }

    /**
     * 获取指定的例句对应的发音文件
     */
    public static String getSentenceSoundFile(String englishDigest) {
        return localBasePath + "sentence/" + englishDigest + ".mp3";
    }

    public DownloadAWordTask(String spell, List<String> sentenceSounds, String soundBaseUrl, Callback callback) {
        this.spell = spell;
        this.sentenceSounds = sentenceSounds;
        this.soundBaseUrl = soundBaseUrl;
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        //下载发音文件
        if (spell != null) {
            Util.downloadFile(soundBaseUrl + Util.getFileNameOfWordSound(spell) + ".mp3", localBasePath + Util.getFileNameOfWordSound(spell) + ".mp3", false, null);
        }

        if (sentenceSounds != null) {
            for (int i = 0; i < sentenceSounds.size(); i++) {
                String englishDigest = sentenceSounds.get(i);
                Util.downloadFile(soundBaseUrl + "sentence/" + englishDigest + ".mp3", localBasePath + "sentence/" + englishDigest + ".mp3", false, null);
            }
        }
        return true;
    }


    @Override
    protected void onPostExecute(Boolean succ) {
        if (callback != null) {
            try {
                callback.onDownloaded(succ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCancelled() {
    }

    public interface Callback {
        void onDownloaded(boolean succ) throws IOException;
    }
}
