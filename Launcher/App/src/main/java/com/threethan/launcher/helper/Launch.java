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

        if (SettingsManager.getAppLaunchOut(app.packageName) || App.isVirtualReality(app,
                launcherActivity)) {
            // Launch in own window properly
            if (App.isWebsite(app))
                try {
                    launcherActivity.wService.killActivities();
                } catch (Exception ignored) {
                }

            try {
                launcherActivity.launcherService.finishAllActivities();
            } catch (Exception ignored) {
            }
            try {
                launcherActivity.finishAndRemoveTask();
            } catch (Exception ignored) {
            }

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

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
    @Nullable
    public static Intent getLaunchIntent(LauncherActivity activity, ApplicationInfo app) {
        if (App.isWebsite(app)) {
            Intent intent = new Intent(activity, (SettingsManager.getAppLaunchOut(app.packageName)
                    ? BrowserActivitySeparate.class : BrowserActivity.class));
            intent.putExtra("url", app.packageName);
            return intent;
        }

        // Get pm
        PackageManager pm = activity.getPackageManager();

        // TODO: Why no work?
//        Intent questIntent = new Intent("com.oculus.vrshell.SHELL_MAIN");
//        questIntent.setPackage(app.packageName);
//        if (questIntent.resolveActivity(pm) != null) {
//            Log.v("QUEST INTENT FOUND FOR PKG", app.packageName);
//            return questIntent;
//        }

        if (App.isVirtualReality(app, activity)) {
            // Get main intent
            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.setPackage(app.packageName);
            if (mainIntent.resolveActivity(pm) == null) return mainIntent;
        }

        // Get launch intent
        Intent launchIntent = pm.getLaunchIntentForPackage(app.packageName);
        if (launchIntent != null) return launchIntent;


//

        return null;
    }
}
