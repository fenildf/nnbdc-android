package com.nb.nnbdc.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BeforeBdcFragment extends MyFragment {
    private PrepareTask prepareTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_before_bdc, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //准备今日要学习的单词
        prepareTask = new PrepareTask(getMainActivity());
        prepareTask.execute((Void) null);

        getView().findViewById(R.id.startBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStudy();
            }
        });
    }

    /**
     * 准备今天要学习的单词
     */
    public class PrepareTask extends MyAsyncTask<Void, Void, JSONArray> {
        protected PrepareTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected JSONArray doInBackground(Void... params) {
            try {
                String serviceUrl = getString(R.string.service_url) + "/prepareForStudy.do";
                HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(serviceUrl, null, "GET", 5000);
                JSONArray wordCounts = new JSONObject(result).getJSONArray("data");
                return wordCounts;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final JSONArray wordCounts) {
            super.onPostExecute(wordCounts);
            if (getView() == null || getActivity() == null) return;
            if (wordCounts == null) {
                ToastUtil.showToast(getActivity(), "系统异常");
                return;
            }

            prepareTask = null;

            if (getView() == null)
                return;

            try {
                ((TextView) getView().findViewById(R.id.newWords)).setText(wordCounts.getString(0));
                ((TextView) getView().findViewById(R.id.oldWords)).setText(wordCounts.getString(1));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            prepareTask = null;
        }
    }

    private void startStudy() {
        ((MainActivity) getActivity()).switchBdcFragment(BeforeBdcFragment.class.getSimpleName(), true);
    }
}
