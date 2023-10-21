package com.threethan.launcher.helper;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.threethan.launcher.R;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.lib.StringLib;
import com.threethan.launcher.support.SettingsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
    Icon

    This abstract class provides helper functions for getting, setting and saving icons.
    It uses IconRepo in order to request icons from the internet, if applicable
 */

public abstract class Icon {
    public static final Map<String, Drawable> cachedIcons = new ConcurrentHashMap<>();
    public static File iconFileForPackage(LauncherActivity launcherActivity, String packageName) {
        packageName = cacheName(StringLib.toValidFilename(packageName));
        ApplicationInfo tempApp = new ApplicationInfo();
        tempApp.packageName = packageName;
        final boolean wide = App.isBanner(launcherActivity, tempApp);
        return new File(launcherActivity.getApplicationInfo().dataDir,
                packageName + (wide?"-wide":"") + ".webp");
    }

    public static String cacheName(String packageName) {
        if (App.isWebsite(packageName)) return StringLib.baseUrl(packageName);
        else return packageName;
    }
    public static void updateIcon(File iconFile, String packageName, ImageView imageView) {
        try {
            Drawable newIconDrawable = Drawable.createFromPath(iconFile.getAbsolutePath());
            if (newIconDrawable != null) {
                cachedIcons.put(cacheName(packageName), newIconDrawable); // Success
                if (imageView != null) imageView.setImageDrawable(newIconDrawable);
            }
        } catch (Exception ignored) {
            Log.w("Icon", "Error when loading icon drawable from path "+iconFile.getAbsolutePath());
        }
    }
    @Nullable
    public static Drawable loadIcon(LauncherActivity activity, ApplicationInfo app, ImageView imageView) {
        // Try to load from memory
        if (cachedIcons.containsKey(cacheName(app.packageName))) return cachedIcons.get(cacheName(app.packageName));
        // Try to load from file
        final File iconFile = iconFileForPackage(activity, app.packageName);

        if (iconFile.exists() && !IconRepo.shouldDownload(activity, app)) {
            updateIcon(iconFile, app.packageName, null);
            return Drawable.createFromPath(iconFile.getAbsolutePath());
        }

        // Try to load from package manager
        Drawable appIcon = null;
        if (cachedIcons.containsKey(cacheName(app.packageName))) return cachedIcons.get(cacheName(app.packageName));
        try {
            PackageManager packageManager = activity.getPackageManager();
            Resources resources = packageManager.getResourcesForApplication(app.packageName);

            // Check Icon
            int iconId = app.icon;
            // Check AndroidTV banner
            if (app.banner != 0 && App.isBanner(activity, app)) iconId = app.banner;

            if (iconId == 0) iconId = android.R.drawable.sym_def_app_icon;
            appIcon = ResourcesCompat.getDrawableForDensity(resources, iconId,
                    DisplayMetrics.DENSITY_XXXHIGH, null);

            // Saves the drawable to a webp,
            // which is faster to load than trying to get the drawable every time
            saveIconDrawable(activity, appIcon, app.packageName);
        } catch (Exception ignored) {} // Fails on web apps, possibly also on invalid packages

        // Attempt to download the icon for this app from an online repo
        // Done AFTER saving the drawable version to prevent a race condition)
        IconRepo.check(activity, app, () -> updateIcon(iconFile, app.packageName, imageView));

        return appIcon; // May rarely be null
    }

    public static void reloadIcon(LauncherActivity activity, ApplicationInfo app, ImageView downloadImageView) {
        final File iconFile = iconFileForPackage(activity, app.packageName);
        final boolean ignored = iconFile.delete();
        downloadImageView.setImageDrawable(loadIcon(activity, app, downloadImageView));
        IconRepo.download(activity, app, () -> updateIcon(iconFile, app.packageName, downloadImageView));
        Dialog.toast(activity.getString(R.string.refreshed_icon));
    }

    protected static void saveIconDrawable(LauncherActivity activity, Drawable icon, String packageName) {
        try {
            Bitmap bitmap = ImageLib.bitmapFromDrawable(icon);
            if (bitmap == null)
                Log.i("Icon", "Failed to load drawable bitmap for "+packageName);
            else {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float aspectRatio = (float) width / height;
                if (width > 512) {
                    width = 512;
                    height = Math.round(width / aspectRatio);
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
                }
                FileOutputStream fileOutputStream =
                        new FileOutputStream(iconFileForPackage(activity, packageName)
                                .getAbsolutePath());
                bitmap.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream);
                fileOutputStream.close();
            }
        } catch (Exception e) {
            Log.i("AbstractPlatform", "Exception while converting file " + packageName);
            e.printStackTrace();
        }
    }
    public static void saveIconDrawableExternal(Activity activity, Drawable icon, String packageName) {
        try {
            Bitmap bitmap = ImageLib.bitmapFromDrawable(icon);
            if (bitmap == null)
                Log.i("Icon", "Failed to load drawable bitmap for "+packageName);
            else {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float aspectRatio = (float) width / height;
                if (width > 512) {
                    width = 512;
                    height = Math.round(width / aspectRatio);
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
                }
                FileOutputStream fileOutputStream =
                        new FileOutputStream(iconFileForPackageExt(activity, packageName, false)
                                .getAbsolutePath());
                bitmap.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream);
                fileOutputStream.close();
                FileOutputStream fileOutputStream2 =
                        new FileOutputStream(iconFileForPackageExt(activity, packageName, true)
                                .getAbsolutePath());
                bitmap.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream2);
                fileOutputStream2.close();
            }
        } catch (Exception e) {
            Log.i("ICON", "Exception while converting file " + packageName);
            e.printStackTrace();
        }
    }
    protected static File iconFileForPackageExt(Activity launcherActivity, String packageName, boolean wide) {
        packageName = cacheName(StringLib.toValidFilename(packageName));
        ApplicationInfo tempApp = new ApplicationInfo();
        tempApp.packageName = packageName;
        return new File(launcherActivity.getApplicationInfo().dataDir,
                packageName + (wide?"-wide":"") + ".webp");
    }
}
