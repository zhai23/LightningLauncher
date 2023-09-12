package com.threethan.launcher.helper;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.threethan.launcher.browser.BrowserActivity;
import com.threethan.launcher.browser.BrowserActivitySeparate;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.support.SettingsManager;

import java.util.Timer;
import java.util.TimerTask;

public abstract class Launch {
    public static boolean launchApp(LauncherActivity launcherActivity, ApplicationInfo app) {
        Intent intent = null;

        if (App.isWebsite(app)) {
            intent = new Intent(launcherActivity, (SettingsManager.getAppLaunchOut(app.packageName)
                    ? BrowserActivitySeparate.class : BrowserActivity.class));
            intent.putExtra("url", app.packageName);
        } else {
            PackageManager pm = launcherActivity.getPackageManager();

            // Getting the main intent instead of default launch intent fixes oculus browser
            if (App.isVirtualReality(app, launcherActivity)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setPackage(app.packageName);
                if (intent.resolveActivity(pm) == null)
                    intent = pm.getLaunchIntentForPackage(app.packageName);
            }
        }

        if (intent == null) {
            Log.w("AppPlatform", "Package could not be launched (Uninstalled?): " +app.packageName);
            launcherActivity.recheckPackages();
            return false;
        }

        if (SettingsManager.getAppLaunchOut(app.packageName) || App.isVirtualReality(app, launcherActivity)) {
            if (App.isWebsite(app))
                try {
                    launcherActivity.wService.killActivities();
                } catch (Exception ignored) {}

            launcherActivity.finish();
            launcherActivity.finishAffinity();

            intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );

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
            launcherActivity.startActivity(intent);
            return true;
        }
    }
}
