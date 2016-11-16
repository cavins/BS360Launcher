package com.ljw.device3x.Activity;

import android.app.Application;
import android.content.Context;

/**
 * Created by Administrator on 2016/8/14 0014.
 */
public class DeviceApplication extends Application{

    private static Context mContext;

    public static Context getContext() {
        if (mContext == null) {
            throw new RuntimeException("Unknown Error");
        }
        return mContext;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }
}
