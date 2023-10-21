package com.threethan.launcher.helper;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.threethan.launcher.browser.BrowserActivity;
import com.threethan.launcher.browser.BrowserActivitySeparate;
import com.threethan.launcher.launcher.LauncherActivity;
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

        final App.Type appType = App.getType(launcherActivity, app);
        if (SettingsManager.
                getAppLaunchOut(app.packageName) ||
                appType == App.Type.TYPE_VR || appType == App.Type.TYPE_PANEL) {
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
        launcherActivity.startService(intent);
    }

    @Nullable
    public static Intent getLaunchIntent(LauncherActivity activity, ApplicationInfo app) {

        // Ignore apps which don't work or should be excluded
        if (app.packageName.startsWith("com.threethan.launcher")) return null;
        if (app.packageName.equals("com.oculus.browser")) return null;



        // Detect panel apps
        if (App.getType(activity, app) == App.Type.TYPE_PANEL) {
            Intent panelIntent = new Intent(Intent.ACTION_VIEW);
            panelIntent.setComponent(new ComponentName(
                    "com.oculus.vrshell", "com.oculus.vrshell.MainActivity"));
            panelIntent.setData(Uri.parse(app.packageName));
            return panelIntent;
        }

        // Detect websites
        if (App.isWebsite(app)) {
            Intent intent = new Intent(activity, (SettingsManager.getAppLaunchOut(app.packageName)
                    ?  BrowserActivitySeparate.class
                    : BrowserActivity.class));
            intent.putExtra("url", app.packageName);
            return intent;
        }

        PackageManager pm = activity.getPackageManager();

        // Otherwise android TV settings is not recognized
        if (Objects.equals(app.packageName, "com.android.tv.settings"))
            return new Intent(android.provider.Settings.ACTION_SETTINGS);


        // Prefer launching as android TV app
        Intent tvIntent = pm.getLeanbackLaunchIntentForPackage(app.packageName);
        if (Platform.isTv(activity) && tvIntent != null) return tvIntent;

        // Get normal launch intent
        final Intent normalIntent = pm.getLaunchIntentForPackage(app.packageName);
        if (normalIntent == null && tvIntent != null) return tvIntent;
        return normalIntent;
    }
}
