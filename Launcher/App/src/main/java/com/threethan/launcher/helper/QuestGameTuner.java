package com.threethan.launcher.helper;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;
import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launcher.activity.dialog.BasicDialog;
import com.threethan.launcher.activity.view.ViewFlinger;
import com.threethan.launchercore.Core;
import com.threethan.launchercore.util.App;

import java.util.Random;

/**
 * Allows direct opening of Quest Game Tuner tuning settings for a specific app
 */
public class QuestGameTuner {
    private static final String APP_TUNING_URI = "tune_app";
    private static final String APP_USAGE_URI = "usage_app";
    public static final String PKG_NAME = "com.threethan.tuner";

    /** Opens Quest Game Tuner tuning for a given package */
    public static void tuneApp(String packageName) {
        if (isInstalled()) {
            if (packageName == null || packageName.isEmpty()) return;
            Intent intent = new Intent();
            intent.setPackage(PKG_NAME);
            intent.setComponent(new ComponentName("com.threethan.tuner",
                    "com.threethan.tuner.dialog.DialogActivity"));
            intent.setAction("com.threethan.tuning.DIALOG");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse(APP_TUNING_URI));
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);
            Core.context().startActivity(intent);
        } else {
            openInfoDialog();
        }
    }


    /** Opens Quest Game Tuner charts for a given package */
    public static void chartApp(String packageName) {
        if (isInstalled()) {
            if (packageName == null || packageName.isEmpty()) return;
            if (packageName.equals(PKG_NAME)) {
                openTunerOptions();
                return;
            }
            Intent intent = new Intent();
            intent.setPackage("com.threethan.tuner");
            intent.setComponent(new ComponentName("com.threethan.tuner",
                    "com.threethan.tuner.dialog.DialogActivity"));
            intent.setAction("com.threethan.tuning.DIALOG");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse(APP_USAGE_URI));
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);

            if (getVersionCode() < 150)
                BasicDialog.toast("Update Quest Game Tuner");
            else
                Core.context().startActivity(intent);
        } else {
            openInfoDialog();
        }
    }

    /** Opens Quest Game Tuner to its options screen */
    private static void openTunerOptions() {
        Intent intent = Core.context().getPackageManager().getLaunchIntentForPackage(PKG_NAME);
        if (intent == null) return;
        intent.putExtra("config", true);
        LaunchExt.launchInOwnWindow(intent, LauncherActivity.getForegroundInstance(), false);
    }

    /** (Tries) to apply tuning for an app with Quest Game Tuner. */
    public static void applyTuning(String packageName) {
        if (!isInstalled()) return;
        if (getVersionCode() < 150) return;
        if (packageName == null || packageName.isEmpty()) return;
        Intent intent = new Intent("com.threethan.tuner.APPLY");
        intent.setPackage(PKG_NAME);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);

        Core.context().sendBroadcast(intent);
    }

    /** Gets the numeric version code of the currently installed Quest Game Tuner */
    private static int getVersionCode() {
        PackageInfo packageInfo;
        try {
            packageInfo = Core.context().getPackageManager().getPackageInfo(
                    PKG_NAME, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return 0;
        }
        return packageInfo.versionCode;
    }
    /** Checks if Quest Game Tuner is installed */
    public static boolean isInstalled() {
        return App.packageExists(PKG_NAME);
    }

    /** Opens a dialog with info about getting Quest Game Tuner */
    public static void openInfoDialog() {
        if (LauncherActivity.getForegroundInstance() == null) return;
        new InfoDialog(LauncherActivity.getForegroundInstance()).show();
    }
    private static class InfoDialog extends BasicDialog<Context> {
        public InfoDialog(Context context) {
            super(context, R.layout.dialog_tuner_info);
        }

        @Nullable
        @Override
        public AlertDialog show() {
            AlertDialog dialog = super.show();
            if (dialog == null) return null;
            dialog.findViewById(R.id.dismissButton).setOnClickListener(v -> dialog.dismiss());
            dialog.findViewById(R.id.root).setClipToOutline(true);
            final View.OnClickListener openListener = v ->
                    LaunchExt.launchUrl(null,
                            Core.context().getString(R.string.tuner_info_get_link), true);

            dialog.findViewById(R.id.purchaseButton).setOnClickListener(openListener);
            dialog.findViewById(R.id.topBanner).setOnClickListener(openListener);
            ViewFlinger imageBanner = dialog.findViewById(R.id.imageBanner);
            for (int i = 0; i < imageBanner.getChildCount(); i++)
                imageBanner.getChildAt(i).setOnClickListener(openListener);
            imageBanner.setAutoAdvance(true);
            imageBanner.setCurrentScreen(new Random().nextInt() % imageBanner.getScreenCount());
            return dialog;
        }
    }
}
