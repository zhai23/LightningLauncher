package com.threethan.launcher.platforms;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.util.Log;

import com.threethan.launcher.MainActivity;
import com.threethan.launcher.helpers.SettingsManager;

import java.util.Timer;
import java.util.TimerTask;

public class AppPlatform extends AbstractPlatform {
    @Override
    public boolean runApp(MainActivity mainActivity, ApplicationInfo appInfo) {
        Intent launchIntent = mainActivity.getPackageManager().getLaunchIntentForPackage(appInfo.packageName);

        if (SettingsManager.getAppLaunchOut(appInfo.packageName) || AbstractPlatform.isVirtualRealityApp(appInfo, mainActivity)) {
            mainActivity.finish();
            mainActivity.overridePendingTransition(0, 0); // Cancel closing animation. Doesn't work on quest, but doesn't hurt
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
                        mainActivity.startActivity(browserIntent);
                    } catch (Exception e) {
                        mainActivity.startActivity(launchIntent);
                    }
                }
            }, 650);
            return false;
        } else {
            if (isWebsite(appInfo)) {
                String url = appInfo.packageName;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                mainActivity.startActivity(i);
                return true;
            } else if (launchIntent != null ){
                mainActivity.startActivity(launchIntent);
                return true;
            } else {
                Log.w("AppPlatform", "Package could not be launched, may have been uninstalled " +appInfo.packageName);
                mainActivity.recheckPackages();
                return false;
            }
        }
    }
}
