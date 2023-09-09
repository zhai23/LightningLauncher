package com.threethan.launcher.helper;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.transition.Slide;
import android.util.Log;

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.browser.BrowserActivity;
import com.threethan.launcher.support.SettingsManager;

import java.util.Timer;
import java.util.TimerTask;

public abstract class Launch {
    public static boolean launchApp(LauncherActivity launcherActivity, ApplicationInfo appInfo) {
        Intent intent;

        if (App.isWebsite(appInfo)) {
            intent = new Intent(launcherActivity, BrowserActivity.class);
            intent.putExtra("url", appInfo.packageName);
        } else {
            PackageManager pm = launcherActivity.getPackageManager();

            if (App.isVirtualReality(appInfo, launcherActivity)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setPackage(appInfo.packageName);
                if (intent.resolveActivity(pm) == null) intent = pm.getLaunchIntentForPackage(appInfo.packageName);
            } else intent = pm.getLaunchIntentForPackage(appInfo.packageName);

        }

        if (intent == null) {
            Log.w("AppPlatform", "Package could not be launched, may have been uninstalled already? " +appInfo.packageName);
            launcherActivity.recheckPackages();
            return false;
        }

        if (SettingsManager.getAppLaunchOut(appInfo.packageName) || App.isVirtualReality(appInfo, launcherActivity)) {
            launcherActivity.finish();
            launcherActivity.overridePendingTransition(0, 0); // Cancel closing animation. Doesn't work on quest, but doesn't hurt

            if (App.isWebsite(appInfo)) {
                BrowserActivity.killInstances(launcherActivity);
            }

            intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION );

            final Intent finalIntent = intent;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    launcherActivity.startActivity(finalIntent);
                }
            }, 650);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    launcherActivity.startActivity(finalIntent);
                }
            }, 800);
            return false;

        } else {
            launcherActivity.getWindow().setExitTransition(new Slide());
            launcherActivity.getWindow().setEnterTransition(new Slide());

            launcherActivity.startActivity(intent);
            return true;
        }
    }
}
