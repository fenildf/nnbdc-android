package com.nb.nnbdc.android;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.ToastUtil;
import com.nb.nnbdc.android.util.Util;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import beidanci.vo.DictGroupVo;
import beidanci.vo.DictVo;
import beidanci.vo.SelectedDictVo;

public class SelectBookFragment extends MyFragment {
    private GetDictGroupsTask getDictGroupsTask;
    private SaveConfigTask saveConfigTask;


    /**
     * 所有单词书对应的选择框
     */
    private List<CheckBox> dictViews = new ArrayList<>();

    /**
     * 所有单词书分组（含单词书）
     */
    private List<DictGroupVo> dictGroups;

    /**
     * 所有服务端用户已经选中的单词书
     */
    private List<SelectedDictVo> selectedDicts;

    /**
     * 发往服务端的数据（URL+选择的单词书），之所以需要一个成员变量是因为此变量只能在主线程组装
     */
    private String saveConfigUrlAndData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_select_book, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        GetDictGroupsTask getDictGroupsTask = new GetDictGroupsTask(getMainActivity());
        getDictGroupsTask.execute((Void) null);

        getView().findViewById(R.id.okBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfig();
            }
        });

        getView().findViewById(R.id.cancelBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).switchToMeFragment();
            }
        });
    }

    /**
     * 获取所有单词书分组（包含单词书）
     */
    public class GetDictGroupsTask extends MyAsyncTask<Void, Void, Void> {
        protected GetDictGroupsTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                //获取所有单词书分组
                dictGroups = getHttpClient().getDictGroups();

                //获取所有服务端已经选中的单词书
                selectedDicts = getHttpClient().getSelectedDicts();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            getDictGroupsTask = null;
            if (getView() == null || getActivity() == null) return;

            if (dictGroups == null) {//前面的步骤发生了异常
                ToastUtil.showToast(getActivity(), "发生了异常，请重试");
                return;
            }


            for (int i = 0; i < dictGroups.size(); i++) {//每个单词书分组
                DictGroupVo dictGroup = dictGroups.get(i);
                if ( !dictGroup.getName().equals("root") && dictGroup.getDictGroup().getName().equals("root")) {
                    //显示分组名
                    LinearLayout layout = (LinearLayout) getView().findViewById(R.id.layout);
                    TextView dictGroupView = new TextView(SelectBookFragment.this.getActivity());
                    dictGroupView.setTextColor(ContextCompat.getColor(SelectBookFragment.this.getActivity(), R.color.titleText));
                    dictGroupView.setText(dictGroup.getName());
                    layout.addView(dictGroupView);

                    //显示分组下的所有单词书
                    List<DictVo> dicts = dictGroup.getDicts();
                    for (int j = 0; j < dicts.size(); j++) {
                        DictVo dict = dicts.get(j);
                        CheckBox dictView = new CheckBox(SelectBookFragment.this.getActivity());
                        dictView.setTextColor(ContextCompat.getColor(SelectBookFragment.this.getActivity(), R.color.defaultTextColor));
                        dictView.setTextSize(12);
                        dictView.setText(dict.getShortName());
                        dictView.setTag(dict);
                        layout.addView(dictView);
                        dictViews.add(dictView);

                        //判断单词书是否已被选中
                        for (int k = 0; k < selectedDicts.size(); k++) {
                            if (selectedDicts.get(k).getDict().getName().equals(dict.getName())) {
                                dictView.setChecked(true);
                                dictView.setTextColor(ContextCompat.getColor(SelectBookFragment.this.getActivity(), R.color.fieldValue));
                                break;
                            }
                        }

                        //设置单词书checkbox点击事件处理
                        dictView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                buttonView.setTextColor(ContextCompat.getColor(SelectBookFragment.this.getActivity(), isChecked ? R.color.fieldValue : R.color.defaultTextColor));
                            }
                        });
                    }
                }
            }

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            getDictGroupsTask = null;
        }
    }

    /**
     * 获取所有单词书分组（包含单词书）
     */
    public class SaveConfigTask extends MyAsyncTask<Void, Void, JSONObject> {

        private SaveConfigTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                HttpClient httpClient = ((MyApp) getActivity().getApplicationContext()).getHttpClient();
                String result = httpClient.sendAjax(saveConfigUrlAndData, null, "GET", 5000);
                return new JSONObject(result);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            if (getView() == null || getActivity() == null) return;
            saveConfigTask = null;

            if (result == null) {//前面的步骤发生了异常
                ToastUtil.showToast(getActivity(), "发生了异常，请重试");
                return;
            }

            try {
                if (result.getBoolean("success")) {
                    ((MainActivity) getActivity()).switchToMeFragment();
                } else {
                    ToastUtil.showToast(getActivity(), result.getString("msg"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            getDictGroupsTask = null;
        }
    }

    private void saveConfig() {
        saveConfigUrlAndData = getString(R.string.service_url) + "/saveConfig.do";
        boolean isFirstParam = true;
        HashMap<String, Object> addedDicts = new HashMap<>();//用于去重
        for (CheckBox dictView : dictViews) {
            if (dictView.isChecked()) {
                String paramName = isFirstParam ? "?selectedDicts[]=" : "&selectedDicts[]=";
                String paramValue = ((DictVo)dictView.getTag()).getId().toString();
                if (!addedDicts.containsKey(paramValue)) {
                    saveConfigUrlAndData += paramName + paramValue;
                    isFirstParam = false;
                    addedDicts.put(paramValue, null);
                }
            }
        }
        saveConfigUrlAndData = Util.encodeUrl(saveConfigUrlAndData);

        if (addedDicts.isEmpty()) {
            ToastUtil.showToast(getActivity(), "至少选择一本单词书");
            return;
        }

        saveConfigTask = new SaveConfigTask(getMainActivity());
        saveConfigTask.execute((Void) null);
    }

}
