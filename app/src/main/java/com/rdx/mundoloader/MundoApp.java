package com.rdx.mundoloader;

import android.app.Application;
import android.content.Context;

import net_62v.external.IMundoProcessCallback;
import net_62v.external.MetaActivationManager;
import net_62v.external.MetaCore;

public class MundoApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MetaCore.attachMetaBase(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MetaCore.registerCoreCallback(() -> {
            MetaCore.setProcessLifecycleCallback(new IMundoProcessCallback() {
                @Override
                public void onInitialized() {
                }

                @Override
                public void onCreate(String packageName, String processName) {
                }
            });
            MetaActivationManager.activateSdk("RDXLOADER01");
        });
    }
}
