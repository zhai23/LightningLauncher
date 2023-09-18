package com.threethan.launcher.helper;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.threethan.launcher.browser.BrowserActivity;
import com.threethan.launcher.browser.BrowserActivitySeparate;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.support.SettingsManager;

import java.util.Timer;
import java.util.TimerTask;

public abstract class Launch {
    public static boolean launchApp(LauncherActivity launcherActivity, ApplicationInfo app) {
        Intent intent = getLaunchIntent(launcherActivity, app);

        if (intent == null) {
            Log.w("AppPlatform", "Package could not be launched (Uninstalled?): "
                    +app.packageName);
            launcherActivity.recheckPackages();
            return false;
        }

        if (SettingsManager.getAppLaunchOut(app.packageName) ||
                App.isVirtualReality(app, launcherActivity)) {
            // Launch in own window properly
            if (App.isWebsite(app))
                try {
                    launcherActivity.wService.killActivities();
                } catch (Exception ignored) {
                }

            launcherActivity.launcherService.finishAllActivities();
            if (launcherActivity != null) launcherActivity.finishAndRemoveTask();

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    startIntent(launcherActivity, intent);
                }
            }, 650);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    startIntent(launcherActivity, intent);
                }
            }, 800);
            return false;
        } else {
            startIntent(launcherActivity, intent);
            return true;
        }
    }

    private static void startIntent(LauncherActivity launcherActivity, Intent intent) {
        launcherActivity.startActivity(intent);
    }


    @Nullable
    public static Intent getLaunchIntent(LauncherActivity activity, ApplicationInfo app) {
        if (App.isWebsite(app)) {
            Intent intent = new Intent(activity, (SettingsManager.getAppLaunchOut(app.packageName)
                    ? BrowserActivitySeparate.class : BrowserActivity.class));
            intent.putExtra("url", app.packageName);
            return intent;
        }

        PackageManager pm = activity.getPackageManager();

        Intent questIntent = new Intent();
        questIntent.setAction("com.oculus.vrshell.SHELL_MAIN");
        questIntent.setPackage(app.packageName);
        if (questIntent.resolveActivity(pm) != null) return questIntent;

        // Get launch intent
        return pm.getLaunchIntentForPackage(app.packageName);
    }
}
