package com.threethan.launcher.platforms;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

import com.threethan.launcher.MainActivity;
import com.threethan.launcher.SettingsProvider;

import java.util.Timer;
import java.util.TimerTask;

public class AppPlatform extends AbstractPlatform {
    @Override
    public void runApp(Activity context, ApplicationInfo appInfo) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appInfo.packageName);

        if (SettingsProvider.getAppLaunchOut(appInfo.packageName) || AbstractPlatform.isVirtualRealityApp(appInfo, (MainActivity) context)) {
            context.finish();
            context.overridePendingTransition(0, 0); // Cancel closing animation. Doesn't work on quest, but doesn't hurt
            assert launchIntent != null;
            launchIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            );
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // Try to run a main intent, if it exists. This fixes oculus browser, possibly others
                        Intent browserIntent = new Intent(Intent.ACTION_MAIN);
                        browserIntent.setPackage(launchIntent.getPackage());
                        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(browserIntent);
                    } catch (Exception e) {
                        context.startActivity(launchIntent);
                    }
                }
            }, 650);
        } else {
            if (launchIntent != null) context.startActivity(launchIntent);
        }
    }
}
