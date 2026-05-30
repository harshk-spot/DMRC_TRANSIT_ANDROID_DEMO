package com.pce.dmrc.ncmccard;

import android.app.Application;
import android.content.Context;

public class MainApplication extends Application {

    private static Context mAppContext;

    public static Context getmAppContext() {
        return mAppContext;
    }

    public static void setmAppContext(Context mAppContext) {
        MainApplication.mAppContext = mAppContext;
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }
}
