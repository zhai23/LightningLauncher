package com.threethan.launcher.platforms;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import com.threethan.launcher.MainActivity;
import com.threethan.launcher.R;
import com.threethan.launcher.helpers.LibHelper;
import com.threethan.launcher.helpers.SettingsManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
public abstract class AbstractPlatform {

    public static final HashMap<String, Drawable> cachedIcons = new HashMap<>();
    public static final HashSet<String> excludedIconPackages = new HashSet<>();
    public static Set<String> dontDownloadIconPackages = new HashSet<>();
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
    public static boolean isImageFileComplete(File imageFile) {
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

    public static AbstractPlatform getPlatform(ApplicationInfo ignoredApplicationInfo) {
        return new AppPlatform();
    }

    public static File iconFileForPackage(MainActivity mainActivity, String packageName) {
        packageName = packageName.replace("/","");
        ApplicationInfo tempApp = new ApplicationInfo();
        tempApp.packageName = packageName;
        final boolean wide = isWideApp(tempApp, mainActivity);
        return new File(mainActivity.getApplicationInfo().dataDir, packageName + (wide?"-wide":"") + ".webp");
    }
    public static void updateIcon(File file, String packageName, ImageView[] imageViews) {
        try {
            Drawable newIconDrawable = Drawable.createFromPath(file.getAbsolutePath());
            if (newIconDrawable != null) {
                cachedIcons.put(packageName, newIconDrawable); // Success
                if (imageViews != null) {
                    for(ImageView imageView : imageViews) {
                        imageView.setImageDrawable(newIconDrawable);
                    }
                }
            }

        } catch (Exception ignored) {}
    }

    protected static boolean saveStream(InputStream inputStream, File outputFile) {
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
    protected static void saveIconDrawable(MainActivity mainActivity, Drawable icon, String packageName) {
        try {
            Bitmap bitmap = LibHelper.bitmapFromDrawable(icon);
            if (bitmap == null) {
                Log.i("AbstractPlatform", "Failed to load drawable bitmap for "+packageName);
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
                    FileOutputStream fileOutputStream =
                            new FileOutputStream(iconFileForPackage(mainActivity, packageName).getAbsolutePath());
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream);
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            Log.i("AbstractPlatform", "Exception while converting file " + packageName);
            e.printStackTrace();
        }
    }
    static Set<String> setVr = new HashSet<>();
    static Set<String> set2d = new HashSet<>();

    public static void clearPackageLists() {
        set2d.clear();
        setVr.clear();
    }
    public static HashSet<String> getAllPackages(MainActivity mainActivity) {
        final SharedPreferences sharedPreferences = mainActivity.sharedPreferences;
        if (setVr.isEmpty()) {
            setVr = sharedPreferences.getStringSet(SettingsManager.KEY_VR_SET, setVr);
            set2d = sharedPreferences.getStringSet(SettingsManager.KEY_2D_SET, set2d);
        }
        HashSet<String> setAll = new HashSet<>(set2d);
        setAll.addAll(setVr);
        return setAll;
    }

    public static boolean isVirtualRealityApp(ApplicationInfo applicationInfo, MainActivity mainActivity) {
        final SharedPreferences sharedPreferences = mainActivity.sharedPreferences;
        final SharedPreferences.Editor sharedPreferenceEditor = mainActivity.sharedPreferenceEditor;
        if (setVr.isEmpty()) {
            setVr = sharedPreferences.getStringSet(SettingsManager.KEY_VR_SET, setVr);
            set2d = sharedPreferences.getStringSet(SettingsManager.KEY_2D_SET, set2d);
        }
        if (setVr.contains(applicationInfo.packageName)) return true;
        if (set2d.contains(applicationInfo.packageName)) return false;

        if (
            checkVirtualRealityApp(applicationInfo)) {
            setVr.add(applicationInfo.packageName);
            mainActivity.mainView.post(() -> sharedPreferenceEditor.putStringSet(SettingsManager.KEY_VR_SET, setVr));

            return true;
        } else {
            set2d.add(applicationInfo.packageName);
            mainActivity.mainView.post(() -> sharedPreferenceEditor.putStringSet(SettingsManager.KEY_2D_SET, set2d));
            return false;
        }
    }
    private static boolean checkVirtualRealityApp(ApplicationInfo applicationInfo) {
        if (applicationInfo.metaData == null) return false;
        for (String key : applicationInfo.metaData.keySet()) {
            if (key.startsWith("notch.config")) return true;
            if (key.contains("com.oculus.supportedDevices")) return true;
            if (key.contains("vr.application.mode")) return true;
        }
        return false;
    }

    static HashSet<String> setSupported = new HashSet<>();
    static HashSet<String> setUnsupported = new HashSet<>();
    public static boolean isSupportedApp(ApplicationInfo appInfo, MainActivity mainActivity) {
        final SharedPreferences sharedPreferences = mainActivity.sharedPreferences;
        final SharedPreferences.Editor sharedPreferenceEditor = mainActivity.sharedPreferenceEditor;
        if (setSupported.isEmpty()) {
            sharedPreferences.getStringSet(SettingsManager.KEY_SUPPORTED_SET, setSupported);
            sharedPreferences.getStringSet(SettingsManager.KEY_UNSUPPORTED_SET, setUnsupported);
            setUnsupported.add(mainActivity.getPackageName());
            setSupported.addAll(sharedPreferences.getStringSet(SettingsManager.KEY_WEBSITE_LIST, Collections.emptySet()));
        }

        if (setSupported.contains(appInfo.packageName)) return true;
        if (setUnsupported.contains(appInfo.packageName)) return false;

        if (checkSupportedApp(appInfo, mainActivity)) {
            setSupported.add(appInfo.packageName);
            sharedPreferenceEditor.putStringSet(SettingsManager.KEY_SUPPORTED_SET, setSupported);
            return true;
        } else {
            setUnsupported.add(appInfo.packageName);
            sharedPreferenceEditor.putStringSet(SettingsManager.KEY_UNSUPPORTED_SET, setUnsupported);
            return false;
        }
    }
    private static String[] unsupportedPrefixes;
    private static boolean checkSupportedApp(ApplicationInfo applicationInfo, MainActivity mainActivity) {
        if (unsupportedPrefixes == null) unsupportedPrefixes = mainActivity.getResources().getStringArray(R.array.unsupported_app_prefixes);

        if (applicationInfo.metaData != null) {
            boolean isVr = isVirtualRealityApp(applicationInfo, mainActivity);
            if (!isVr && applicationInfo.metaData.keySet().contains("com.oculus.environmentVersion")) return false;
        }
        if (mainActivity.getPackageManager().getLaunchIntentForPackage(applicationInfo.packageName) == null)
            return isWebsite(applicationInfo);

        for (String prefix : unsupportedPrefixes)
            if (applicationInfo.packageName.startsWith(prefix))
                return false;
        return true;
    }

    public static boolean isWideApp(ApplicationInfo applicationInfo, MainActivity mainActivity) {
        final boolean isVr = isVirtualRealityApp(applicationInfo, mainActivity);
        final SharedPreferences sharedPreferences = mainActivity.sharedPreferences;
        if (isVr)  return sharedPreferences.getBoolean(SettingsManager.KEY_WIDE_VR , SettingsManager.DEFAULT_WIDE_VR );
        final boolean isWeb = isWebsite(applicationInfo);
        if (isWeb) return sharedPreferences.getBoolean(SettingsManager.KEY_WIDE_WEB, SettingsManager.DEFAULT_WIDE_WEB);
        else       return sharedPreferences.getBoolean(SettingsManager.KEY_WIDE_2D , SettingsManager.DEFAULT_WIDE_2D );
    }
    public static boolean isWebsite(ApplicationInfo applicationInfo) {
        return (applicationInfo.packageName.contains("//"));
    }

    private boolean shouldDownloadIcon(MainActivity activity, ApplicationInfo appInfo) {
        if (!(isVirtualRealityApp(appInfo, activity) || isWebsite(appInfo))) return false;
        if (dontDownloadIconPackages.isEmpty())
            dontDownloadIconPackages = activity.sharedPreferences.getStringSet(SettingsManager.DONT_DOWNLOAD_ICONS, dontDownloadIconPackages);
        return !dontDownloadIconPackages.contains(appInfo.packageName);
    }
    public Drawable loadIcon(MainActivity activity, ApplicationInfo appInfo, ImageView[] imageViews) {
        final File iconFile = iconFileForPackage(activity, appInfo.packageName);
        if (shouldDownloadIcon(activity, appInfo))
            downloadIcon(activity, appInfo, () -> updateIcon(iconFile, appInfo.packageName, imageViews));

        if (cachedIcons.containsKey(appInfo.packageName)) return cachedIcons.get(appInfo.packageName);

        PackageManager packageManager = activity.getPackageManager();

        Drawable appIcon;
        if (iconFile.exists()) {
            AbstractPlatform.updateIcon(iconFile, appInfo.packageName, null);
            return Drawable.createFromPath(iconFile.getAbsolutePath());
        }
        if (cachedIcons.containsKey(appInfo.packageName)) return cachedIcons.get(appInfo.packageName);
        try {
            Resources resources = packageManager.getResourcesForApplication(appInfo.packageName);
            int iconId = appInfo.icon;
            if (iconId == 0) {
                iconId = android.R.drawable.sym_def_app_icon;
                appIcon = ResourcesCompat.getDrawableForDensity(resources, iconId, DisplayMetrics.DENSITY_XXXHIGH, null);
            } else {
                appIcon = ResourcesCompat.getDrawableForDensity(resources, iconId, DisplayMetrics.DENSITY_XXXHIGH, null);
                saveIconDrawable(activity, appIcon, appInfo.packageName);
            }
            return appIcon;
        } catch (Exception ignored) {} // Fails on web apps

        return null;
    }

    public void reloadIcon(MainActivity activity, ApplicationInfo appInfo, ImageView[] imageViews) {
        final File iconFile = iconFileForPackage(activity, appInfo.packageName);
        final boolean ignored = iconFile.delete();
        imageViews[0].setImageDrawable(loadIcon(activity, appInfo, imageViews));
        downloadIcon(activity, appInfo, () -> updateIcon(iconFile, appInfo.packageName, imageViews));
    }

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    private void downloadIcon(final MainActivity activity, ApplicationInfo appInfo, final Runnable callback) {
        final String pkgName = appInfo.packageName;
        if (excludedIconPackages.contains(pkgName)) return;
        else excludedIconPackages.add(pkgName);

        final boolean isWide = isWideApp(appInfo, activity);
        final File iconFile = iconFileForPackage(activity, pkgName);

        new Thread(() -> {
            Object lock = locks.putIfAbsent(pkgName, new Object());
            if (lock == null) {
                lock = locks.get(pkgName);
            }
            synchronized (Objects.requireNonNull(lock)) {
                try {
                    for (final String url: isWebsite(appInfo) ? ICON_URLS_WEB : (isWide ? ICON_URLS_WIDE : ICON_URLS)) {
                        final String urlName = isWebsite(appInfo) ?
                                pkgName.split("//")[0]+"//"+pkgName.split("/")[2] : pkgName;

                        Log.v("DOWNLOADNAME", urlName);
                        Log.v("AbstractPlatform", "Checking URL "+String.format(url, urlName));

                        Log.v("DOWNLOADNAME", urlName);
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
                    dontDownloadIconPackages.add(pkgName);
                    activity.sharedPreferenceEditor.putStringSet(SettingsManager.DONT_DOWNLOAD_ICONS, dontDownloadIconPackages);
                    locks.remove(pkgName);
                }
            }
        }).start();
    }

    public abstract boolean runApp(MainActivity mainActivity, ApplicationInfo applicationInfo);

    boolean downloadIconFromUrl(String url, File iconFile) {
        try (InputStream inputStream = new URL(url).openStream()) {
            if (saveStream(inputStream, iconFile)) return true;
        } catch (IOException ignored) {}
        return false;
    }
}