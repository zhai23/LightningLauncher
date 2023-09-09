package com.threethan.launcher.helper;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.ImageLib;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

public abstract class Icon {
    public static final HashMap<String, Drawable> cachedIcons = new HashMap<>();
    public static File iconFileForPackage(LauncherActivity launcherActivity, String packageName) {
        packageName = packageName.replace("/","");
        ApplicationInfo tempApp = new ApplicationInfo();
        tempApp.packageName = packageName;
        final boolean wide = App.isBanner(tempApp, launcherActivity);
        return new File(launcherActivity.getApplicationInfo().dataDir, packageName + (wide?"-wide":"") + ".webp");
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
    public static Drawable loadIcon(LauncherActivity activity, ApplicationInfo appInfo, ImageView[] imageViews) {
        final File iconFile = iconFileForPackage(activity, appInfo.packageName);
        IconRepo.check(activity, appInfo, () -> updateIcon(iconFile, appInfo.packageName, imageViews));

        if (cachedIcons.containsKey(appInfo.packageName)) return cachedIcons.get(appInfo.packageName);

        PackageManager packageManager = activity.getPackageManager();

        Drawable appIcon;
        if (iconFile.exists()) {
            updateIcon(iconFile, appInfo.packageName, null);
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

    public static void reloadIcon(LauncherActivity activity, ApplicationInfo appInfo, ImageView[] imageViews) {
        final File iconFile = iconFileForPackage(activity, appInfo.packageName);
        final boolean ignored = iconFile.delete();
        imageViews[0].setImageDrawable(loadIcon(activity, appInfo, imageViews));
        IconRepo.download(activity, appInfo, () -> updateIcon(iconFile, appInfo.packageName, imageViews));
    }

    protected static void saveIconDrawable(LauncherActivity launcherActivity, Drawable icon, String packageName) {
        try {
            Bitmap bitmap = ImageLib.bitmapFromDrawable(icon);
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
                            new FileOutputStream(iconFileForPackage(launcherActivity, packageName).getAbsolutePath());
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
}
