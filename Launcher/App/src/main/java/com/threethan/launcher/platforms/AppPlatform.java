package com.threethan.launcher.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
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
        if (SettingsProvider.getAppLaunchOut(appInfo.packageName) || AbstractPlatform.isVirtualRealityApp(appInfo)) {
            context.finish();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    context.startActivity(launchIntent);
                }
            }, 615);
        } else {
            context.startActivity(launchIntent);
        }
    }
}
