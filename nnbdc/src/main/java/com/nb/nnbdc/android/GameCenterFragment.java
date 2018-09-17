package com.nb.nnbdc.android;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nb.nnbdc.R;
import com.nb.nnbdc.android.util.ToastUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beidanci.vo.GameHallVo;
import beidanci.vo.GetGameHallDataResult;
import beidanci.vo.HallGroupVo;
import beidanci.vo.HallVo;

/**
 * Created by myb on 18-9-16.
 */

public class GameCenterFragment extends MyFragment {
    private GetGameHallDataTask getGameHallDataTask;

    /**
     * 游戏大厅数据
     */
    private GetGameHallDataResult gameHallData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gamecenter, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getGameHallDataTask = new GetGameHallDataTask(getMainActivity());
        getGameHallDataTask.execute((Void) null);
    }

    @Override
    public void onFragmentSwitched(MyFragment from, MyFragment to) {

    }

    /**
     * 获取游戏大厅的数据
     */
    public class GetGameHallDataTask extends MyAsyncTask<Void, Void, Void> {
        protected GetGameHallDataTask(MyActivity myActivity) {
            super(myActivity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                //获取游戏大厅数据
                gameHallData = getHttpClient().getGameHallData();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            final MainActivity mainActivity = getMainActivity();
            getGameHallDataTask = null;
            if (getView() == null || getActivity() == null) return;

            if (gameHallData == null) {//前面的步骤发生了异常
                ToastUtil.showToast(getActivity(), "发生了异常，请重试");
                return;
            }

            // 把Hall以名字为key组织为Hash map
            Map<String, HallVo> hallsByName = new HashMap<>();
            for (HallVo hall : gameHallData.getHalls()) {
                hallsByName.put(hall.getName(), hall);
            }

            // 计算大厅分组及每个大厅中的在线人数
            for (HallGroupVo hallGroup : gameHallData.getHallGroups()) {
                int userCount = 0;
                for (GameHallVo gameHall : hallGroup.getGameHalls()) {
                    HallVo hall = hallsByName.get(gameHall.getHallName());
                    gameHall.setUserCount(hall == null ? 0 : hall.getUserCount());
                    userCount += gameHall.getUserCount();
                }
                hallGroup.setUserCount(userCount);
            }

            for (int i = 0; i < gameHallData.getHallGroups().size(); i++) {//每个大厅分组
                HallGroupVo hallGroup = gameHallData.getHallGroups().get(i);

                //显示分组名
                LinearLayout layout = (LinearLayout) getView().findViewById(R.id.layout);
                TextView hallGroupView = new TextView(GameCenterFragment.this.getActivity());
                hallGroupView.setTextColor(ContextCompat.getColor(GameCenterFragment.this.getActivity(), R.color.titleText));
                hallGroupView.setText(String.format("%s (%d人)", hallGroup.getGroupName(), hallGroup.getUserCount()));
                hallGroupView.setPadding(0, 8, 0, 8);
                layout.addView(hallGroupView);

                //点击组名时的事件处理
                hallGroupView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean expanded = (boolean) v.getTag(R.id.tag_is_group_expanded);
                        List<TextView> hallViews = (List<TextView>) v.getTag(R.id.tag_views_under_group);

                        //展开/收起该组下的游戏大厅列表
                        for (TextView hallView : hallViews) {
                            hallView.setVisibility(expanded ? View.GONE : View.VISIBLE);
                        }
                        v.setTag(R.id.tag_is_group_expanded, !expanded);
                    }
                });

                //显示分组下的游戏大厅
                List<GameHallVo> halls = hallGroup.getGameHalls();
                List<TextView> hallViews = new ArrayList<>();
                hallGroupView.setTag(R.id.tag_is_group_expanded, false); // 是否展开显示
                hallGroupView.setTag(R.id.tag_views_under_group, hallViews); //大厅列表
                for (int j = 0; j < halls.size(); j++) {
                    GameHallVo hall = halls.get(j);
                    TextView hallView = new TextView(mainActivity);
                    hallView.setTextColor(ContextCompat.getColor(GameCenterFragment.this.getActivity(), R.color.defaultTextColor));
                    hallView.setTextSize(12);
                    hallView.setText(String.format("%s (%d人)", hall.getHallName(), hall.getUserCount()));
                    hallView.setTag(hall);
                    hallView.setPadding(0, 8, 0, 8);
                    hallView.setVisibility(View.GONE);
                    layout.addView(hallView);
                    hallViews.add(hallView);

                    //设置大厅的点击事件处理
                    hallView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            GameHallVo hall = ((GameHallVo) v.getTag());
                            getMainActivity().switchToRussiaFragment(GameCenterFragment.this, hall.getHallName(), null);
                        }
                    });
                }
            }

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            getGameHallDataTask = null;
        }
    }
}
