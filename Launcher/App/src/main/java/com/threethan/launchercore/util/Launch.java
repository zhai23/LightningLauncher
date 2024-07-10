package com.threethan.launchercore.util;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

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
        if (app.packageName.equals(Core.context().getPackageName())) return null;
        if (Platform.excludedPackageNames.contains(app.packageName)) return null;

        PackageManager pm = Core.context().getPackageManager();

        if (Platform.isQuest() && App.getType(app) == App.Type.PANEL) {
            Intent intent = pm.getLaunchIntentForPackage("com.oculus.vrshell");
            assert intent != null;
            intent.setData(Uri.parse("apk://"+app.packageName));
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        Core.context().startActivity(intent);
    }
    /** Launch an app - in it's own window, if applicable */
    public static void launchInOwnWindow(ApplicationInfo app, Activity activity) {
        if (App.getType(app).equals(App.Type.VR) || App.getType(app).equals(App.Type.PANEL)
        || app instanceof UtilityApplicationInfo) {
            launch(app);
            return;
        }
        activity.finishAffinity();
        DelayLib.delayed(() -> launch(app), 50);
        DelayLib.delayed(() -> launch(app), 700);
    }
}
