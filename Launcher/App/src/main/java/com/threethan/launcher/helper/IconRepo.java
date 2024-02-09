package com.threethan.launcher.helper;

import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.util.Log;

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.lib.StringLib;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This abstract class is dedicated to downloading icons from online repositories
 * for apps and added websites.
 * <p>
 * Its functions are called by the Icon class. If no downloadable icon is found,
 * the Icon class will then decide on the icon to be used.
 */
public abstract class IconRepo {
    // Repository URLs:
    // Each URL will be tried in order: the first with a file matching the package name will be used
    private static final String[] ICON_URLS_SQUARE = {
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/oculus_square/%s.jpg",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/pico_square/%s.png",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/viveport_square/%s.webp",
            "https://raw.githubusercontent.com/threethan/QuestLauncherImages/main/icon/%s.jpg",
            "https://raw.githubusercontent.com/veticia/binaries/main/icons/%s.png",
    };
    private static final String[] ICON_URLS_BANNER = {
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/oculus_landscape/%s.jpg",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/pico_landscape/%s.png",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/viveport_landscape/%s.webp",
            "https://raw.githubusercontent.com/threethan/QuestLauncherImages/main/banner/%s.jpg",
            "https://raw.githubusercontent.com/veticia/binaries/main/banners/%s.png",
    };
    // Instead of matching a package name, websites match their TLD
    private static final String[] ICON_URLS_WEB = {
            "https://www.google.com/s2/favicons?domain=%s&sz=256", // Provides high-res icons
            "%s/favicon.ico", // The standard directory for a website's icon to be placed
    };
    private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    /**
     * Stores the time when we're next allowed to try to download an icon for a package.
     * <p>
     * Downloads are NOT automatically called at this time, but will be called asynchronously
     * when the icon is next checked/displayed.
     * <p>
     * Since this isn't stored persistently, all icons will be rechecked when the app is fully quit.
     * This is a non-issue since the LauncherService stays open persitently.
     */
    private static final ConcurrentHashMap<String, Long> nextCheckByPackageMs = new ConcurrentHashMap<>();

    // How many minutes before we can recheck an icon that hasn't downloaded
    private static final long ICON_CHECK_TIME_MINUTES = 5;
    // How many minutes before we can recheck an icon that has downloaded for updates
    private static final long ICON_UPDATE_TIME_MINUTES = 60;

    /**
     * Starts the download of an icon, if one should be downloaded for that app
     * @param app App for which to download an icon image
     * @param callback Called when the download completes successfully and the icon is changed
     */
    public static void check(final LauncherActivity activity, ApplicationInfo app, final Runnable callback) {
        if (shouldDownload(app)) download(activity, app, callback);
    }

    /**
     * Clears all icon download delays, letting icons be redownloaded when needed
     */
    public static void clearDelays() {
        nextCheckByPackageMs.clear();
    }

    /**
     * Check if an icon should be downloaded for a particular app
     * @param app Application info of the app (currently only requires packageName)
     * @return True if the icon should be downloaded
     */
    private static synchronized boolean shouldDownload(ApplicationInfo app) {
        if (App.isShortcut(app)) return false; // Shortcut icons are only provided on add
        if (!nextCheckByPackageMs.containsKey(app.packageName)) return true; // Download if not done yet
        // Check time since last download
        final long nextCheckMs = Objects.requireNonNull(nextCheckByPackageMs.get(app.packageName));
        return System.currentTimeMillis() > nextCheckMs;
    }

    /**
     * Starts the download of an icon and handles relevant threading
     * @param app App for which to download an icon image
     * @param callback Called when the download completes successfully
     */
    public static void download(final LauncherActivity activity, ApplicationInfo app, final Runnable callback) {
        final String packageName = app.packageName;
        nextCheckByPackageMs.put(packageName, System.currentTimeMillis() + ICON_CHECK_TIME_MINUTES*1000*60);

        final boolean isWide = App.isBanner(app);
        final File iconFile = Icon.iconCacheFileForPackage(activity, app);

        Thread thread = new Thread(() -> {
            Object lock = locks.putIfAbsent(packageName, new Object());
            if (lock == null) lock = locks.get(packageName);
            synchronized (Objects.requireNonNull(lock)) {
                try {
                    final String file = App.isWebsite(app) ?
                            StringLib.baseUrlWithScheme(packageName) :
                            packageName.replace("://","").replace(PanelApp.packagePrefix, "");
                    for (final String url : App.isWebsite(app) ? ICON_URLS_WEB : (isWide ? ICON_URLS_BANNER : ICON_URLS_SQUARE)) {
                        if (downloadIconFromUrl(String.format(url, file), iconFile)) {
                                nextCheckByPackageMs.put(packageName,
                                        System.currentTimeMillis() + ICON_UPDATE_TIME_MINUTES * 1000 * 60);
                                Icon.saveIcon(app, iconFile);
                                activity.runOnUiThread(callback);
                                return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // Set the icon to now download if we either successfully downloaded it,
                    // or the download tried and failed
                    locks.remove(packageName);
                }
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /**
     * Downloads an icon from a given url and saves it using saveStream()
     * @return True if icon was downloaded
     */
    private static boolean downloadIconFromUrl(String url, File iconFile) {
        try (InputStream inputStream = new URL(url).openStream()) {
            // Try to save
            if (saveStream(inputStream, iconFile)) {
                inputStream.close();
                return true;
            }
        } catch (IOException ignored) {}
        return false;
    }

    /**
     * Saves an inputstream used to download a bitmap to an actual file, applying webp compression.
     * @return True if the stream was different and has been saved
     */
    private static boolean saveStream(InputStream inputStream, File outputFile) {
        try {
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            int length;
            byte[] buffer = new byte[65536];

            //noinspection ResultOfMethodCallIgnored
            Objects.requireNonNull(outputFile.getParentFile()).mkdirs();
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile, false);

            while ((length = dataInputStream.read(buffer)) > 0)
                fileOutputStream.write(buffer, 0, length);

            fileOutputStream.flush();
            fileOutputStream.close();

            Bitmap bitmap = ImageLib.bitmapFromFile(outputFile);

            if (bitmap != null) {
                Icon.compressAndSaveBitmap(outputFile, bitmap);
                return true;
            }
            return false;

        } catch (Exception e) {
            Log.i("AbstractPlatform", "Exception while converting file " + outputFile.getAbsolutePath());
            e.printStackTrace();
            return false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
