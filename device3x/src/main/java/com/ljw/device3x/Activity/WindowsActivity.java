package com.ljw.device3x.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.util.Util;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.ljw.device3x.MainActivity;
import com.ljw.device3x.R;
import com.ljw.device3x.Utils.Utils;
import com.ljw.device3x.adapter.ButtonViewAdapter;
import com.ljw.device3x.adapter.ButtonViewLevel2Adapter;
import com.ljw.device3x.adapter.ContentFragmentAdapter;
import com.ljw.device3x.adapter.pagerAdapter;
import com.ljw.device3x.common.CommonBroacastName;
import com.ljw.device3x.common.CommonCtrl;
import com.ljw.device3x.gpscontrol.MyGpsHardware;
import com.ljw.device3x.gpscontrol.MyGpsListener;
import com.ljw.device3x.gpscontrol.MyLocation;
import com.ljw.device3x.service.LocationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import me.kaelaela.verticalviewpager.VerticalViewPager;

/**
 * Created by Administrator on 2016/5/25 0025.
 */
public class WindowsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{
    private Level1_Fragment level1_fragment;
    private Level2_Fragment level2_fragment;
    private VerticalViewPager verticalViewPager;
    private final String ACTION_LAUNCHER_PAGE_CHANGED = "action_launcher_page_changed";//notify launcher to change page
    private static final String CHANGE_LOCAL_MAP = "action_change_local_map";
    private static final String SYNCMAP_FROM_SPEECH = "com.bs360.syncmapfromspeech";
    private static final String SD_CARD_IS_IN = "SD卡已插入";
    private static final String SD_CARD_IS_OUT = "SD卡已拔出";
    private static final String SD_CARD_PATH = "/storage/sdcard1";
    private static final int EVENT_CLOSE_NAVIGATION = 0x100;
    private MyGpsHardware myGpsHardware;
    /**
     * 发送TTS语音播报广播
     */
    public static final String AIOS_TTS_SPEAK = "com.aispeech.aios.adapter.speak";
    private MsgRecieve mRecieve;
    private IntentFilter intentFilter;
    private boolean isArmOrSystem = false;
    public static WindowsActivity windowsActivity = null;
    private NavigationView navigationView;
    private DrawerLayout drawer;
    SeekBar brightSeekBar;
    SeekBar voiceSeekbar;
    View headView;
    private Timer timer;
    private CloseDrawbleTimer closeDrawbleTimer;
    private CloseDrawbleHandler closeDrawbleHandler;
    private ImageView quickSetTip;
    private ImageView quickSetClose;
    private int currentLevelPage = 0;

    private StorageManager storageManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        windowsActivity = this;
        setContentView(R.layout.float_window_setting);
        initView();
        notifyFMAndBt();
    }

    private void notifyFMAndBt() {
        Intent intent = new Intent(CommonBroacastName.NOTIFY_FM_BLUETOOTH);
        sendBroadcast(intent);
    }

    /**
     * @createDate
     * @version v0.5.1
     */
    public static class UsbBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_MEDIA_EJECT)){
                Log.i("ljwtest:", "SD卡被拔出");
                Intent tts = new Intent(AIOS_TTS_SPEAK);
                tts.putExtra("text", SD_CARD_IS_OUT);
                context.sendBroadcast(tts);
            }else if(action.equals(Intent.ACTION_MEDIA_MOUNTED) && SD_CARD_PATH.equals(intent.getData().getPath())){
                Log.i("ljwtest:", "SD卡已插入");
                Intent tts = new Intent(AIOS_TTS_SPEAK);
                tts.putExtra("text", SD_CARD_IS_IN);
                context.sendBroadcast(tts);
            } else if(action.equals("android.intent.action.MEDIA_REMOVED")|| action.equals("android.intent.action.ACTION_MEDIA_UNMOUNTED")) {
                Log.i("ljwtest:", "SD卡已卸载");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(currentLevelPage == 0)
//            Utils.getInstance().notifyHomeChangedIcon(1);
//        else
            Utils.getInstance().notifyHomeChangedIcon(currentLevelPage);
    }

    @Override
    protected void onStop() {
        super.onStop();
        log_i("launcher onstop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        log_i("launcher onPause");
        Utils.getInstance().notifyHomeChangedIcon(1);
    }

    /**
     * 初始化View
     */
    private void initView() {
        level1_fragment = new Level1_Fragment();
        level2_fragment = new Level2_Fragment();

        mRecieve = new MsgRecieve();
        intentFilter = new IntentFilter(ACTION_LAUNCHER_PAGE_CHANGED);
        intentFilter.addAction(CHANGE_LOCAL_MAP);
        intentFilter.addAction(SYNCMAP_FROM_SPEECH);
        intentFilter.addAction("com.bs360.synclaunchervol");
        intentFilter.addAction("com.bs360.synclauncherbri");
        intentFilter.addAction("com.bs360.volchangefromspeech");
        intentFilter.addAction("com.bs360.brichangefromspeech");
        registerReceiver(mRecieve, intentFilter);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.getBackground().setAlpha(150);
        headView = navigationView.getHeaderView(0);
        navigationView.setNavigationItemSelectedListener(this);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                quickSetClose.setImageResource(slideOffset < 1.0 ? R.mipmap.quick_set_arrow : R.mipmap.quick_set_close);

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                if(quickSetTip.getVisibility() == View.VISIBLE)
                    quickSetTip.setVisibility(View.INVISIBLE);
                startTimerAfterNaviOpen();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                if(quickSetTip.getVisibility() == View.INVISIBLE)
                    quickSetTip.setVisibility(View.VISIBLE);

                if(timer != null && closeDrawbleTimer != null)
                    closeDrawbleTimer.cancel();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });
        quickSetTip = (ImageView) findViewById(R.id.quick_set_tips);
        quickSetTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawer.openDrawer(Gravity.LEFT);
            }
        });

        quickSetClose = (ImageView)headView.findViewById(R.id.quick_set_end);
        quickSetClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawer.closeDrawer(Gravity.LEFT);
            }
        });


        myGpsHardware = new MyGpsHardware();
        myGpsHardware.open(this, new MyGpsListener() {
            @Override
            public void onLocationChanged(MyLocation location) {
                log_i(location.toString());
                level1_fragment.displayDirectionAndSpeed(location.course, location.speed);
            }
        });

        verticalViewPager = (VerticalViewPager)findViewById(R.id.vertical_viewpager);
        verticalViewPager.setAdapter(new ContentFragmentAdapter.Holder(getSupportFragmentManager())
                .add(level1_fragment)
                .add(level2_fragment)
                .set());
        verticalViewPager.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        verticalViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Utils.getInstance().notifyHomeChangedIcon(position);
                currentLevelPage = position;

//                if(!isArmOrSystem) {
//                    Log.i("ljwtest:", "现在是手动滑动到第" + position + "页");
//                    Utils.getInstance().notifyHomeChangedIcon(position);
//                }
//                else {
//                    Log.i("ljwtest:", "现在是系统的滑动");
//                    isArmOrSystem = false;
//                }
//                Log.i("ljwtest:", "已从第" + position + "页发送广播,附加值是" + position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        closeDrawbleHandler = new CloseDrawbleHandler();
        timer = new Timer(true);
        initVoiceSeekbar();
        initBrightSeekbar();
    }



    public void jumpToPage2() {
        verticalViewPager.setCurrentItem(1);
        isArmOrSystem = true;
    }

    public void jumpToPage1() {
        verticalViewPager.setCurrentItem(0);
        isArmOrSystem = true;
        level1_fragment.jumpIfLevel1SecondPageVisible();
    }

    /**
     * 开启闹钟计时
     */
    private void startTimerAfterNaviOpen() {
        if(timer != null && closeDrawbleTimer != null)
            closeDrawbleTimer.cancel();
        closeDrawbleTimer = new CloseDrawbleTimer();
        timer.schedule(closeDrawbleTimer, 30000);
    }

    /**
     * 闹钟handler
     */
    private class CloseDrawbleHandler extends android.os.Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == EVENT_CLOSE_NAVIGATION)
                closeDrawble();
        }
    }

    private void closeDrawble() {
        if(drawer != null)
            drawer.closeDrawer(Gravity.LEFT);
    }

    /**
     * 闹钟到点提醒
     */
    class CloseDrawbleTimer extends TimerTask {

        @Override
        public void run() {
            Message msg = closeDrawbleHandler.obtainMessage(EVENT_CLOSE_NAVIGATION);
            msg.sendToTarget();
        }
    }

    /**
     * 开启定位服务
     */
    private void startLocationService() {
        Intent intent = new Intent(this, LocationService.class);
        startService(intent);
    }

    /**
     * 关闭定位服务
     */
    private void stopLocationService() {
        Intent intent = new Intent(this, LocationService.class);
        stopService(intent);
    }

    private void log_i(String s) {
        Log.i("ljwtest:", s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        stopLocationService();
        unregisterReceiver(mRecieve);
        myGpsHardware.close();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return false;
    }

    /**
     * 接收广播
     */
    public class MsgRecieve extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(ACTION_LAUNCHER_PAGE_CHANGED)) {
                int flag = intent.getIntExtra("whichPage", 0);
                if(flag != 0) {
                    if(flag == 1) {
                        log_i("收到了按键传来的跳转到第0页的广播");
                        jumpToPage1();
                    }
                    else {
                        log_i("收到了按键传来的跳转到第1页的广播");
                        jumpToPage2();
                    }
                }
            } else if(action.equals(CHANGE_LOCAL_MAP)) {
                Log.i("ljwtest:", "收到的地图是" + intent.getStringExtra("maptype"));
                Utils.setLocalMapType(intent.getStringExtra("maptype"), getApplicationContext());
            } else if (action.equals(SYNCMAP_FROM_SPEECH)) {
                Log.i("ljwtest:", "收到的地图是" + intent.getStringExtra("maptype"));
                Utils.setLocalMapType(intent.getStringExtra("maptype"), getApplicationContext());
            } else if(action.equals("com.bs360.synclaunchervol") || action.equals("com.bs360.volchangefromspeech")) {
                initVoiceSeekbar();
            } else if(action.equals("com.bs360.synclauncherbri") || action.equals("com.bs360.brichangefromspeech")) {
                initBrightSeekbar();
            }
        }
    }

    private void initBrightSeekbar() {
        brightSeekBar = (SeekBar)headView.findViewById(R.id.brightseekbar);
        // 进度条绑定最大亮度，255是最大亮度
        brightSeekBar.setMax(255);
        // 取得当前亮度
        int normal = Settings.System.getInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 255);
        // 进度条绑定当前亮度
        brightSeekBar.setProgress(normal);

        brightSeekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        brightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Intent intent1 = new Intent("com.bs360.syncsettingbri");
                sendBroadcast(intent1);
//                // 取得当前进度
//                int tmpInt = seekBar.getProgress();
//
//                // 当进度小于80时，设置成80，防止太黑看不见的后果。
//                if (tmpInt < 20) {
//                    tmpInt = 20;
//                }
//
//                // 根据当前进度改变亮度
//                Settings.System.putInt(getContentResolver(),
//                        Settings.System.SCREEN_BRIGHTNESS, tmpInt);
//                tmpInt = Settings.System.getInt(getContentResolver(),
//                        Settings.System.SCREEN_BRIGHTNESS, -1);
//                WindowManager.LayoutParams wl = getWindow().getAttributes();
//
//                float tmpFloat = (float) tmpInt / 100;
//                if (tmpFloat > 0 && tmpFloat <= 1) {
//                    wl.screenBrightness = tmpFloat;
//                }
//                getWindow().setAttributes(wl);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                // 取得当前进度
                int tmpInt = progress;

                // 当进度小于80时，设置成80，防止太黑看不见的后果。
                if (tmpInt < 50) {
                    tmpInt = 50;
                }

                // 根据当前进度改变亮度
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, tmpInt);
                tmpInt = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, -1);
                WindowManager.LayoutParams wl = getWindow().getAttributes();

                float tmpFloat = (float) tmpInt / 255;
                if (tmpFloat > 0 && tmpFloat <= 1) {
                    wl.screenBrightness = tmpFloat;
                }
                getWindow().setAttributes(wl);
            }
        });
    }

    private void setScreenBrightness(float b){
        //取得window属性保存在layoutParams中
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = b;//b已经除以10
        getWindow().setAttributes(layoutParams);
    }

    private void initVoiceSeekbar() {
        voiceSeekbar = (SeekBar)headView.findViewById(R.id.voiceseekbar);
        //获取到系统服务
        final AudioManager mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final int curvolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        voiceSeekbar.setMax(maxVolume);
        voiceSeekbar.setProgress(curvolume);
        voiceSeekbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        voiceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
                Intent intent1 = new Intent("com.bs360.syncsettingvol");
                sendBroadcast(intent1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                // TODO Auto-generated method stub
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
                mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM , arg1, 0);
                mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION , arg1, 0);
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING , arg1, 0);
                mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM , arg1, 0);
                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL , arg1, 0);
                mAudioManager.setStreamVolume(AudioManager.STREAM_DTMF , arg1, 0);
                voiceSeekbar.setProgress(arg1);
            }
        });
    }


    /**
     * 发送TTS语音播报
     *
     * @param content
     */
    private void sendTTSpeak(String content) {
        Intent tts = new Intent(AIOS_TTS_SPEAK);
        tts.putExtra("text", content);
        sendBroadcast(tts);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(currentLevelPage == 0 && !level1_fragment.isLevel1Visible)
                return true;
            else if(currentLevelPage == 1)
                jumpToPage1();
            else
                level1_fragment.jumpIfLevel1SecondPageVisible();
        }
//        return super.onKeyDown(keyCode, event);
        return false;
    }
}
