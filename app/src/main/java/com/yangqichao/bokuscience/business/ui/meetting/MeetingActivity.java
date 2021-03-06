package com.yangqichao.bokuscience.business.ui.meetting;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.xys.libzxing.zxing.activity.CaptureActivity;
import com.yangqichao.bokuscience.R;
import com.yangqichao.bokuscience.business.bean.MyMeetingBean;
import com.yangqichao.bokuscience.common.APP;
import com.yangqichao.bokuscience.common.base.BaseActivity;
import com.yangqichao.bokuscience.common.net.CommonsSubscriber;
import com.yangqichao.bokuscience.common.net.RequestBody;
import com.yangqichao.bokuscience.common.net.RequestUtil;
import com.yangqichao.commonlib.util.PreferenceUtils;

import butterknife.BindView;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MeetingActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener,AMapLocationListener,BaseQuickAdapter.RequestLoadMoreListener {

    @BindView(R.id.recycle_meetting)
    RecyclerView recycleMeetting;
    @BindView(R.id.swipe_meeting)
    SwipeRefreshLayout swipeMeeting;
    @BindView(R.id.img_add)
    ImageView imgAdd;

    private static final int REQUESTCODE = 300;
    private String codeUrl;

    private BaseQuickAdapter<MyMeetingBean.RecordsBean,BaseViewHolder> adapter;


    private int page = 1;
    private int pageSize = 10;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_meeting;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        swipeMeeting.setOnRefreshListener(this);
        swipeMeeting.setColorSchemeResources(R.color.base_orange);
        int publish = PreferenceUtils.getPrefInt(this, "publish", -1);
        if (publish == 1) {
            imgAdd.setVisibility(View.VISIBLE);
        }

        adapter = new BaseQuickAdapter<MyMeetingBean.RecordsBean, BaseViewHolder>(R.layout.item_my_meeting) {
            @Override
            protected void convert(BaseViewHolder helper, final MyMeetingBean.RecordsBean item) {
                // TODO: 2017/7/1 会议状态样式

                helper.setText(R.id.tv_meeting_title,item.getMeetingTitle());

                String[] time = item.getGmtCreate().split(" ");
                helper.setText(R.id.tv_meeting_date,time[0]);
                helper.setText(R.id.tv_meeting_time,time[1]);
                // TODO: 2017/7/1 会议状态
                // (0=未开始，1=已开始，2=已结束，3=已取消)
                switch (item.getMeetingState()){
                    case 0:
                        helper.setImageResource(R.id.img_meeting_status,R.drawable.icon_meet_1);
                        helper.setTextColor(R.id.tv_meeting_title,getColorResource(R.color.base_text_black));
                        helper.setTextColor(R.id.tv_meeting_date,getColorResource(R.color.base_text_black));
                        helper.setTextColor(R.id.tv_meeting_time,getColorResource(R.color.base_text_black));
//                        ((TextView)helper.getView(R.id.tv_meeting_title)).getPaint().setFlags(0);
                        break;
                    case 1:
                        helper.setImageResource(R.id.img_meeting_status,R.drawable.icon_meet_2);
                        helper.setTextColor(R.id.tv_meeting_title,getColorResource(R.color.base_text_black));
                        helper.setTextColor(R.id.tv_meeting_date,getColorResource(R.color.base_text_black));
                        helper.setTextColor(R.id.tv_meeting_time,getColorResource(R.color.base_text_black));
//                        ((TextView)helper.getView(R.id.tv_meeting_title)).getPaint().setFlags(0);
                        break;
                    case 2:
                        helper.setImageResource(R.id.img_meeting_status,R.drawable.icon_meet_3);
                        helper.setTextColor(R.id.tv_meeting_title,getColorResource(R.color.base_text_gray_dark));
                        helper.setTextColor(R.id.tv_meeting_date,getColorResource(R.color.gray_dark));
                        helper.setTextColor(R.id.tv_meeting_time,getColorResource(R.color.gray_dark));
//                        ((TextView)helper.getView(R.id.tv_meeting_title)).getPaint().setFlags(0);
                        break;
                    case 3:
                        helper.setImageResource(R.id.img_meeting_status,R.drawable.icon_meet_4);
                        helper.setTextColor(R.id.tv_meeting_title,getColorResource(R.color.base_text_gray_dark));
                        helper.setTextColor(R.id.tv_meeting_date,getColorResource(R.color.gray_dark));
                        helper.setTextColor(R.id.tv_meeting_time,getColorResource(R.color.gray_dark));
//                        ((TextView)helper.getView(R.id.tv_meeting_title)).getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);

                        break;

                }
                helper.getView(R.id.tv_meeting_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(APP.getUserId().equals(item.getCreaterid())&&item.getMeetingState()==0){
                            RequestUtil.createApi().cancelMeeting(item.getMeetingId()+"").compose(RequestUtil.<String>handleResult())
                                    .subscribe(new CommonsSubscriber<String>() {
                                        @Override
                                        protected void onSuccess(String s) {
                                            showToast("会议已取消");
                                            item.setMeetingState(3);
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                        }else{
                            if(!APP.getUserId().equals(item.getCreaterid())){
                                showToast("您不是会议发布人无法取消");
                                return;
                            }
                            if(item.getMeetingState()!=0){
                                showToast("只有未开始会议，可以取消");
                                return;
                            }
                        }
                    }
                });
//                helper.setImageResource(R.id.img_meeting_status,R.drawable.icon_meet_1);
            }
        };
        adapter.setOnLoadMoreListener(this,recycleMeetting);
        recycleMeetting.setLayoutManager(new LinearLayoutManager(this));
        recycleMeetting.setAdapter(adapter);
        recycleMeetting.addOnItemTouchListener(new OnItemClickListener() {
            @Override
            public void onSimpleItemClick(BaseQuickAdapter adapter, View view, int position) {
                MyMeetingBean.RecordsBean recordsBean = (MyMeetingBean.RecordsBean) adapter.getData().get(position);
                MeetingDetailActivity.startAction(MeetingActivity.this, recordsBean.getMeetingId(),recordsBean.getMeetingState());
            }
        });
        adapter.setEmptyView(R.layout.empty_video_view,recycleMeetting);



    }


    @Override
    protected void onResume() {
        super.onResume();
        page = 1;
        getdate();
    }

    private void getdate() {
        RequestBody requestBody = new RequestBody();
        requestBody.setPage(page+"");
        requestBody.setPageSize(pageSize+"");
        requestBody.setUserId(APP.getUserId());
        RequestUtil.createApi().select(requestBody).compose(RequestUtil.<MyMeetingBean>handleResult())
                .subscribe(new CommonsSubscriber<MyMeetingBean>() {
                    @Override
                    protected void onSuccess(MyMeetingBean myMeetingBean) {
                        swipeMeeting.setRefreshing(false);
                        if(page == 1){
                            adapter.setNewData(myMeetingBean.getRecords());
                        }else{
                            if(adapter.getData().size()==myMeetingBean.getTotal()){
                                adapter.loadMoreEnd();
                            }else{
                                adapter.getData().addAll(myMeetingBean.getRecords());
                                adapter.loadMoreComplete();
                            }
                        }
                    }
                });
    }

    @OnClick({R.id.img_back, R.id.img_add, R.id.img_sao})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.img_add:
                Intent intent = new Intent(this, CreateMeetingActivity.class);
                startActivity(intent);
                break;
            case R.id.img_sao:
                MeetingActivityPermissionsDispatcher.saomaWithCheck(this);
                break;
        }
    }


    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                page = 1;
                getdate();
            }
        },1000);
    }



    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    @NeedsPermission(Manifest.permission.CAMERA)
    public void saoma() {
        Intent intent = new Intent(this, CaptureActivity.class);
        startActivityForResult(intent,REQUESTCODE);
    }
    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    public void sign() {
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //获取一次定位结果：该方法默认为false。
//        mLocationOption.setOnceLocation(true);
        //获取最近3s内精度最高的一次定位结果：
        mLocationOption.setOnceLocationLatest(true);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //关闭缓存机制
        mLocationOption.setLocationCacheEnable(false);


        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                //可在其中解析amapLocation获取相应内容。
                String location = amapLocation.getLongitude() + "," + amapLocation.getLatitude();
                RequestUtil.createApi().sign(codeUrl+"/"+APP.getUserId()+"/"+location).compose(RequestUtil.<String>handleResult())
                        .subscribe(new CommonsSubscriber<String>() {
                            @Override
                            protected void onSuccess(String s) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MeetingActivity.this)
                                        .setMessage("恭喜您会议签到成功")
                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.dismiss();
                                            }
                                        });
                                builder.show();
                            }

                            @Override
                            public void onFail(String errorCode, String message) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MeetingActivity.this)
                                        .setMessage(message)
                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.dismiss();
                                            }
                                        });
                                builder.show();
                            }
                        });
            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MeetingActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mLocationClient!=null){
            mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUESTCODE == requestCode && data != null) {
            codeUrl = data.getExtras().getString("result");
            MeetingActivityPermissionsDispatcher.signWithCheck(this);
        }
    }

    @Override
    public void onLoadMoreRequested() {
        page++;
        getdate();
    }
}
