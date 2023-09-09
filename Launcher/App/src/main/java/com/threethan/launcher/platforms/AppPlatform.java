package com.threethan.launcher.platforms;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.transition.Slide;
import android.util.Log;

import com.threethan.launcher.MainActivity;
import com.threethan.launcher.web.WebViewActivity;
import com.threethan.launcher.helpers.SettingsManager;

import java.time.temporal.WeekFields;
import java.util.Timer;
import java.util.TimerTask;

public class AppPlatform extends AbstractPlatform {
    @Override
    public boolean launchApp(MainActivity mainActivity, ApplicationInfo appInfo) {
        Intent intent;

        if (isWebsite(appInfo)) {
            intent = new Intent(mainActivity, WebViewActivity.class);
            intent.putExtra("url", appInfo.packageName);
        } else {
            PackageManager pm = mainActivity.getPackageManager();

            if (isVirtualRealityApp(appInfo, mainActivity)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setPackage(appInfo.packageName);
                if (intent.resolveActivity(pm) == null) intent = pm.getLaunchIntentForPackage(appInfo.packageName);
            } else intent = pm.getLaunchIntentForPackage(appInfo.packageName);

        }

        if (intent == null) {
            Log.w("AppPlatform", "Package could not be launched, may have been uninstalled already? " +appInfo.packageName);
            mainActivity.recheckPackages();
            return false;
        }

        if (SettingsManager.getAppLaunchOut(appInfo.packageName) || AbstractPlatform.isVirtualRealityApp(appInfo, mainActivity)) {
            mainActivity.finish();
            mainActivity.overridePendingTransition(0, 0); // Cancel closing animation. Doesn't work on quest, but doesn't hurt

            if (isWebsite(appInfo)) {
                WebViewActivity.killInstances(mainActivity);
            }

            intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION );

            final Intent finalIntent = intent;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    mainActivity.startActivity(finalIntent);
                }
            }, 650);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    mainActivity.startActivity(finalIntent);
                }
            }, 800);
            return false;

        } else {
            mainActivity.getWindow().setExitTransition(new Slide());
            mainActivity.getWindow().setEnterTransition(new Slide());

            mainActivity.startActivity(intent);
            return true;
        }
    }
}
