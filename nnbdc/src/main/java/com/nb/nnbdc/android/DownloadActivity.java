package com.nb.nnbdc.android;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.task.DownloadAWordTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 下载用户选择的所有单词书中的单词声音资源
 */
public class DownloadActivity extends MyActivity {
    private GetAllWordsTask getAllWordsTask;

    /**
     * 所有待下载的单词
     */
    private Map<String, JSONObject> words = new HashMap<>();

    /**
     * 所有待下载的单词的迭代器
     */
    private Iterator<String> wordIterator;


    private int succCount = 0;

    private int failedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        getAllWordsTask = new GetAllWordsTask();
        getAllWordsTask.execute((Void) null);
    }

    /**
     * 获取所有待下载单词
     */
    public class GetAllWordsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                //获取用户选择的所有单词书
                HttpClient httpClient = ((MyApp) getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(getString(R.string.service_url) + "/getSelectedLearningDicts.do", null, "GET", 5000);
                JSONArray selectedDicts = new JSONArray(result);

                //获取用户选择的所有单词书中的单词
                for (int i = 0; i < selectedDicts.length(); i++) {
                    String dictName = selectedDicts.getJSONObject(i).getJSONObject("dict").getString("name");
                    Map<String, Object> pars = new HashMap<>();
                    pars.put("dictName", dictName);
                    result = httpClient.sendAjax(getString(R.string.service_url) + "/getWordsOfDict.do", pars, "GET", 5000);
                    JSONArray wordsOfDict = new JSONArray(result);
                    for (int j = 0; j < wordsOfDict.length(); j++) {
                        JSONObject word = wordsOfDict.getJSONObject(j);
                        words.put(word.getString("spell"), word);
                    }
                }
                wordIterator = words.keySet().iterator();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            getAllWordsTask = null;
            getProgressBar().setMax(words.size());
            try {
                downloadNextWord();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onCancelled() {
            getAllWordsTask = null;
        }
    }


    private void downloadNextWord() throws JSONException {
        if (!wordIterator.hasNext()) { //下载完成
            finish();
            Intent intent = new Intent(DownloadActivity.this, MeFragment.class);
            startActivity(intent);
            return;
        }

        String spell = wordIterator.next();
        JSONObject wordInfo = words.get(spell);

        List<String> sentenceSounds = new ArrayList<>();
        for (int i = 0; i < wordInfo.getJSONArray("sentenceMP3s").length(); i++) {
            String englishDigest = wordInfo.getJSONArray("sentenceMP3s").getString(i);
            sentenceSounds.add(englishDigest);
        }

        DownloadAWordTask getAWordTask = new DownloadAWordTask(spell, sentenceSounds, getString(R.string.sound_base_url), new DownloadAWordTask.Callback() {
            @Override
            public void onDownloaded(boolean succ) {
                if (succ) {
                    succCount++;
                } else {
                    failedCount++;
                }
                ((TextView) findViewById(R.id.progressText)).setText(String.format("正在下载声音文件(成功%d 失败%d)...", succCount, failedCount));
                getProgressBar().setProgress(getProgressBar().getProgress() + 1);
                try {
                    downloadNextWord();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        getAWordTask.execute((Void) null);
    }


}

