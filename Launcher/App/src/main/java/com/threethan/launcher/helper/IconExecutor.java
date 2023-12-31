package com.threethan.launcher.helper;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.threethan.launcher.launcher.LauncherActivity;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
    BackgroundTask

    This task loads an image background asynchronously, then sets it to the specified view when done
    Used only by LauncherActivity
 */

class IconExecutor {

    public static void execute(LauncherActivity activity, ApplicationInfo app, ImageView
            imageView) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            Drawable appIcon = loadIcon(activity, app, imageView);
            if (appIcon != null) {
                Icon.cacheIcon(app, appIcon);
                activity.runOnUiThread(() -> imageView.setImageDrawable(appIcon));
            }
        });
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    @Nullable
    public static Drawable loadIcon(LauncherActivity activity, ApplicationInfo app, ImageView
    imageView) {
        Drawable appIcon = null;

        // Try to load from custom icon file
        final File iconCustomFile = Icon.iconCustomFileForApp(activity, app);
        if (iconCustomFile.exists()) appIcon = Drawable.createFromPath(iconCustomFile.getAbsolutePath());
        if (appIcon != null) return appIcon;

        // Try to load from cached icon file
        final File iconCacheFile = Icon.iconCacheFileForPackage(activity, app);
        if (iconCacheFile.exists()) appIcon = Drawable.createFromPath(iconCustomFile.getAbsolutePath());
        if (appIcon != null) return appIcon;

        // Try to load from package manager
        try {
            PackageManager packageManager = activity.getPackageManager();
            Resources resources = packageManager.getResourcesForApplication(app);

            // Check Icon
            int iconId = app.icon;
            // Check AndroidTV banner
            if (app.banner != 0 && App.isBanner(app)) iconId = app.banner;

            if (iconId == 0) iconId = android.R.drawable.sym_def_app_icon;
            appIcon = resources.getDrawable(iconId, activity.getTheme());

            return appIcon;
        } catch (Exception ignored) {} // Fails on web apps, possibly also on invalid packages

        // Attempt to download the icon for this app from an online repo
        // Done AFTER saving the drawable version to prevent a race condition)
        IconRepo.check(activity, app, () -> loadIcon(activity, app, imageView));
        return null;
    }
}
