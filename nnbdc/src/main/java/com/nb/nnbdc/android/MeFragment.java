package com.nb.nnbdc.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.task.GetLoggedInUserTask;
import com.nb.nnbdc.android.util.MyProgress;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import beidanci.vo.UserVo;

public class MeFragment extends MyFragment {
    private RenderStudyProgressTask renderStudyProgressTask = null;
    private DeleteDictTask deleteDictTask = null;
    private SaveWordsPerDayTask saveWordsPerDayTask = null;
    private PrivilegeDictTask privilegeDictTask = null;
    private BeforeStudyTask beforeStudyTask = null;

    //缺省单词书的背景色（未设置有限取词的单词书的背景色）
    private int defaultDictBackgoundColor;

    //设置了优先取词的单词书的背景色
    private int privilegedDictBackgoundColor;

    //当前点击的单词书的背景色（高亮显示）
    private int selectedDictBackgoundColor;

    //所有学习中的单词书的界面元素与背景数据
    private Map<View, JSONObject> learningDicts = new HashMap<View, JSONObject>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_me, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //从资源文件读取颜色定义
        defaultDictBackgoundColor = ContextCompat.getColor(getActivity(), R.color.level2);
        selectedDictBackgoundColor = ContextCompat.getColor(getActivity(), R.color.highlight);
        privilegedDictBackgoundColor = ContextCompat.getColor(getActivity(), R.color.privilegedDictBackgoundColor);

        //渲染进度学习进度信息
        renderStudyProgressTask = new RenderStudyProgressTask(getMainActivity());
        renderStudyProgressTask.execute((Void) null);

        //渲染每日单词量设置框
        GetLoggedInUserTask.getLoggedInUser(getMainActivity(), new GetLoggedInUserTask.CallBack() {
            @Override
            public void onSuccess(UserVo user) {
                Spinner spin = (Spinner) getView().findViewById(R.id.wordsPerDay);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.words_per_day_arry, R.layout.spinner_item);
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                spin.setAdapter(adapter);
                Util.setSpinnerItemSelectedByValue(spin, String.valueOf(user.getWordsPerDay()));

                spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        String selectedValue = parentView.getItemAtPosition(position).toString();
                        saveWordsPerDayTask = new SaveWordsPerDayTask(getMainActivity());
                        saveWordsPerDayTask.execute(Integer.parseInt(selectedValue));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                    }
                });
            }

            @Override
            public void onFailed() {

            }
        });


        getView().findViewById(R.id.startBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStudy();
            }
        });

        getView().findViewById(R.id.selectBookBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMainActivity().switchToSelectBookFragment(MeFragment.this);
            }
        });

        getView().findViewById(R.id.msgBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMainActivity().switchToMsgFragment(MeFragment.this);
            }
        });

        getView().findViewById(R.id.rawWordBook).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    learningDictClicked(v);
                } catch (JSONException e) {
                    ToastUtil.showToast(getActivity(), "系统异常:" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onFragmentSwitched(MyFragment from, MyFragment to) {

    }

    public class DeleteDictTask extends MyAsyncTask<View, Void, Boolean> {
        private View dictView;

        protected DeleteDictTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected Boolean doInBackground(View... params) {
            dictView = params[0];
            try {
                String dictName = learningDicts.get(dictView).getJSONObject("dict").getString("name");
                String serviceUrl = getString(R.string.service_url) + "/deleteSelectedDict.do";
                Map<String, Object> postParams = new HashMap<>();
                postParams.put("dictName", dictName);
                HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(serviceUrl, postParams, "GET", 5000);
                return result == null ? false : new JSONObject(result).getBoolean("success");
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean succ) {
            super.onPostExecute(succ);
            if (getView() == null || getActivity() == null) return;
            deleteDictTask = null;

            //从界面上删除相应元素
            ((ViewGroup) dictView.getParent()).removeView(dictView);
            learningDicts.remove(dictView);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            deleteDictTask = null;
        }
    }

    /**
     * 保存每日单词数的任务
     */
    public class SaveWordsPerDayTask extends MyAsyncTask<Integer, Void, Boolean> {

        protected SaveWordsPerDayTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            int wordsPerDay = params[0];
            try {
                String serviceUrl = getString(R.string.service_url) + "/saveWordsPerDay.do";
                Map<String, Object> postParams = new HashMap<>();
                postParams.put("wordsPerDay", wordsPerDay);
                HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(serviceUrl, postParams, "GET", 5000);
                return result == null ? false : new JSONObject(result).getBoolean("success");
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean succ) {
            super.onPostExecute(succ);
            if (getView() == null || getActivity() == null) return;
            saveWordsPerDayTask = null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            saveWordsPerDayTask = null;
        }
    }


    /**
     * 切换单词书”优先取词“状态的任务
     */
    public class PrivilegeDictTask extends MyAsyncTask<View, Void, Boolean> {
        private View dictView;

        protected PrivilegeDictTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected Boolean doInBackground(View... params) {
            dictView = params[0];
            try {
                String dictName = getDictShortNameByView(dictView);
                dictName = dictName.equals("生词本") ? "rawWordBook" : (dictName + ".dict");
                String serviceUrl = getString(R.string.service_url) + "/privilegeSelectedDict.do";
                Map<String, Object> postParams = new HashMap<>();
                postParams.put("dictName", dictName);
                HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(serviceUrl, postParams, "GET", 5000);
                return result == null ? false : new JSONObject(result).getBoolean("success");
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean succ) {
            super.onPostExecute(succ);
            if (getView() == null || getActivity() == null) return;
            privilegeDictTask = null;
            if (succ) {
                ((MainActivity) getActivity()).switchToMeFragment(MeFragment.this);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            privilegeDictTask = null;
        }
    }

    /**
     * 在用户点击“开始学习”之后，需要判断是进入选书页面还是学习页面
     */
    public class BeforeStudyTask extends MyAsyncTask<Void, Void, String> {
        private View dictView;

        protected BeforeStudyTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                String serviceUrl = getString(R.string.service_url) + "/needSelectDictBeforeStudy.do";
                HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(serviceUrl, null, "GET", 5000);
                boolean needSelectDict = new JSONObject(result).getBoolean("data");
                return needSelectDict ? "selectDict" : "beforeBdc";
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final String nextActivity) {
            super.onPostExecute(nextActivity);
            if (getView() == null || getActivity() == null) return;
            beforeStudyTask = null;
            if (nextActivity == null) { //前面的步骤发生了异常
                ToastUtil.showToast(getActivity(), "发生异常，请重试");
            } else if (nextActivity.equals("selectDict")) {
                ((MainActivity) getActivity()).switchToSelectBookFragment(MeFragment.this);
            } else if (nextActivity.equals("beforeBdc")) {
                ((MainActivity) getActivity()).switchToBeforeBdcFragment(MeFragment.this);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            beforeStudyTask = null;
        }
    }

    public class RenderStudyProgressTask extends MyAsyncTask<Void, Void, JSONObject> {
        RenderStudyProgressTask(MyActivity myActivity) {
            super(myActivity);
        }

        /**
         * 从服务端查询学习进度
         *
         * @param serviceBaseUrl
         * @return
         * @throws JSONException
         * @throws IOException
         */
        public JSONObject queryData(String serviceBaseUrl) throws JSONException, IOException {
            String serviceUrl = serviceBaseUrl + "/getStudyProgress.do";
            HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
            String result = httpClient.sendAjax(serviceUrl, null, "GET", 10000);
            JSONObject jo = result == null ? null : new JSONObject(result);
            return jo;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                JSONObject jo = queryData(getString(R.string.service_url));
                return jo;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(final JSONObject data) {
            super.onPostExecute(data);
            if (getView() == null || getActivity() == null) return;
            renderStudyProgressTask = null;
            if (data == null) {
                ToastUtil.showToast(getActivity(), "系统异常");
                return;
            }
            if (getView() == null) {//快速点击底部的【进度】按钮，会创建新的fragment，此时老的fragment的getView()可能返回null
                return;
            }

            //渲染进度学习进度信息
            try {
                ((TextView) getView().findViewById(R.id.studyDays)).setText(String.format("第 %s 天", data.getString("existDays")));
                ((TextView) getView().findViewById(R.id.level)).setText(data.getJSONObject("level").getString("name"));
                ((TextView) getView().findViewById(R.id.dakaDayCount)).setText(data.getString("dakaDayCount"));
                ((TextView) getView().findViewById(R.id.totalScore)).setText(data.getString("totalScore"));
                ((TextView) getView().findViewById(R.id.userOrder)).setText(data.getString("userOrder").equals("-1") ? "无" : data.getString("userOrder"));
                ((TextView) getView().findViewById(R.id.dakaRatio)).setText(String.format("%.2f", data.getDouble("dakaRatio") * 100) + "%");
                ((TextView) getView().findViewById(R.id.cowDung)).setText(data.getString("cowDung"));
                ((TextView) getView().findViewById(R.id.continuousDakaDayCount)).setText(data.getString("continuousDakaDayCount"));
                ((TextView) getView().findViewById(R.id.extraScore)).setText(data.getString("continuousDakaDayCount"));
                ((TextView) getView().findViewById(R.id.masteredWordsCount)).setText(data.getString("masteredWordsCount"));
                ((TextView) getView().findViewById(R.id.learningWordsCount)).setText(data.getString("learningWordsCount"));

                //生词本
                TextView rawWordBook = (TextView) getView().findViewById(R.id.rawWordBook);
                rawWordBook.setText(String.format("生词本(%d 词)", data.getInt("rawWordCount")));
                boolean isRawWordBookPrivileged = data.getBoolean("isRawWordBookPrivileged");
                //构造生词本的背景数据，把它模拟成一本普通单词书
                JSONObject rawWordBookData = new JSONObject();
                rawWordBookData.put("isPrivileged", isRawWordBookPrivileged);
                JSONObject dictData = new JSONObject();
                dictData.put("shortName", "生词本");
                rawWordBookData.put("dict", dictData);
                learningDicts.put(rawWordBook, rawWordBookData);

                //单词书
                JSONArray selectedLearningDicts = (JSONArray) data.get("selectedLearningDicts");
                LinearLayout layout = (LinearLayout) getView().findViewById(R.id.learningDicts);
                for (int i = 0; i < selectedLearningDicts.length(); i++) {
                    JSONObject learningDict = selectedLearningDicts.getJSONObject(i);

                    //容器layout
                    LinearLayout learningDictLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.learning_dict, null);
                    LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    lp2.topMargin = Util.dp2px(8, MeFragment.this.getActivity());
                    learningDictLayout.setLayoutParams(lp2);
                    layout.addView(learningDictLayout);
                    learningDictLayout.setId(Util.generateViewId());
                    learningDictLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                learningDictClicked(v);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                ToastUtil.showToast(getActivity(), "系统异常:" + e.getMessage());
                            }
                        }
                    });

                    //书名
                    dictData = learningDict.getJSONObject("dict");
                    TextView bookName = (TextView) learningDictLayout.getChildAt(0);
                    bookName.setText(dictData.getString("shortName"));
                    bookName.setId(Util.generateViewId());

                    //进度条
                    MyProgress myProgress = (MyProgress) learningDictLayout.getChildAt(1);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT, Util.dp2px(16, MeFragment.this.getActivity()));
                    myProgress.setLayoutParams(lp);
                    myProgress.setId(Util.generateViewId());
                    myProgress.setMax(dictData.getInt("wordCount"));
                    myProgress.setTextSize(24);
                    myProgress.setProgress(learningDict.isNull("currentWordOrder") ? 0 : learningDict.getInt("currentWordOrder"));

                    learningDicts.put(learningDictLayout, learningDict);
                }

                resetBackgroundOfDicts();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            renderStudyProgressTask = null;
        }
    }

    private PopupMenu learningDictPopupMenu;

    /**
     * 重置所有单词书的背景色
     */
    private void resetBackgroundOfDicts() throws JSONException {
        for (View dictView : learningDicts.keySet()) {
            JSONObject learningDict = learningDicts.get(dictView);
            if (learningDict.getBoolean("isPrivileged")) { //如果单词书设置了优先取词，用特殊背景色标识
                dictView.setBackground(getResources().getDrawable(R.drawable.privileged_dict_background));
            } else {
                dictView.setBackgroundColor(defaultDictBackgoundColor);
            }
            dictView.setAlpha(1);
        }
    }

    /**
     * 重置所有单词书背景色的alpha值，使其回复到正常值（当一个单词书被点击时，会通过改变alpha使其出现选中效果）
     */
    private void resetBackgroundAlphaOfDicts() throws JSONException {
        for (View dictView : learningDicts.keySet()) {
            dictView.setAlpha(1);
        }
    }

    public void learningDictClicked(final View src) throws JSONException {
        //将点击的单词书高亮
        resetBackgroundAlphaOfDicts();
        src.setAlpha(0.5f);

        //创建PopupMenu对象
        learningDictPopupMenu = new PopupMenu(getActivity(), src);
        getActivity().getMenuInflater().inflate(R.menu.learning_dict_menu, learningDictPopupMenu.getMenu());

        //为popup菜单的菜单项单击事件绑定事件监听器
        learningDictPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    switch (item.getItemId()) {
                        case R.id.privileged:
                            learningDictPopupMenu.dismiss();
                            privilegeDictTask = new PrivilegeDictTask(getMainActivity());
                            privilegeDictTask.execute(src);
                            break;
                        case R.id.delete:
                            learningDictPopupMenu.dismiss();
                            //生词本不允许删除
                            if (getDictShortNameByView(src).equals("生词本")) {
                                ToastUtil.showToast(getActivity(), "生词本不可删除");
                                return false;
                            }
                            showDeleteDictConfirmDlg(src);
                            break;
                        default:
                            //使用Toast显示用户单击的菜单项
                            ToastUtil.showToast(getActivity(), "您单击了【" + item.getTitle() + "】菜单项");

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        learningDictPopupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                try {
                    resetBackgroundOfDicts();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        learningDictPopupMenu.show();


    }

    protected void showDeleteDictConfirmDlg(final View dictView) throws JSONException {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(String.format("确定要删除[%s]吗？", getDictShortNameByView(dictView)));
        builder.setTitle("确认");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                deleteDictTask = new DeleteDictTask(getMainActivity());
                deleteDictTask.execute(dictView);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    String getDictShortNameByView(View dictView) throws JSONException {
        return learningDicts.get(dictView).getJSONObject("dict").getString("shortName");
    }

    private void startStudy() {
        beforeStudyTask = new BeforeStudyTask(getMainActivity());
        beforeStudyTask.execute((Void) null);
    }

}
