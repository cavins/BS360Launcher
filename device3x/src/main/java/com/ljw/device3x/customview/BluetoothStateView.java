package com.ljw.device3x.customview;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ljw.device3x.R;

/**
 * Created by Administrator on 2016/5/6 0006.
 */
public class BluetoothStateView extends LinearLayout {
    private static int STATE_ON = BluetoothAdapter.STATE_ON;
    private static int STATE_OFF = BluetoothAdapter.STATE_OFF;
    private BluetoothAdapter myBluetoothAdapter;

    private ImageView imageView;
    Context context;
    TextView textView;
    public BluetoothStateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.bluetoothstateview, this);
        imageView = (ImageView) findViewById(R.id.bluetoothimage);
        textView = (TextView) findViewById(R.id.bluetooth_text);
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        imageView.setImageResource(myBluetoothAdapter.isEnabled() ? R.mipmap.bluetooth_on : R.mipmap.bluetooth_off);
        textView.setTextColor(myBluetoothAdapter.isEnabled() ? context.getResources().getColor(R.color.white) : context.getResources().getColor(R.color.dark_text));
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (myBluetoothAdapter.getState())
                {
                    case BluetoothAdapter.STATE_ON:
                        myBluetoothAdapter.disable();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        myBluetoothAdapter.disable();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        myBluetoothAdapter.enable();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        myBluetoothAdapter.enable();
                        break;
                }
            }
        });
    }

//    private static class BluetoothHandler extends Handler {
//        WeakReference<ImageView> mView;
//
//        public BluetoothHandler(ImageView view) {
//            mView = new WeakReference<BluetoothStateView.imageView>(view);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//
//            if (mView.get() == null) {
//                return;
//            }
//            BluetoothStateView view = mView.get();
//
//            view.setImageResource(msg.what == STATE_ON ? R.mipmap.bluetooth_on : R.mipmap.bluetooth_off);
//        }
//    }

    private BroadcastReceiver bluetoothReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) || BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                refreshButton();
            }
        }
    };

    //���°�ť״̬
    private void refreshButton()
    {
        switch (myBluetoothAdapter.getState())
        {
            case BluetoothAdapter.STATE_ON:
                imageView.setImageResource(R.mipmap.bluetooth_on);
                textView.setTextColor(context.getResources().getColor(R.color.white));
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                imageView.setImageResource(R.mipmap.bluetooth_on);
                textView.setTextColor(context.getResources().getColor(R.color.white));
                break;
            case BluetoothAdapter.STATE_OFF:
                imageView.setImageResource(R.mipmap.bluetooth_off);
                textView.setTextColor(context.getResources().getColor(R.color.dark_text));
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                imageView.setImageResource(R.mipmap.bluetooth_off);
                textView.setTextColor(context.getResources().getColor(R.color.dark_text));
                break;
        }
    }

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
        getContext().unregisterReceiver(bluetoothReceive);
    }
}
