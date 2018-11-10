package com.nb.nnbdc.android.task;

import com.nb.nnbdc.android.HttpClient;
import com.nb.nnbdc.android.MyActivity;
import com.nb.nnbdc.android.MyAsyncTask;

import beidanci.vo.SearchWordResult;

/**
 * 向服务端发送查询指定单词的请求，当收到服务端的查询结果后，调用指定的监听器。
 */
public class SearchWordTask extends MyAsyncTask<Void, Void, SearchWordResult> {
    private HttpClient httpClient;
    private String spell;
    SearchWordListener searchWordListener;

    public SearchWordTask(MyActivity activity, String spell, HttpClient httpClient, SearchWordListener searchWordListener) {
        super(activity);
        this.httpClient = httpClient;
        this.spell = spell;
        this.searchWordListener = searchWordListener;
    }

    @Override
    protected SearchWordResult doInBackground(Void... params) {
        try {
            return httpClient.searchWord(spell);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    protected void onPostExecute(final SearchWordResult result) {
        super.onPostExecute(result);
        searchWordListener.onComplete(result);
    }

    public interface SearchWordListener {
        void onComplete(SearchWordResult searchResult);
    }
}
