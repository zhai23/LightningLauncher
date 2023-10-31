package com.threethan.launcher.helper;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.threethan.launcher.browser.BrowserActivity;
import com.threethan.launcher.browser.BrowserActivitySeparate;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.support.AddShortcutActivity;
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
    protected static final String ACTION_ACTUALLY_SHORTCUT = "ACTION_ACTUALLY_SHORTCUT";
    public static boolean launchApp(LauncherActivity launcherActivity, ApplicationInfo app) {
        try {
            // Apply any pending preference changes before launching
            launcherActivity.sharedPreferenceEditor.apply();
            // This is unlikely to fail, but it shouldn't stop us from launching if it somehow does
            Keyboard.hide(launcherActivity, launcherActivity.mainView);
        } catch (Exception ignored) {}

        Intent intent = getLaunchIntent(launcherActivity, app);

        if (intent == null) {
            Log.w("AppLaunch", "Package could not be launched (Uninstalled?): "
                    +app.packageName);
            launcherActivity.reloadPackages();
            return false;
        }

        final App.Type appType = App.getType(launcherActivity, app);
        if (SettingsManager.
                getAppLaunchOut(app.packageName) ||
                appType == App.Type.TYPE_VR || appType == App.Type.TYPE_PANEL) {


            if (launcherActivity.browserService != null)
                launcherActivity.browserService.killActivities();
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
        if (Objects.equals(intent.getAction(), ACTION_ACTUALLY_SHORTCUT)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                AddShortcutActivity.launchShortcut(launcherActivity, intent.getStringExtra("json"));
            }
        }
        else launcherActivity.startActivity(intent);
    }

    @Nullable
    public static Intent getLaunchIntent(LauncherActivity activity, ApplicationInfo app) {

        // Ignore apps which don't work or should be excluded
        if (app.packageName.startsWith(activity.getPackageName())) return null;
        if (AppData.invalidAppsList.contains(app.packageName)) return null;

        PackageManager pm = activity.getPackageManager();

        // Detect panel apps
        if (App.getType(activity, app) == App.Type.TYPE_PANEL) {
            String uri = app.packageName;
            if (uri.startsWith(PanelApp.packagePrefix))
                uri = uri.replace(PanelApp.packagePrefix, "");

            Intent panelIntent = new Intent(Intent.ACTION_VIEW);
            panelIntent.setComponent(new ComponentName(
                    "com.oculus.vrshell", "com.oculus.vrshell.MainActivity"));
            panelIntent.setData(Uri.parse(uri));

            // Special case for events, which depends on explore
            if (app.packageName.equals("systemux://events") &&
                    !App.isPackageEnabled(activity, AppData.EXPLORE_PACKAGE)) return null;

            if (pm.resolveActivity(panelIntent, 0) != null) return panelIntent;
            else return null;
        }

        // Detect websites
        if (App.isShortcut(app)) {
            Intent intent = new Intent(ACTION_ACTUALLY_SHORTCUT);
            intent.putExtra("json", app.packageName.replaceFirst("json://", ""));
            return intent;
        }

        // Detect websites
        if (App.isWebsite(app)) {
            Intent intent;
            if (app.packageName.startsWith("http://") || (app.packageName.startsWith("https://"))) {
                // Actual Website
                intent = new Intent(activity, (SettingsManager.getAppLaunchOut(app.packageName)
                        ? BrowserActivitySeparate.class
                        : BrowserActivity.class));

                intent.putExtra("url", app.packageName);
            } else {
                // Non-web intent
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(app.packageName));
            }
            return intent;
        }


        // Otherwise android TV settings is not recognized
        if (Objects.equals(app.packageName, "com.android.tv.settings"))
            return new Intent(android.provider.Settings.ACTION_SETTINGS);

        // Prefer launching as android TV app
        Intent tvIntent = pm.getLeanbackLaunchIntentForPackage(app.packageName);
        if (Platform.isTv(activity) && tvIntent != null) return tvIntent;

        // Chainload for advanced launch options
        if (SettingsManager.getAdvancedLaunching(activity)
                && App.getType(activity, app) == App.Type.TYPE_PHONE &&
                SettingsManager.getAppLaunchOut(app.packageName)) {

            int index = activity.sharedPreferences.getInt(Settings.KEY_LAUNCH_SIZE + app.packageName, 1);
            // Index of 1 is normal launch own
            if (index != 1) {
                Intent chainIntent = new Intent(activity, Settings.launchSizeClasses[index]);
                chainIntent.putExtra("app", app);
                return chainIntent;
            }
        }

        // Get normal launch intent
        final Intent normalIntent = pm.getLaunchIntentForPackage(app.packageName);
        if (normalIntent == null && tvIntent != null) return tvIntent;
        return normalIntent;
    }
}
