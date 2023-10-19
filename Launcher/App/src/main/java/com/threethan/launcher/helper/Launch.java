package com.threethan.launcher.helper;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.threethan.launcher.browser.BrowserActivity;
import com.threethan.launcher.browser.BrowserActivitySeparate;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.launcher.LauncherService;
import com.threethan.launcher.support.SettingsManager;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/*
    Launch

    This abstract class is dedicated to actually launching apps.

    The helper function "getAppLaunchIntent" is also used by the App class to determine if an app
    can possibly be launched.
 */

public abstract class Launch {
    public static boolean launchApp(LauncherActivity launcherActivity, ApplicationInfo app) {
        try {
            // Apply any pending preference changes before launching
            launcherActivity.sharedPreferenceEditor.apply();
            // This is unlikely to fail, but it shouldn't stop us from launching if it somehow does
            Keyboard.hide(launcherActivity, launcherActivity.mainView);
        } catch (Exception ignored) {}

        Intent intent = getLaunchIntent(launcherActivity, app);

        if (intent == null) {
            Log.w("AppPlatform", "Package could not be launched (Uninstalled?): "
                    +app.packageName);
            launcherActivity.recheckPackages();
            return false;
        }

        if (SettingsManager.
                getAppLaunchOut(app.packageName) ||
                App.isVirtualReality(app, launcherActivity)) {
            // Launch in own window properly
            if (App.isWebsite(app))
                try {
                    launcherActivity.browserService.killActivities();
                } catch (Exception ignored) {}

            launcherActivity.launcherService.finishAllActivities();

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK );

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
                    ?  BrowserActivitySeparate.class
                    : BrowserActivity.class));
            intent.putExtra("url", app.packageName);
            return intent;
        }

        PackageManager pm = activity.getPackageManager();

        Intent questIntent = new Intent();
        questIntent.setAction("com.oculus.vrshell.SHELL_MAIN");
        questIntent.setPackage(app.packageName);
        if (questIntent.resolveActivity(pm) != null) return questIntent;

        // Otherwise android TV settings is not recognized
        if (Objects.equals(app.packageName, "com.android.tv.settings")) {
            return new Intent(android.provider.Settings.ACTION_SETTINGS);
        }

        // Prefer launching as android TV app
        Intent tvIntent = pm.getLeanbackLaunchIntentForPackage(app.packageName);
        if (Platform.isTv(activity) && tvIntent != null) return tvIntent;

        // Get normal launch intent
        final Intent normalIntent = pm.getLaunchIntentForPackage(app.packageName);
        if (normalIntent == null && tvIntent != null) return tvIntent;
        return normalIntent;
    }
}
