package com.ljw.device3x.customview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ljw.device3x.R;
import com.ljw.device3x.common.CommonBroacastName;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Administrator on 2016/8/19 0019.
 */
public class MobileStateView extends LinearLayout {
    private ConnectivityManager connectivityManager;
    private IntentFilter intentFilter;
    private ImageView imageView;
    private TextView textView;
    private Context context;
    private static final String NETWORK_CHANGE = "android.intent.action.ANY_DATA_STATE";
    private static final String MY_OPEN_MOBILE = "ljw_open_mibiledata";
    private static final String MY_ASK_MOBILE = "ljw_ask_mibiledata";
    private static final String SYSTEM_MOBILE_STATE = "ljw_system_mobilestate";

    public MobileStateView(Context context) {
        this(context, null);
    }

    public MobileStateView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.mobilestateview, this);
        connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        imageView = (ImageView) findViewById(R.id.mobileimage);
        textView = (TextView) findViewById(R.id.mobiletext);
        imageView.setImageResource(getMobileDataStatus() ? R.mipmap.mobiledata_on : R.mipmap.mobiledata_off);
        textView.setTextColor(getMobileDataStatus() ? context.getResources().getColor(R.color.white) : context.getResources().getColor(R.color.dark_text));
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ljwtest:", "getMobileDataStatus():" + getMobileDataStatus());
                notifyToChangeMobile(MY_OPEN_MOBILE, context, getMobileDataStatus() ? 1 : 0);
                refreshButton();
            }
        });

        imageView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent=new Intent(CommonBroacastName.SYSTEM_WIFIWETTING);
                intent.putExtra("jumptype", "mobileswitch");
                context.startActivity(intent);
                return false;
            }
        });
    }

    //获取移动数据开关状态
    private boolean getMobileDataStatus() {
        ConnectivityManager mConnectivityManager  = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        String methodName = "getMobileDataEnabled";
        Class cmClass = mConnectivityManager.getClass();
        Boolean isOpen = null;

        try {
            Method method = cmClass.getMethod(methodName, null);

            isOpen = (Boolean) method.invoke(mConnectivityManager, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isOpen;
    }

    private void notifyToChangeMobile(String action, Context context, int flag) {
        Intent intent = new Intent(action);
        intent.putExtra("openmobiledata", flag);
        Log.i("ljwtest:", action + "广播已发送,附加值是" + flag);
        context.sendBroadcast(intent);
    }

    private void refreshButton() {
//        if("true".equals(info)) {
//            notifyToChangeMobile(MY_OPEN_MOBILE, getContext(), 1);
//            imageView.setImageResource(R.mipmap.mobiledata_off);
//            textView.setTextColor(context.getResources().getColor(R.color.dark_text));
//        }
//        else if("false".equals(info)) {
//            notifyToChangeMobile(MY_OPEN_MOBILE, getContext(), 0);
//            imageView.setImageResource(R.mipmap.mobiledata_on);
//            textView.setTextColor(context.getResources().getColor(R.color.white));
//        }
        boolean mobileDataStatus = getMobileDataStatus();
        imageView.setImageResource(mobileDataStatus ? R.mipmap.mobiledata_on : R.mipmap.mobiledata_off);
        textView.setTextColor(mobileDataStatus ? context.getResources().getColor(R.color.white) : context.getResources().getColor(R.color.dark_text));

    }

    private BroadcastReceiver MobileDataRecieve = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SYSTEM_MOBILE_STATE.equals(intent.getAction())) {
                refreshButton();
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                Log.i("ljwtest:", "移动网络已改变");
                refreshButton();
            }
        }
    };

    public void openDataNetworkIfWifiIsClose() {
        if(!getMobileDataStatus())
            notifyToChangeMobile(MY_OPEN_MOBILE, context, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(SYSTEM_MOBILE_STATE);
        getContext().registerReceiver(MobileDataRecieve, intentFilter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(MobileDataRecieve);
    }
}
