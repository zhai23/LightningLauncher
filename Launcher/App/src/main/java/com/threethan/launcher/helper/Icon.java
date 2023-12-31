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
    private static final String BANNER_SUFFIX = "$banner";

    public static File iconCacheFileForPackage(LauncherActivity launcherActivity, ApplicationInfo app) {
        return iconFileForPackage(launcherActivity, app, false);
    }
    public static File iconCustomFileForApp(LauncherActivity launcherActivity, ApplicationInfo app) {
        return iconFileForPackage(launcherActivity, app, true);
    }
    private static File iconFileForPackage(LauncherActivity launcherActivity, ApplicationInfo app, boolean custom) {
        String cachename = cacheName(app);
        ApplicationInfo tempApp = new ApplicationInfo();
        tempApp.packageName = cachename;
        final boolean wide = App.isBanner(tempApp);
        return new File(launcherActivity.getApplicationInfo().dataDir +
                (custom ? ICON_CUSTOM_FOLDER : ICON_CACHE_FOLDER),
                cachename + (wide && !custom ? "-wide" : "") + ".webp");
    }
    public static void init(LauncherActivity launcherActivity) {
        // Icon init
        IconRepo.updateInternet(launcherActivity);
        File cacheDir = new File(launcherActivity.getApplicationInfo().dataDir + Icon.ICON_CACHE_FOLDER);
        boolean ignored1 = cacheDir.mkdir();
        File customDir = new File(launcherActivity.getApplicationInfo().dataDir + Icon.ICON_CUSTOM_FOLDER);
        boolean ignored2 = customDir.mkdir();
    }

    public static String cacheName(ApplicationInfo app) {
        String cacheName;
        if (App.isWebsite(app)) return StringLib.baseUrlWithScheme(app.packageName);
        else {
            cacheName = app.packageName;
            if (App.isBanner(app)) cacheName += BANNER_SUFFIX;
        }
        return StringLib.toValidFilename(cacheName);
    }
    public static void updateIcon(File iconFile, ApplicationInfo app, ImageView imageView) {
        try {
            Drawable newIconDrawable = Drawable.createFromPath(iconFile.getAbsolutePath());
            if (newIconDrawable != null) {
                cachedIcons.put(cacheName(app), newIconDrawable); // Success
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
        if (cachedIcons.containsKey(cacheName(app))) return cachedIcons.get(cacheName(app));

        Drawable appIcon = null;

        // Try to load from custom icon file
        final File iconCustomFile = iconCustomFileForApp(activity, app);
        if (iconCustomFile.exists()) {
            updateIcon(iconCustomFile, app, null);
            appIcon = Drawable.createFromPath(iconCustomFile.getAbsolutePath());
            if (appIcon != null) return appIcon; // No need to download if we have a custom icon
        }
        // Try to load from cached icon file
        final File iconCacheFile = iconCacheFileForPackage(activity, app);
        if (iconCacheFile.exists()) {
            updateIcon(iconCacheFile, app, null);
            appIcon = Drawable.createFromPath(iconCacheFile.getAbsolutePath());
        }

        if (appIcon == null) {
            // Try to load from package manager
            if (cachedIcons.containsKey(cacheName(app)))
                appIcon = cachedIcons.get(cacheName(app));
            else try {
                PackageManager packageManager = activity.getPackageManager();
                Resources resources = packageManager.getResourcesForApplication(app);

                // Check Icon
                int iconId = app.icon;
                // Check AndroidTV banner
                if (app.banner != 0 && App.isBanner(app)) iconId = app.banner;

                if (iconId == 0) iconId = android.R.drawable.sym_def_app_icon;
                appIcon = resources.getDrawable(iconId, activity.getTheme());

                // Saves the drawable to a webp,
                // which is faster to load than trying to get the drawable every time
                saveIconDrawable(activity, appIcon, app);
            } catch (Exception ignored) {
            } // Fails on web apps, possibly also on invalid packages
        }
        // Attempt to download the icon for this app from an online repo
        // Done AFTER saving the drawable version to prevent a race condition)
        IconRepo.check(activity, app, () -> updateIcon(iconCacheFile, app, imageView));

        return appIcon; // May rarely be null
    }

    public static void reloadIcon(LauncherActivity activity, ApplicationInfo app, ImageView downloadImageView) {
        final boolean ignored0 = iconCustomFileForApp(activity, app).delete();
        final File iconFile = iconCacheFileForPackage(activity, app);
        final boolean ignored1 = iconFile.delete();
        downloadImageView.setImageDrawable(loadIcon(activity, app, downloadImageView));
        IconRepo.download(activity, app, () -> updateIcon(iconFile, app, downloadImageView));
        Dialog.toast(activity.getString(R.string.refreshed_icon));
    }

    protected static void saveIconDrawable(LauncherActivity activity, Drawable icon, ApplicationInfo app) {
        try {
            Bitmap bitmap = ImageLib.bitmapFromDrawable(icon);
            if (bitmap == null)
                Log.i("Icon", "Failed to load drawable bitmap for "+app.packageName);
            else {
                compressAndSaveBitmap(iconCacheFileForPackage(activity, app), bitmap);
                Log.i("Icon", "Saved drawable bitmap for "+app.packageName);
            }
        } catch (Exception e) {
            Log.i("AbstractPlatform", "Exception while converting file "+app.packageName);
            e.printStackTrace();
        }
    }
    public static void saveIconDrawableExternal(Activity activity, Drawable icon, ApplicationInfo app) {
        try {
            Bitmap bitmap = ImageLib.bitmapFromDrawable(icon);
            if (bitmap == null)
                Log.i("Icon", "Failed to load drawable bitmap for "+app.packageName);
            else {
                String cacheName = cacheName(app);
                File f1 =  new File(activity.getApplicationInfo().dataDir + ICON_CUSTOM_FOLDER,
                        cacheName + ".webp");
                File f2 =  new File(activity.getApplicationInfo().dataDir + ICON_CUSTOM_FOLDER,
                        cacheName + BANNER_SUFFIX + ".webp");
                compressAndSaveBitmap(f1,bitmap);
                compressAndSaveBitmap(f2,bitmap);
            }
        } catch (Exception e) {
            Log.i("ICON", "Exception while converting file " + app.packageName);
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
}
