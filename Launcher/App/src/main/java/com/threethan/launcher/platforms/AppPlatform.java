package com.threethan.launcher.platforms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.threethan.launcher.SettingsProvider;

import java.util.Timer;
import java.util.TimerTask;

public class AppPlatform extends AbstractPlatform {
    @Override
    public boolean isSupported(Context context) {
        return true;
    }

    @Override
    public void runApp(Activity context, ApplicationInfo appInfo) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appInfo.packageName);
        if (SettingsProvider.getAppLaunchOut(appInfo.packageName)) {
            context.finish();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    context.startActivity(launchIntent);
                }
            }, 650);
        } else {
            context.startActivity(launchIntent);
        }
    }
}
