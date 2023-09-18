package com.threethan.launcher.helper;

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

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.lib.StringLib;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

public abstract class Icon {
    public static final HashMap<String, Drawable> cachedIcons = new HashMap<>();
    public static File iconFileForPackage(LauncherActivity launcherActivity, String packageName) {
        packageName = StringLib.toValidFilename(packageName);
        ApplicationInfo tempApp = new ApplicationInfo();
        tempApp.packageName = packageName;
        final boolean wide = App.isBanner(tempApp, launcherActivity);
        return new File(launcherActivity.getApplicationInfo().dataDir,
                packageName + (wide?"-wide":"") + ".webp");
    }
    public static void updateIcon(File iconFile, String packageName, ImageView imageView) {
        try {
            Drawable newIconDrawable = Drawable.createFromPath(iconFile.getAbsolutePath());
            if (newIconDrawable != null) {
                cachedIcons.put(packageName, newIconDrawable); // Success
                if (imageView != null) imageView.setImageDrawable(newIconDrawable);
            }
        } catch (Exception ignored) {
            Log.w("Icon", "Error when loading icon drawable from path "+iconFile.getAbsolutePath());
        }
    }
    @Nullable
    public static Drawable loadIcon(LauncherActivity activity, ApplicationInfo app, ImageView imageView) {
        // Try to load from memory
        if (cachedIcons.containsKey(app.packageName)) return cachedIcons.get(app.packageName);
        // Try to load from file
        final File iconFile = iconFileForPackage(activity, app.packageName);
        if (iconFile.exists() && !IconRepo.shouldDownload(activity, app)) {
            updateIcon(iconFile, app.packageName, null);
            return Drawable.createFromPath(iconFile.getAbsolutePath());
        }

        // Try to load from package manager
        Drawable appIcon = null;
        if (cachedIcons.containsKey(app.packageName)) return cachedIcons.get(app.packageName);
        try {
            PackageManager packageManager = activity.getPackageManager();
            Resources resources = packageManager.getResourcesForApplication(app.packageName);

            int iconId = app.icon;
            if (iconId == 0) iconId = android.R.drawable.sym_def_app_icon;
            appIcon = ResourcesCompat.getDrawableForDensity(resources, iconId,
                    DisplayMetrics.DENSITY_XXXHIGH, null);

            // Saves the drawable to a webp,
            // which is faster to load than trying to get the drawable every time
            saveIconDrawable(activity, appIcon, app.packageName);
        } catch (PackageManager.NameNotFoundException ignored) {} // Fails on web apps, possibly also on invalid packages

        // Download icon AFTER saving the drawable version
        // (this prevents a race condition)
        IconRepo.check(activity, app, () -> updateIcon(iconFile, app.packageName, imageView));

        return appIcon; // May rarely be null
    }

    public static void reloadIcon(LauncherActivity activity, ApplicationInfo app, ImageView downloadImageView) {
        final File iconFile = iconFileForPackage(activity, app.packageName);
        final boolean ignored = iconFile.delete();
        downloadImageView.setImageDrawable(loadIcon(activity, app, downloadImageView));
        IconRepo.download(activity, app, () -> updateIcon(iconFile, app.packageName, downloadImageView));
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
}
