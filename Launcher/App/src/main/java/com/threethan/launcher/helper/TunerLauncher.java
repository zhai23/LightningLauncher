package com.threethan.launcher.helper;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;

import com.threethan.launchercore.Core;
import com.threethan.launchercore.util.App;

/**
 * Allows direct opening of Quest Game Tuner tuning settings for a specific app
 */
public class TunerLauncher {
    private static final String APP_TUNING_URI = "tune_app";

    /** Opens Quest Game Tuner for a given app */
    public static void openForApp(ApplicationInfo app) {
        openForPackage(app.packageName);
    }
    /** Opens Quest Game Tuner for a given package */
    public static void openForPackage(String packageName) {
        if (App.packageExists("com.threethan.tuner")) {
            if (packageName == null || packageName.isEmpty()) return;
            Intent intent = new Intent();
            intent.setPackage("com.threethan.tuner");
            intent.setComponent(new ComponentName("com.threethan.tuner", "com.threethan.tuner.dialog.DialogActivity"));
            intent.setAction("com.threethan.tuning.DIALOG");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse(APP_TUNING_URI));
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);
            Core.context().startActivity(intent);
        } else {
            LaunchExt.launchUrl(null, "https://threethan.itch.io/quest-game-tuner");
        }
    }
}
