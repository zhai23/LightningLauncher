package com.threethan.launcher.helper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.lib.StringLib;
import com.threethan.launcher.support.SettingsManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*
    IconRepo

    This abstract class is dedicated to downloading icons from online repositories.

    It is called by the Icon class. If no downloadable icon is found, the Icon class decides instead
 */

public abstract class IconRepo {
    // Repository URLs:
    // Each URL will be tried in order: the first with a file matching the package name will be used
    private static final String[] ICON_URLS_SQUARE = {
            "https://raw.githubusercontent.com/threethan/QuestLauncherImages/main/icon/%s.jpg",
            "https://raw.githubusercontent.com/veticia/binaries/main/icons/%s.png",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/oculus_square/%s.jpg",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/pico_square/%s.jpg",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/viveport_square/%s.jpg"
    };
    private static final String[] ICON_URLS_BANNER = {
            "https://raw.githubusercontent.com/threethan/QuestLauncherImages/main/banner/%s.jpg",
            "https://raw.githubusercontent.com/veticia/binaries/main/banners/%s.png",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/oculus_landscape/%s.jpg",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/pico_landscape/%s.jpg",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/viveport_landscape/%s.jpg"
    };
    // Instead of matching a package name, websites match their TLD
    private static final String[] ICON_URLS_WEB = {
            "https://logo.clearbit.com/%s", // Provides high-res icons for TLDs
            "%s/favicon.ico", // The standard directory for an icon to be places
    };
    private static final String TEST_URL = "https://github.com/threethan/QuestLauncherImages/blob/main/banner/com.oculus.browser.jpg";
    // If a download finishes, regardless of whether an icon is found, the app will be added to this
    // list, and will not be downloaded again unless manually requested.
    protected static Set<String> downloadExemptPackages = Collections.synchronizedSet(new HashSet<>());
    private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    // Helper functions
    public static void check(final LauncherActivity activity, ApplicationInfo app, final Runnable callback) {
        if (shouldDownload(activity, app)) download(activity, app, callback);
    }

    public static synchronized boolean shouldDownload(LauncherActivity activity, ApplicationInfo app) {
        if (App.isShortcut(app)) return false;
        if (downloadExemptPackages.isEmpty()) {
            downloadExemptPackages.addAll(activity.dataStoreEditor
                    .getStringSet(SettingsManager.DONT_DOWNLOAD_ICONS, downloadExemptPackages));
        }
        return !downloadExemptPackages.contains(app.packageName);
    }

    public static synchronized void dontDownloadIconFor(LauncherActivity activity, String packageName) {
        if (downloadExemptPackages.isEmpty())
            downloadExemptPackages = activity.dataStoreEditor
                    .getStringSet(SettingsManager.DONT_DOWNLOAD_ICONS, downloadExemptPackages);
        downloadExemptPackages.add(packageName);
        if (hasInternet) {
            activity.dataStoreEditor
                    .putStringSet(SettingsManager.DONT_DOWNLOAD_ICONS, downloadExemptPackages);
        }
        else shouldSaveDownloadExemptPackagesIfConnected = true;
    }

    // Starts the download and handles threading
    public static void download(final LauncherActivity activity, ApplicationInfo app, final Runnable callback) {
        final String pkgName = app.packageName;

        final boolean isWide = App.isBanner(app);
        final File iconFile = Icon.iconCacheFileForPackage(activity, app);

        new Thread(() -> {
            Object lock = locks.putIfAbsent(pkgName, new Object());
            if (lock == null) {
                lock = locks.get(pkgName);
            }
            synchronized (Objects.requireNonNull(lock)) {
                try {
                    for (final String url : App.isWebsite(app) ? ICON_URLS_WEB : (isWide ? ICON_URLS_BANNER : ICON_URLS_SQUARE)) {
                        final String urlTLD = App.isWebsite(app) ?
                                StringLib.baseUrlWithScheme(pkgName) :
                                pkgName.replace("://","").replace(PanelApp.packagePrefix, "");
                        if (downloadIconFromUrl(activity, String.format(url, urlTLD), iconFile)) {
                            activity.runOnUiThread(callback);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // Set the icon to now download if we either successfully downloaded it, or the download tried and failed
                    locks.remove(pkgName);
                    // ..and if we have internet
                    dontDownloadIconFor(activity, pkgName);
                }
            }
        }).start();
    }

    private static boolean downloadIconFromUrl(Context context, String url, File iconFile) {
        try {
            InputStream inputStream = new URL(url).openStream();
            if (saveStream(context, inputStream, iconFile)) {
                inputStream.close();
                return true;
            } else inputStream.close();
        } catch (IOException ignored) {}
        return false;
    }

    // Turns the downloaded bitmap into an actual file, and applies webp compression
    private static boolean saveStream(Context context, InputStream inputStream, File outputFile) {
        try {
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            int length;
            byte[] buffer = new byte[65536];
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            while ((length = dataInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, length);
            }
            fileOutputStream.flush();
            fileOutputStream.close();

            if (!isImageFileComplete(context, outputFile)) {
                Log.i("IconRepo", "Image file not complete" + outputFile.getAbsolutePath());
                return false;
            }

            Bitmap bitmap = ImageLib.bitmapFromFile(context, outputFile);

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

    // This usually returns true, but may fail if the download was interrupted or corrupt
    private static boolean isImageFileComplete(Context context, File imageFile) {
        boolean success = false;
        if (imageFile.length() > 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            if (ImageLib.bitmapFromFile(context, imageFile, options) == null) {
                Log.i("IconRepo", "Failed to get valid bitmap from "+imageFile.getAbsolutePath());
            }
            success = (options.outWidth > 0 && options.outHeight > 0);
        }

        if (!success) Log.e("AbstractPlatform", "Failed to validate image file: " + imageFile);
        return success;
    }

    public static Boolean hasInternet = false;
    public static Boolean shouldSaveDownloadExemptPackagesIfConnected = false;
    public static void updateInternet(LauncherActivity activity) {
        Thread thread = new Thread(() -> {
            try {
                hasInternet = checkInternet();
                if (shouldSaveDownloadExemptPackagesIfConnected) {
                    activity.dataStoreEditor
                            .putStringSet(SettingsManager.DONT_DOWNLOAD_ICONS, downloadExemptPackages);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
    }
    private static boolean checkInternet() {
        try {
            InputStream inputStream = new URL(TEST_URL).openStream();
            if (inputStream == null) return false;
            inputStream.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
