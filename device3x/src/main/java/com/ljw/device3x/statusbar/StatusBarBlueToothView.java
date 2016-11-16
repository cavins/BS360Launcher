package com.ljw.device3x.statusbar;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.ljw.device3x.R;

import java.lang.ref.WeakReference;

/**
 * Created by Administrator on 2016/10/8 0008.
 */
public class StatusBarBlueToothView extends ImageView{
    private static int STATE_ON = BluetoothAdapter.STATE_ON;
    private static int STATE_OFF = BluetoothAdapter.STATE_OFF;
    private BluetoothHandler bluetoothHandler;
    private BluetoothAdapter myBluetoothAdapter;


    public StatusBarBlueToothView(Context context) {
        this(context, null);
    }

    public StatusBarBlueToothView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i("ljwtest:", "StatusBarBlueToothView被新建了");
        bluetoothHandler = new BluetoothHandler(this);
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter.isEnabled())
            this.setImageResource(R.mipmap.statusbar_bluetooth);
    }

//    public StatusBarBlueToothView(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//
//    }

    private static class BluetoothHandler extends Handler {
        WeakReference<StatusBarBlueToothView> mView;

        public BluetoothHandler(StatusBarBlueToothView view) {
            mView = new WeakReference<StatusBarBlueToothView>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (mView.get() == null) {
                return;
            }
            StatusBarBlueToothView view = mView.get();
            if(msg.what != STATE_ON)
                view.setVisibility(View.GONE);
            else {
                view.setVisibility(View.VISIBLE);
                view.setImageResource(R.mipmap.statusbar_bluetooth);
            }

//            view.setImageResource(msg.what == STATE_ON ? R.mipmap.bluetooth_on : R.mipmap.bluetooth_off);
        }
    }

    private BroadcastReceiver bluetoothReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) || BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int bluetoothState = myBluetoothAdapter.getState();
                switch (bluetoothState) {
                    case BluetoothAdapter.STATE_OFF:
                        bluetoothHandler.sendEmptyMessage(STATE_OFF);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        bluetoothHandler.sendEmptyMessage(STATE_OFF);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        bluetoothHandler.sendEmptyMessage(STATE_ON);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        bluetoothHandler.sendEmptyMessage(STATE_ON);
                        break;
                }
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        getContext().registerReceiver(bluetoothReceive, intentFilter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        bluetoothHandler.removeCallbacksAndMessages(null);
        getContext().unregisterReceiver(bluetoothReceive);
    }
}
