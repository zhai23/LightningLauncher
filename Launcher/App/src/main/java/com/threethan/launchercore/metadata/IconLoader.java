package com.threethan.launchercore.metadata;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import com.threethan.launchercore.Core;
import com.threethan.launchercore.adapter.UtilityApplicationInfo;
import com.threethan.launchercore.lib.ImageLib;
import com.threethan.launchercore.lib.StringLib;
import com.threethan.launchercore.util.App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/*
    Icon

    This abstract class provides helper functions for getting, setting and saving icons.
    It uses IconRepo in order to request icons from the internet, if applicable
 */

public abstract class IconLoader {
    private static final int ICON_QUALITY = 50;
    public static final int ICON_HEIGHT = 192;
    public static final String ICON_CACHE_FOLDER = "/icon-cache";
    public static final Map<String, Drawable> cachedIcons = new ConcurrentHashMap<>();
    public static final Object ICON_CUSTOM_FOLDER = "/icon-custom";

    public static void cacheIcon(ApplicationInfo app, Drawable iconDrawable) {
        cachedIcons.put(StringLib.toValidFilename(app.packageName), iconDrawable);
    }

    /**
     * Loads the icon for an app.
     * The callback will be called immediately on this thread,
     * and may also be called on a different thread after a small delay
     * @param app App to get the icon for
     * @param consumer Consumer which handles the icon
     */
    public static void loadIcon(ApplicationInfo app, final Consumer<Drawable> consumer) {
        IconUpdater.executorService.submit(() -> {
            if (app instanceof UtilityApplicationInfo uApp)
                consumer.accept(uApp.getDrawable());
            else if (IconLoader.cachedIcons.containsKey(cacheName(app)))
                consumer.accept(IconLoader.cachedIcons.get(cacheName(app)));
            else loadIcon(icon -> {
                    consumer.accept(icon);
                    cacheIcon(app, icon);
                }, app);
        });
    }

    private static void loadIcon(Consumer<Drawable> callback, ApplicationInfo app) {
        // Everything in the try will still attempt to download an icon
        try {
            Drawable appIcon = null;
            // Try to load from external custom icon file
            final File iconCustomFile = iconCustomFileForApp(app);
            if (iconCustomFile.exists())
                appIcon = Drawable.createFromPath(iconCustomFile.getAbsolutePath());
            if (appIcon != null) {
                callback.accept(appIcon);
                return;
            }
            // Try to load from cached icon file
            final File iconCacheFile = iconCacheFileForApp(app);

            if (iconCacheFile.exists())
                appIcon = Drawable.createFromPath(iconCacheFile.getAbsolutePath());
            if (appIcon != null) {
                callback.accept(appIcon);
                return;
            }

            // Try to load from package manager
            PackageManager packageManager = Core.context().getPackageManager();
            Resources resources = packageManager.getResourcesForApplication(app);

            // Check Icon
            int iconId = app.icon;
            // Check AndroidTV banner
            if (app.banner != 0 && App.isBanner(app)) iconId = app.banner;

            if (iconId == 0) iconId = android.R.drawable.sym_def_app_icon;
            appIcon = ResourcesCompat.getDrawable(resources, iconId, null);
            callback.accept(appIcon);
        } catch (PackageManager.NameNotFoundException ignored) {
        } finally {
            // Attempt to download the icon for this app from an online repo
            // Done AFTER saving the drawable version to prevent a race condition)
            IconUpdater.check(app, callback);
        }
    }

    /** @return The file location which should be used for the applications cache file */
    static File iconCacheFileForApp(ApplicationInfo app) {
        String cacheName = cacheName(app);
        final boolean banner = App.isBanner(app);
        return new File(Core.context().getApplicationInfo().dataDir + ICON_CACHE_FOLDER,
                cacheName + (banner ? "-banner" : "") + ".webp");
    }
    public static File iconCustomFileForApp(ApplicationInfo app) {
        String cacheName = cacheName(app);
        final boolean banner = App.isBanner(app);
        return new File(Core.context().getApplicationInfo().dataDir + ICON_CUSTOM_FOLDER,
                cacheName + (banner ? "-banner" : "") + ".webp");    }
    public static String cacheName(ApplicationInfo app) {
        if (App.getType(app.packageName) == App.Type.WEB)
            return StringLib.toValidFilename(StringLib.baseUrl(app.packageName));
        return StringLib.toValidFilename(app.packageName);
    }
    public static void saveIconDrawableExternal(Drawable icon, ApplicationInfo app) {
        try {
            Bitmap bitmap = ImageLib.bitmapFromDrawable(icon);
            if (bitmap == null)
                Log.i("Icon", "Failed to load drawable bitmap for "+app.packageName);
            else {
                String cacheName = cacheName(app);
                File f1 =  new File(Core.context().getApplicationInfo().dataDir + ICON_CUSTOM_FOLDER,
                        cacheName + ".webp");
                File f2 =  new File(Core.context().getApplicationInfo().dataDir + ICON_CUSTOM_FOLDER,
                        cacheName + "-banner" + ".webp");
                compressAndSaveBitmap(f1,bitmap);
                compressAndSaveBitmap(f2,bitmap);
            }
        } catch (Exception e) {
            Log.i("Icon", "Exception while converting file " + app.packageName, e);
        }
    }
    public static void compressAndSaveBitmap(File file, Bitmap bitmap) {
        try {
            //noinspection ResultOfMethodCallIgnored
            Objects.requireNonNull(file.getParentFile()).mkdirs();
            FileOutputStream fileOutputStream = new FileOutputStream(file.getAbsolutePath(), false);
            bitmap = ImageLib.getResizedBitmap(bitmap, ICON_HEIGHT);
            bitmap.compress(Bitmap.CompressFormat.WEBP, ICON_QUALITY, fileOutputStream);
            fileOutputStream.close();
        } catch (IOException e) {
            Log.e("Icon", "IOException during bitmap save", e);
        }
    }
}
