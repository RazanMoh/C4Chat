package com.c4chat.mychat.ui.fragments;

import android.app.Application;
import android.content.Context;

/**
 * Created by hendalzahrani on 8/14/17.
 */

public class FirebaseChatMainApp extends Application{

    private static Context context;

    public void onCreate() {
        super.onCreate();
        FirebaseChatMainApp.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return FirebaseChatMainApp.context;
    }
}
