package com.nb.nnbdc.android.util;

import android.os.Process;

import com.google.gson.reflect.TypeToken;
import com.nb.nnbdc.R;
import com.nb.nnbdc.android.MyActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import beidanci.vo.WordVo;

/**
 * Created by Administrator on 2016/8/6.
 */
public class WordStore {
    private List<WordVo> allWords = new ArrayList<>();

    private WordStore() {
    }

    private static WordStore instance = null;

    public static WordStore getInstance(final MyActivity myActivity) {
        if (instance == null) {
            synchronized (WordStore.class) {
                if (instance == null) {
                    WordStore store = new WordStore();
                    try {
                        store.load(myActivity);
                        instance = store;
                    } catch (IOException e) {
                        ToastUtil.showToast(myActivity, "加载本地词库失败");
                    }
                }
            }
        }
        return instance;
    }

    private void load(final MyActivity myActivity) throws IOException {
        //在一个线程中加载词，避免对界面的阻塞
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    //得到资源中的Raw数据流
                    InputStream in = myActivity.getResources().openRawResource(R.raw.words);
                    InputStreamReader inputStreamReader = new InputStreamReader(in);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    //解析json为单词列表
                    Type objectType = new TypeToken<List<WordVo>>() {
                    }.getType();
                    allWords = Util.getGsonBuilder().create().fromJson(bufferedReader, objectType);
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    public List<WordVo> searchWord(String spell) {
        List<WordVo> words = new ArrayList<>();
        int count = 0;
        for (WordVo wordVO : allWords) {
            if (wordVO.getSpell().toLowerCase().startsWith(spell.toLowerCase())) {
                words.add(wordVO);
                count++;
                if (count >= 50) {
                    break;
                }
            }
        }
        return words;
    }
}
