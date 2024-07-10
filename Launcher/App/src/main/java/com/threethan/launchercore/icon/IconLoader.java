package com.threethan.launchercore.icon;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

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
    private static final int ICON_MAX_HEIGHT = 128;
    private static final int ICON_QUALITY = 90;
    public static final String ICON_CACHE_FOLDER = "/icon-cache";
    public static final Map<String, Drawable> cachedIcons = new ConcurrentHashMap<>();
    public static final Object ICON_CUSTOM_FOLDER = "/icon-custom";

    public static void saveIcon(ApplicationInfo app, File iconFile) {
        try {
            Drawable newIconDrawable = Drawable.createFromPath(iconFile.getAbsolutePath());
            if (newIconDrawable != null) // Success
                cachedIcons.put(StringLib.toValidFilename(app.packageName), newIconDrawable);
        } catch (Exception ignored) {
            Log.w("Icon", "Error when loading icon drawable from path "+iconFile.getAbsolutePath());
        }
    }
    public static void cacheIcon(ApplicationInfo app, Drawable iconDrawable) {
        cachedIcons.put(StringLib.toValidFilename(app.packageName), iconDrawable);
    }
    @Deprecated
    public static void loadIcon(ApplicationInfo app, Activity activity, ImageView... imageViews) {
        loadIcon(app, icon -> activity.runOnUiThread(() -> {
            for (ImageView imageView : imageViews) imageView.setImageDrawable(icon);
        }));
    }

    /**
     * Loads the icon for an app.
     * The callback will be called immediately on this thread,
     * and may also be called on a different thread after a small delay
     * @param app App to get the icon for
     * @param callback Consumer which handles the icon
     */
    public static void loadIcon(ApplicationInfo app, final Consumer<Drawable> callback) {
        if (app instanceof UtilityApplicationInfo uApp)
            callback.accept(uApp.getDrawable());
        else if (IconLoader.cachedIcons.containsKey(cacheName(app)))
            callback.accept(IconLoader.cachedIcons.get(cacheName(app)));
        else new LoadIconExecutor(app, callback).execute();
    }

    /**
     * Loads an icon asynchronously
     */
    protected static class LoadIconExecutor {
        private final ApplicationInfo app;
        private final Consumer<Drawable> consumer;

        protected LoadIconExecutor(ApplicationInfo app, Consumer<Drawable> consumer) {
            this.app = app;
            this.consumer = consumer;
        }

        public void execute() {
            Thread thread = new Thread(() ->
                    loadIcon(icon -> {
                        IconLoader.cacheIcon(app, icon);
                        consumer.accept(icon);
            }));
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
        public void loadIcon(Consumer<Drawable> callback) {
            Drawable appIcon = null;
            // Everything in the try will still attempt to download an icon
            try {
                // Try to load from external custom icon file
                final File iconCustomFile = iconCustomFileForApp(app);
                if (iconCustomFile.exists()) appIcon = Drawable.createFromPath(iconCustomFile.getAbsolutePath());
                if (appIcon != null) {
                    callback.accept(appIcon);
                    return;
                }
                // Try to load from cached icon file
                final File iconCacheFile = iconCacheFileForApp(app);
                if (iconCacheFile.exists()) appIcon = Drawable.createFromPath(iconCacheFile.getAbsolutePath());
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
                appIcon = ResourcesCompat.getDrawable(resources, iconId, Core.context().getTheme());
                callback.accept(appIcon);
            } catch (PackageManager.NameNotFoundException ignored) {}

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
        return StringLib.toValidFilename(app.packageName);
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
            //noinspection ResultOfMethodCallIgnored
            Objects.requireNonNull(file.getParentFile()).mkdirs();
            FileOutputStream fileOutputStream = new FileOutputStream(file.getAbsolutePath(), false);
            bitmap = scaleBitmap(bitmap);
            bitmap.compress(Bitmap.CompressFormat.WEBP, ICON_QUALITY, fileOutputStream);
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            Log.i("ICON", "Exception while converting file " + app.packageName);
            e.printStackTrace();
        }

    }
}
