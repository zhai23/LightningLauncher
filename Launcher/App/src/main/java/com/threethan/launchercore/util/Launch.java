package com.threethan.launchercore.util;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;

import com.threethan.launchercore.Core;
import com.threethan.launchercore.adapter.UtilityApplicationInfo;
import com.threethan.launchercore.lib.DelayLib;

import java.util.Objects;

/** @noinspection unused*/
public abstract class Launch {

    /**
     * Gets the intent to launch a given app
     * @param app App to launch
     * @return Intent to launch if found, null if failed
     */
    @Nullable
    public static Intent getLaunchIntent(ApplicationInfo app) {
        if (app.packageName.startsWith(Core.context().getPackageName())) return null;
        if (Platform.excludedPackageNames.contains(app.packageName)) return null;
        PackageManager pm = Core.context().getPackageManager();

        if (Platform.isQuest() && App.getType(app) == App.Type.PANEL) {
            Intent intent = pm.getLaunchIntentForPackage("com.oculus.vrshell");
            assert intent != null;
            intent.setData(Uri.parse(app.packageName));
            return intent;
        }

        if (App.getType(app) == App.Type.WEB) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(app.packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }

        final Intent defaultIntent  = pm.getLaunchIntentForPackage(app.packageName);
        final Intent leanbackIntent = pm.getLeanbackLaunchIntentForPackage(app.packageName);
        if (Platform.isQuest()) {
            final Intent intent = defaultIntent != null ? defaultIntent : leanbackIntent;
            if (intent != null) intent.setAction("com.oculus.vrshell.intent.action.LAUNCH");
            return intent;
        } else if (Platform.isTv()) {
            if (Objects.equals(app.packageName, "com.android.tv.settings"))
                return new Intent(android.provider.Settings.ACTION_SETTINGS);
            return leanbackIntent != null ? leanbackIntent : defaultIntent;
        } else {
            return defaultIntent != null ? defaultIntent : leanbackIntent;
        }
    }
    /** Launch an app from the core context as a new task */
    public static void launch(ApplicationInfo app) {
        Log.i(Core.TAG, "Launching "+app.packageName);
        if (app instanceof UtilityApplicationInfo utilityApplicationInfo) {
            utilityApplicationInfo.launch();
            return;
        }
        Intent intent = getLaunchIntent(app);
        if (intent == null) return;
        if (!Platform.isTv()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        Core.context().startActivity(intent);
    }
    /** Launch an app - in it's own window, if applicable */
    public static void launchInOwnWindow(ApplicationInfo app, Activity activity) {
        launchInOwnWindow(app, activity, true);
    }

    /** Launch an app - in it's own window, if applicable */
    public static void launchInOwnWindow(ApplicationInfo app, Activity activity,
                                         boolean allowNewVrOsMultiWindow) {
        Runnable onPostDestroy = () -> {
            launch(app);
            if (Platform.supportsNewVrOsMultiWindow() && allowNewVrOsMultiWindow) {
                PackageManager pm = Core.context().getPackageManager();
                Intent relaunch = pm.getLaunchIntentForPackage(activity.getPackageName());
                DelayLib.delayed(() -> activity.startActivity(relaunch), 550);
            } else if (Platform.isVr()) {
                DelayLib.delayed(() -> launch(app));
                DelayLib.delayed(() -> activity.startActivity(getLaunchIntent(app)));
            }
        };
        if (!Platform.isVr()) {
            onPostDestroy.run();
        } else if (activity instanceof LaunchingActivity launchingActivity) {
            launchingActivity.setOnPostDestroy(onPostDestroy);
            launchingActivity.finishAffinity();
        } else {
            DelayLib.delayed(onPostDestroy, 50);
            activity.finishAffinity();
        }
    }

    /** Launch a custom activity intent - in it's own window, if applicable */
    public static void launchInOwnWindow(Intent intent, Activity activity,
                                         boolean allowNewVrOsMultiWindow) {
        Runnable onPostDestroy = () -> {
            activity.startActivity(intent);
            if (Platform.supportsNewVrOsMultiWindow() && allowNewVrOsMultiWindow) {
                PackageManager pm = Core.context().getPackageManager();
                Intent relaunch = pm.getLaunchIntentForPackage(activity.getPackageName());
                DelayLib.delayed(() -> activity.startActivity(relaunch), 550);
            } else if (Platform.isVr()) {
                DelayLib.delayed(() -> activity.startActivity(intent));
                DelayLib.delayed(() -> activity.startActivity(intent), 2000);
            }
        };
        if (!Platform.isVr()) {
            onPostDestroy.run();
        } else if (activity instanceof LaunchingActivity launchingActivity) {
            launchingActivity.setOnPostDestroy(onPostDestroy);
            launchingActivity.finishAffinity();
        } else {
            DelayLib.delayed(onPostDestroy, 50);
            activity.finishAffinity();
        }
    }
    /** A subclass of androidx ComponentActivity which supports launching vrOS apps without delay */
    public static class LaunchingActivity extends ComponentActivity {
        private Runnable onPostDestroy = null;

        /** Sets a runnable to be called after the activity is destroyed */
        protected void setOnPostDestroy(Runnable onPostDestroy) {
            this.onPostDestroy = onPostDestroy;
        }
        @Override
        protected void onDestroy() {
            super.onDestroy();
            if (onPostDestroy != null) new Handler().post(onPostDestroy);
        }
    }
}
