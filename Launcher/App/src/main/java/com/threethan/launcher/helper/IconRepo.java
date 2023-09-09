package com.threethan.launcher.helper;

import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.support.SettingsManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IconRepo {
    private static final String[] ICON_URLS = {
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/oculus_square/%s.jpg",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/pico_square/%s.jpg",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/viveport_square/%s.jpg"
    };
    private static final String[] ICON_URLS_WIDE = {
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/oculus_landscape/%s.jpg",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/pico_landscape/%s.jpg",
            "https://raw.githubusercontent.com/basti564/LauncherIcons/main/viveport_landscape/%s.jpg"
    };
    private static final String[] ICON_URLS_WEB = {
            "https://logo.clearbit.com/google.com/%s",
            "%s/favicon.ico",
    };
    public static final HashSet<String> excludedIconPackages = new HashSet<>();
    public static Set<String> dontDownloadIconPackages = new HashSet<>();

    private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public static void check(final LauncherActivity activity, ApplicationInfo appInfo, final Runnable callback) {
        if (shouldDownload(activity, appInfo)) download(activity, appInfo, callback);
    }
    private static boolean shouldDownload(LauncherActivity activity, ApplicationInfo appInfo) {
        if (!(App.isVirtualReality(appInfo, activity) || App.isWebsite(appInfo))) return false;
        if (dontDownloadIconPackages.isEmpty())
            dontDownloadIconPackages = activity.sharedPreferences.getStringSet(SettingsManager.DONT_DOWNLOAD_ICONS, dontDownloadIconPackages);
        return !dontDownloadIconPackages.contains(appInfo.packageName);
    }
    public static void download(final LauncherActivity activity, ApplicationInfo appInfo, final Runnable callback) {
        final String pkgName = appInfo.packageName;
        if (excludedIconPackages.contains(pkgName)) return;
        else excludedIconPackages.add(pkgName);

        final boolean isWide = App.isBanner(appInfo, activity);
        final File iconFile = Icon.iconFileForPackage(activity, pkgName);

        new Thread(() -> {
            Object lock = locks.putIfAbsent(pkgName, new Object());
            if (lock == null) {
                lock = locks.get(pkgName);
            }
            synchronized (Objects.requireNonNull(lock)) {
                try {
                    for (final String url: App.isWebsite(appInfo) ? ICON_URLS_WEB : (isWide ? ICON_URLS_WIDE : ICON_URLS)) {
                        final String urlName = App.isWebsite(appInfo) ?
                                pkgName.split("//")[0]+"//"+pkgName.split("/")[2] : pkgName;

                        if (downloadIconFromUrl(String.format(url, urlName), iconFile)) {
                            activity.runOnUiThread(callback);
                            return;
                        }
                    }
                    Log.v("AbstractPlatform", "Failed to find icon at any URL for " + pkgName);
                    // If we get here, it failed to fetch. That's fine though.
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (!App.isWebsite(appInfo)) dontDownloadIconPackages.add(pkgName);
                    activity.sharedPreferenceEditor.putStringSet(SettingsManager.DONT_DOWNLOAD_ICONS, dontDownloadIconPackages);
                    locks.remove(pkgName);
                }
            }
        }).start();
    }

    private static boolean downloadIconFromUrl(String url, File iconFile) {
        try (InputStream inputStream = new URL(url).openStream()) {
            if (saveStream(inputStream, iconFile)) return true;
        } catch (IOException ignored) {}
        return false;
    }

    private static boolean saveStream(InputStream inputStream, File outputFile) {
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

            if (!isImageFileComplete(outputFile)) {
                Log.i("AbstractPlatform", "Image file not complete" + outputFile.getAbsolutePath());
                return false;
            }

            Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
            if (bitmap == null) {
                Log.i("AbstractPlatform", "Failed to get bitmap from "+outputFile.getAbsolutePath());
            }
            if (bitmap != null) {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float aspectRatio = (float) width / height;
                if (width > 512) {
                    width = 512;
                    height = Math.round(width / aspectRatio);
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
                }

                try {
                    fileOutputStream = new FileOutputStream(outputFile);
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream);
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return true;
        } catch (Exception e) {
            Log.i("AbstractPlatform", "Exception while converting file " + outputFile.getAbsolutePath());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isImageFileComplete(File imageFile) {
        boolean success = false;
        try {
            if (imageFile.length() > 0) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                if (BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options) == null) {
                    Log.i("AbstractPlatform", "Failed to get bitmap from "+imageFile.getAbsolutePath());
                }
                success = (options.outWidth > 0 && options.outHeight > 0);
            }
        } catch (Exception ignored) {}

        if (!success) {
            Log.e("AbstractPlatform", "Failed to validate image file: " + imageFile);
        }

        return success;
    }

}
