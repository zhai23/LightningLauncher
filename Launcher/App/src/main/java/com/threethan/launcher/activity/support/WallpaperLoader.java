package com.threethan.launcher.activity.support;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;

import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launcher.data.Settings;
import com.threethan.launchercore.lib.ImageLib;
import com.threethan.launchercore.util.Platform;

import java.io.File;

/**
    Loads an image background asynchronously, then sets it to the specified view when done
    It is also responsible from cropping/resizing the image for different window sizes
    (Used only by LauncherActivity)
 */

public class WallpaperLoader {

    private Bitmap imageBitmap = null;
    LauncherActivity owner;
    public WallpaperLoader(LauncherActivity owner) {
        this.owner = owner;
    }
    public void crop() {
        Thread thread = new Thread(this::cropInternal);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }
    public void load() {
        Thread thread = new Thread(() -> {
            loadInternal();
            cropInternal();
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }
    private void cropInternal() {
        if (imageBitmap == null) loadInternal();
        if (imageBitmap == null) return;
        float aspectScreen = getAspectScreen(owner);

        float aspectImage = imageBitmap.getWidth() / (float) imageBitmap.getHeight();
        int cropWidth = imageBitmap.getWidth();
        int cropHeight = imageBitmap.getHeight();

        if (aspectScreen < aspectImage)
            cropWidth = (int) (imageBitmap.getHeight() * aspectScreen);
        else cropHeight = (int) (imageBitmap.getWidth() / aspectScreen);

        int cropMarginWidth = imageBitmap.getWidth() - cropWidth;
        int cropMarginHeight = imageBitmap.getHeight() - cropHeight;

        imageBitmap = Bitmap.createBitmap(imageBitmap, cropMarginWidth, cropMarginHeight,
                imageBitmap.getWidth() - cropMarginWidth,
                imageBitmap.getHeight() - cropMarginHeight);

        BitmapDrawable wallpaperDrawable = new BitmapDrawable(Resources.getSystem(), imageBitmap);

        if (Platform.isQuest())
            wallpaperDrawable.setAlpha(getBackgroundAlpha(owner.dataStoreEditor) + 1);

        // Apply
        owner.runOnUiThread(() ->
                owner.getWindow().setBackgroundDrawable(wallpaperDrawable));

    }
    public void loadInternal() {
        int background = LauncherActivity.backgroundIndex;
        imageBitmap = null;
        if (background >= 0 && background < SettingsManager.BACKGROUND_DRAWABLES.length) {

            // Create a cropped image asset for the window background
            imageBitmap = BitmapFactory.decodeResource(owner.getResources(), SettingsManager.BACKGROUND_DRAWABLES[background]);
        } else {
            File file = new File(owner.getApplicationInfo().dataDir, Settings.CUSTOM_BACKGROUND_PATH);
            try {
                imageBitmap = ImageLib.bitmapFromFile(file);
            } catch (Exception ignored) {} // In case file no longer exists or similar
        }
    }

    private static float getAspectScreen(Activity owner) {
        float aspectScreen;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect bounds = owner.getWindowManager().getCurrentWindowMetrics().getBounds();
            aspectScreen = bounds.width() / (float) bounds.height();
        } else {
            int heightPixels = Resources.getSystem().getDisplayMetrics().heightPixels;
            int widthPixels = Resources.getSystem().getDisplayMetrics().widthPixels;
            aspectScreen = widthPixels / (float) heightPixels;
        }
        return aspectScreen;
    }

    /** Quest only. Clamped between 1 and 254 to prevent compositing issues. */
    public static int getBackgroundAlpha(DataStoreEditor dataStoreEditor) {
        int alpha = dataStoreEditor.getInt(Settings.KEY_BACKGROUND_ALPHA, Settings.DEFAULT_ALPHA);
        return Math.max(1, Math.min(alpha, 254));
    }
}
