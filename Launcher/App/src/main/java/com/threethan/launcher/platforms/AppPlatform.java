package com.threethan.launcher.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

public class AppPlatform extends AbstractPlatform {
    @Override
    public boolean isSupported(Context context) {
        return true;
    }

    @Override
    public void runApp(Activity context, ApplicationInfo appInfo) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
        context.finish();
        Log.i("LAUNCHING",appInfo.packageName);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                context.startActivity(launchIntent);
            }
        }, 600);
        // Backup, in case the first call fails
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                context.startActivity(launchIntent);
            }
        }, 1500);
    }
}
