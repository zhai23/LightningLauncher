package com.threethan.launcher.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.lib.StringLib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
    Icon

    This abstract class provides helper functions for getting, setting and saving icons.
    It uses IconRepo in order to request icons from the internet, if applicable
 */

public abstract class Icon {
    private static final int ICON_MAX_HEIGHT = 256;
    private static final int ICON_QUALITY = 90;
    public static final String ICON_CACHE_FOLDER = "/icon-cache";
    public static final String ICON_CUSTOM_FOLDER = "/icon-custom";
    public static final Map<String, Drawable> cachedIcons = new ConcurrentHashMap<>();
    public static File iconCacheFileForPackage(LauncherActivity launcherActivity, String packageName) {
        return iconFileForPackage(launcherActivity, packageName, false);
    }
    public static File iconCustomFileForPackage(LauncherActivity launcherActivity, String packageName) {
        return iconFileForPackage(launcherActivity, packageName, true);
    }
    private static File iconFileForPackage(LauncherActivity launcherActivity, String packageName, boolean custom) {
        packageName = cacheName(StringLib.toValidFilename(packageName));
        ApplicationInfo tempApp = new ApplicationInfo();
        tempApp.packageName = packageName;
        final boolean wide = App.isBanner(launcherActivity, tempApp);
        return new File(launcherActivity.getApplicationInfo().dataDir +
                (custom ? ICON_CUSTOM_FOLDER : ICON_CACHE_FOLDER),
                packageName + (wide && !custom ? "-wide" : "") + ".webp");
    }
    public static void init(LauncherActivity launcherActivity) {
        // Icon init
        IconRepo.updateInternet(launcherActivity);
        File cacheDir = new File(launcherActivity.getApplicationInfo().dataDir + Icon.ICON_CACHE_FOLDER);
        boolean ignored1 = cacheDir.mkdir();
        File customDir = new File(launcherActivity.getApplicationInfo().dataDir + Icon.ICON_CUSTOM_FOLDER);
        boolean ignored2 = customDir.mkdir();
    }

    public static String cacheName(String packageName) {
        if (App.isWebsite(packageName)) return StringLib.baseUrlWithScheme(packageName);
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
    @SuppressLint("UseCompatLoadingForDrawables")
    @Nullable
    public static Drawable loadIcon(LauncherActivity activity, ApplicationInfo app, ImageView imageView) {
        // Try to load from memory
        if (cachedIcons.containsKey(cacheName(app.packageName))) return cachedIcons.get(cacheName(app.packageName));

        Drawable appIcon = null;

        // Try to load from custom icon file
        final File iconCustomFile = iconCustomFileForPackage(activity, app.packageName);
        if (iconCustomFile.exists()) {
            updateIcon(iconCustomFile, app.packageName, null);
            appIcon = Drawable.createFromPath(iconCustomFile.getAbsolutePath());
            if (appIcon != null) return appIcon; // No need to download if we have a custom icon
        }
        // Try to load from cached icon file
        final File iconCacheFile = iconCacheFileForPackage(activity, app.packageName);
        if (iconCacheFile.exists()) {
            updateIcon(iconCacheFile, app.packageName, null);
            appIcon = Drawable.createFromPath(iconCacheFile.getAbsolutePath());
        }

        if (appIcon == null) {
            // Try to load from package manager
            if (cachedIcons.containsKey(cacheName(app.packageName)))
                appIcon = cachedIcons.get(cacheName(app.packageName));
            else try {
                PackageManager packageManager = activity.getPackageManager();
                Resources resources = packageManager.getResourcesForApplication(app.packageName);

                // Check Icon
                int iconId = app.icon;
                // Check AndroidTV banner
                if (app.banner != 0 && App.isBanner(activity, app)) iconId = app.banner;

                if (iconId == 0) iconId = android.R.drawable.sym_def_app_icon;
                appIcon = resources.getDrawable(iconId, activity.getTheme());

                // Saves the drawable to a webp,
                // which is faster to load than trying to get the drawable every time
                saveIconDrawable(activity, appIcon, app.packageName);
            } catch (Exception ignored) {
            } // Fails on web apps, possibly also on invalid packages
        }
        // Attempt to download the icon for this app from an online repo
        // Done AFTER saving the drawable version to prevent a race condition)
        IconRepo.check(activity, app, () -> updateIcon(iconCacheFile, app.packageName, imageView));

        return appIcon; // May rarely be null
    }

    public static void reloadIcon(LauncherActivity activity, ApplicationInfo app, ImageView downloadImageView) {
        final boolean ignored0 = iconCustomFileForPackage(activity, app.packageName).delete();
        final File iconFile = iconCacheFileForPackage(activity, app.packageName);
        final boolean ignored1 = iconFile.delete();
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
                compressAndSaveBitmap(iconCacheFileForPackage(activity, packageName), bitmap);
                Log.i("Icon", "Saved drawable bitmap for "+packageName);
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
                compressAndSaveBitmap(iconFileForPackageExt(activity, packageName),bitmap);
            }
        } catch (Exception e) {
            Log.i("ICON", "Exception while converting file " + packageName);
            e.printStackTrace();
        }
    }
    protected static Bitmap scaleBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float aspectRatioInv = (float) height / width;
        if (height > ICON_MAX_HEIGHT) {
            height = ICON_MAX_HEIGHT;
            width = Math.round(height / aspectRatioInv);
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
        return bitmap;
    }
    public static void compressAndSaveBitmap(File file, Bitmap bitmap) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file.getAbsolutePath());
            bitmap = scaleBitmap(bitmap);
            bitmap.compress(Bitmap.CompressFormat.WEBP, ICON_QUALITY, fileOutputStream);
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    protected static File iconFileForPackageExt(Activity launcherActivity, String packageName) {
        packageName = cacheName(StringLib.toValidFilename(packageName));
        ApplicationInfo tempApp = new ApplicationInfo();
        tempApp.packageName = packageName;
        return new File(launcherActivity.getApplicationInfo().dataDir + ICON_CUSTOM_FOLDER,
                packageName + ".webp");
    }
}
