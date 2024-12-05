package com.threethan.launcher.activity.executor;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;

import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launcher.activity.support.DataStoreEditor;
import com.threethan.launcher.activity.support.SettingsManager;
import com.threethan.launcher.data.Settings;
import com.threethan.launchercore.lib.ImageLib;
import com.threethan.launchercore.util.Platform;

import java.io.File;

/**
    Loads an image background asynchronously, then sets it to the specified view when done
    It is also responsible from cropping/resizing the image for different window sizes
    (Used only by LauncherActivity)
 */

public class WallpaperExecutor {
    public void execute(LauncherActivity owner) {
        Thread thread = new Thread(() -> {
            // Do fetching of data
            int background = LauncherActivity.backgroundIndex;
            BitmapDrawable backgroundThemeDrawable;
            Bitmap imageBitmap = null;
            if (background >= 0 && background < SettingsManager.BACKGROUND_DRAWABLES.length) {

                // Create a cropped image asset for the window background
                imageBitmap = BitmapFactory.decodeResource(owner.getResources(), SettingsManager.BACKGROUND_DRAWABLES[background]);
            } else {
                File file = new File(owner.getApplicationInfo().dataDir, Settings.CUSTOM_BACKGROUND_PATH);
                try {
                    imageBitmap = ImageLib.bitmapFromFile(file);
                } catch (Exception ignored) {} // In case file no longer exists or similar
            }
            if (imageBitmap == null) return;
            float aspectScreen;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Rect bounds = owner.getWindowManager().getCurrentWindowMetrics().getBounds();
                aspectScreen = bounds.width() / (float) bounds.height();
            } else {
                int heightPixels = Resources.getSystem().getDisplayMetrics().heightPixels;
                int widthPixels = Resources.getSystem().getDisplayMetrics().widthPixels;
                aspectScreen = widthPixels / (float) heightPixels;
            }

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
            backgroundThemeDrawable = new BitmapDrawable(Resources.getSystem(), imageBitmap);

            if (Platform.isQuest()) {
                backgroundThemeDrawable.setAlpha(getBackgroundAlpha(owner.dataStoreEditor) + 1);
            }
            // Apply
            BitmapDrawable finalBackgroundThemeDrawable = backgroundThemeDrawable;
            owner.runOnUiThread(() -> {
                owner.getWindow().setBackgroundDrawable(finalBackgroundThemeDrawable);
                owner.updateToolBars();
            });
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /** Quest only. Clamped between 1 and 254 to prevent compositing issues. */
    public static int getBackgroundAlpha(DataStoreEditor dataStoreEditor) {
        int alpha = dataStoreEditor.getInt(Settings.KEY_BACKGROUND_ALPHA, Settings.DEFAULT_ALPHA);

        // Workaround for weird new translucency behaviour
//        if (Platform.getVrOsVersion() >= 72) alpha = alpha / 2 + 128;

        return Math.max(1, Math.min(alpha, 254));
    }
}
