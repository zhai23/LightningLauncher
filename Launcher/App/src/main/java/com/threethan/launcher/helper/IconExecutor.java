package com.threethan.launcher.helper;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.updater.IconUpdater;

import java.io.File;
import java.util.Objects;

/**
 * Loads app icons asyncronously
 */
public class IconExecutor {
    public static void execute(LauncherActivity activity, ApplicationInfo app, ImageView
            imageView) {
        Thread thread = new Thread(() -> {
            Drawable appIcon = loadIcon(activity, app);
            if (appIcon != null) {
                Icon.cacheIcon(app, appIcon);
                if (imageView != null) activity.runOnUiThread(() -> imageView.setImageDrawable(appIcon));
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    @Nullable
    public static Drawable loadIcon(LauncherActivity activity, ApplicationInfo app) {
        Drawable appIcon = null;

        // Try to load from custom icon file
        final File iconCustomFile = Icon.iconCustomFileForApp(activity, app);
        if (iconCustomFile.exists()) appIcon = Drawable.createFromPath(iconCustomFile.getAbsolutePath());
        if (appIcon != null) return appIcon;

        // Everything in the try will still attempt to download an icon
        try {
            // Try to load from cached icon file
            final File iconCacheFile = Icon.iconCacheFileForPackage(activity, app);
            if (iconCacheFile.exists()) appIcon = Drawable.createFromPath(iconCacheFile.getAbsolutePath());
            if (appIcon != null) return appIcon;

            // Try to load from package manager
            PackageManager packageManager = activity.getPackageManager();
            Resources resources = packageManager.getResourcesForApplication(app);

            // Check Icon
            int iconId = app.icon;
            // Check AndroidTV banner
            if (app.banner != 0 && App.isBanner(app)) iconId = app.banner;

            if (iconId == 0) iconId = android.R.drawable.sym_def_app_icon;
            appIcon = resources.getDrawable(iconId, null);

            return appIcon;
        } catch (PackageManager.NameNotFoundException ignored) { // Fails on websites
            return null;
        }  finally {
            // Attempt to download the icon for this app from an online repo
            // Done AFTER saving the drawable version to prevent a race condition)
            IconUpdater.check(activity, app, () ->
                    activity.launcherService.forEachActivity(a ->
                            Objects.requireNonNull(a.getAppAdapter()).notifyItemChanged(app)));
        }
    }
}
