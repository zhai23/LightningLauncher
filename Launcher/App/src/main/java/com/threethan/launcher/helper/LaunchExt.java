package com.threethan.launcher.helper;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;
import com.threethan.launcher.activity.AddShortcutActivity;
import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launcher.activity.dialog.BasicDialog;
import com.threethan.launcher.activity.support.SettingsManager;
import com.threethan.launcher.data.Settings;
import com.threethan.launcher.updater.BrowserUpdater;
import com.threethan.launchercore.lib.DelayLib;
import com.threethan.launchercore.util.App;
import com.threethan.launchercore.util.Keyboard;
import com.threethan.launchercore.util.Launch;

import java.util.Objects;

/**
 * This abstract class is dedicated to actually launching apps.
 * <p>
 * The helper function "getAppLaunchable" is also used by the App class to determine if an app
 * can possibly be launched.
 */
public abstract class LaunchExt extends Launch {
    protected static final String ACTION_ACTUALLY_SHORTCUT = "ACTION_ACTUALLY_SHORTCUT";
    // Must match: arrays.xml -> advance_launch_browsers
    private static final int LAUNCH_BROWSER_SYSTEM = 1;
    private static final int LAUNCH_BROWSER_QUEST = 2;

    /**
     * Launches a given app, checking various configuration options in the process
     * @param launcherActivity The activity to launch from
     * @param app The app to launch
     * @return True if the app was launched
     */
    public static boolean launchApp(LauncherActivity launcherActivity, ApplicationInfo app) {
        // Apply any pending preference changes before launching
        try {
            // This is unlikely to fail, but it shouldn't stop us from launching if it somehow does
            Keyboard.hide(launcherActivity, launcherActivity.mainView);
        } catch (Exception ignored) {}

        if (!app.enabled) {
            AppExt.openInfo(launcherActivity, app.packageName);
            return false;
        }

        Intent intent = getIntentForLaunch(launcherActivity, app);

        if (intent == null) {
            Log.w("AppLaunch", "Package could not be launched (Uninstalled?): "
                    +app.packageName);
            launcherActivity.refreshPackages();
            return false;
        }

        // Browser Check
        if (Objects.equals(intent.getPackage(), PlatformExt.BROWSER_PACKAGE)) {
            if (PlatformExt.hasBrowser(launcherActivity)) {
                // Check for browser update. User probably won't see the prompt until closing, though.
                BrowserUpdater browserUpdater = new BrowserUpdater(launcherActivity);
                if (browserUpdater.getInstalledVersionCode() < BrowserUpdater.REQUIRED_VERSION_CODE) {
                    // If browser is required, but not installed
                    // Prompt installation
                    AlertDialog dialog = new BasicDialog<>(launcherActivity, R.layout.dialog_prompt_browser_update).show();
                    if (dialog == null) return false;
                    dialog.findViewById(R.id.cancel).setOnClickListener((view) -> dialog.dismiss());
                    dialog.findViewById(R.id.install).setOnClickListener((view) -> {
                        new BrowserUpdater(launcherActivity).checkAppUpdateAndInstall();
                        BasicDialog.toast(launcherActivity.getString(R.string.download_browser_toast_main),
                                launcherActivity.getString(R.string.download_browser_toast_bold), true);
                    });
                    return false;
                }
            } else {
                // If browser is required, but not installed
                // Prompt installation
                AlertDialog dialog = new BasicDialog<>(launcherActivity, R.layout.dialog_prompt_browser_install).show();
                if (dialog == null) return false;
                dialog.findViewById(R.id.cancel).setOnClickListener((view) -> dialog.dismiss());
                dialog.findViewById(R.id.install).setOnClickListener((view) -> {
                    new BrowserUpdater(launcherActivity).checkAppUpdateAndInstall();
                    BasicDialog.toast(launcherActivity.getString(R.string.download_browser_toast_main),
                            launcherActivity.getString(R.string.download_browser_toast_bold), true);
                });
                return false;
            }
        }

        if (SettingsManager.getAppLaunchSize(app.packageName) > 0) {
            Intent chain = getIntentForLaunch(launcherActivity, app);
            DelayLib.delayed(() -> launcherActivity.startActivity(chain), 50);
            launcherActivity.finishAffinity();
        } else if (SettingsManager.getAppLaunchOut(app.packageName)) {
            launchInOwnWindow(app, launcherActivity);
        } else {
            startIntent(launcherActivity, intent);
        }
        return true;
    }
    private static void startIntent(LauncherActivity launcherActivity, Intent intent) {
        if (Objects.equals(intent.getAction(), ACTION_ACTUALLY_SHORTCUT)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            AddShortcutActivity.launchShortcut(launcherActivity, intent.getStringExtra("json"));
        else launcherActivity.startActivity(intent);
    }

    /**
     * Gets the intent used to actually launch the given app,
     * including workarounds for browsers & panel apps
     */
    @Nullable
    private static Intent getIntentForLaunch(LauncherActivity activity, ApplicationInfo app) {
        // Ignore apps which don't work or should be excluded
        if (app.packageName.startsWith(activity.getPackageName())) return null;

        // Detect websites
        if (App.isWebsite(app.packageName)) {
            Intent intent;
            if (app.packageName.startsWith("http://") || (app.packageName.startsWith("https://"))) {
                final int browserIndex
                        = activity.dataStoreEditor.getInt(Settings.KEY_LAUNCH_BROWSER + app.packageName,
                        activity.dataStoreEditor.getInt(Settings.KEY_DEFAULT_BROWSER, 0));
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(app.packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // Reference: arrays.xml -> advance_launch_browsers
                switch (browserIndex) {
                    case (LAUNCH_BROWSER_QUEST):
                        intent.setPackage("com.oculus.browser");
                        intent.setComponent(
                                new ComponentName("com.oculus.browser",
                                        "com.oculus.browser.PanelActivity"));
                        break;
                    case (LAUNCH_BROWSER_SYSTEM):
                        break;
                    default:
                        intent.setPackage(PlatformExt.BROWSER_PACKAGE);
                        break;
                }
                return intent;
            } else {
                // Non-web intent
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(app.packageName));
            }
            return intent;
        }

        Intent li = getLaunchIntent(app);
        // Chain-load for advanced launch options
        if (AppExt.getType(app) == App.Type.PHONE && li != null) {
            int index = SettingsManager.getAppLaunchSize(app.packageName);
            if (index > 0) {
                Intent chainIntent = new Intent(activity, Settings.launchSizeClasses[index]);
                chainIntent.putExtra("app", app);
                return chainIntent;
            }
        }

        return li;
    }
}
