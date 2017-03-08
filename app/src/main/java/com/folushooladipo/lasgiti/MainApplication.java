package com.folushooladipo.lasgiti;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // This is required for using the Fresco lib. Do it here and NOT in every activity.
        Fresco.initialize(this);
    }
}