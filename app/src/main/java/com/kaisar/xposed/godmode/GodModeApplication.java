package com.kaisar.xposed.godmode;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by jrsen on 17-10-16.
 */

public final class GodModeApplication extends Application {

    public static final String TAG = "GodMode";
    private static GodModeApplication sApplication;

    public GodModeApplication() {
        sApplication = this;
    }

    @Override
    protected void attachBaseContext(Context base) {
        CrashHandler.install(base);
        super.attachBaseContext(base);
    }

    public static GodModeApplication getApplication() {
        return sApplication;
    }

    public static File getBaseDir(Context pContext) {
        String tFStr = String.format("%s/%s", pContext.getFilesDir().getAbsolutePath(), "godmode");
        File dir = new File(tFStr);
        if (dir.exists() || dir.mkdirs()) {
            return dir.getAbsoluteFile();
        }
        throw new IllegalStateException(new FileNotFoundException());
    }

}
