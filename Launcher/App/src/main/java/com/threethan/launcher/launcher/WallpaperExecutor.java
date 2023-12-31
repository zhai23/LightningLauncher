package com.threethan.launcher.launcher;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;

import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.support.SettingsManager;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
    BackgroundTask

    This task loads an image background asynchronously, then sets it to the specified view when done
    Used only by LauncherActivity
 */

class WallpaperExecutor {
    public void execute(LauncherActivity owner) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Do fetching of data
            int background = owner.dataStoreEditor.getInt(Settings.KEY_BACKGROUND,
                    Platform.isTv(owner)
                            ? Settings.DEFAULT_BACKGROUND_TV
                            : Settings.DEFAULT_BACKGROUND_VR);
            BitmapDrawable backgroundThemeDrawable = null;
            if (background >= 0 && background < SettingsManager.BACKGROUND_DRAWABLES.length) {

                // Create a cropped image asset for the window background
                Bitmap imageBitmap = BitmapFactory.decodeResource(owner.getResources(), SettingsManager.BACKGROUND_DRAWABLES[background]);

                float aspectScreen;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Rect bounds = owner.getWindowManager().getCurrentWindowMetrics().getBounds();
                    aspectScreen = bounds.width() / (float) bounds.height();
                } else {
                    int heightPixels = Resources.getSystem().getDisplayMetrics().heightPixels;
                    int widthPixels = Resources.getSystem().getDisplayMetrics().widthPixels;
                    aspectScreen = widthPixels / (float) heightPixels;
                }


                float aspectImage  = imageBitmap.getWidth() / (float) imageBitmap.getHeight();
                int cropWidth = imageBitmap.getWidth();
                int cropHeight = imageBitmap.getHeight();

                if (aspectScreen < aspectImage) cropWidth  = (int) (imageBitmap.getHeight() * aspectScreen);
                else                            cropHeight = (int) (imageBitmap.getWidth()  / aspectScreen);

                int cropMarginWidth  =  imageBitmap.getWidth()  - cropWidth;
                int cropMarginHeight =  imageBitmap.getHeight() - cropHeight;

                imageBitmap = Bitmap.createBitmap(imageBitmap, cropMarginWidth, cropMarginHeight,
                        imageBitmap.getWidth() - cropMarginWidth,
                        imageBitmap.getHeight() - cropMarginHeight);
                backgroundThemeDrawable = new BitmapDrawable(Resources.getSystem(), imageBitmap);

            } else {
                File file = new File(owner.getApplicationInfo().dataDir, Settings.CUSTOM_BACKGROUND_PATH);
                try {
                    Bitmap backgroundBitmap = ImageLib.bitmapFromFile(owner, file);
                    backgroundThemeDrawable = new BitmapDrawable(owner.getResources(), backgroundBitmap);
                } catch (Exception e) { e.printStackTrace(); }
            }
            if (backgroundThemeDrawable == null) return;

            if (Platform.isQuest(owner)) {
                backgroundThemeDrawable.setAlpha(owner.dataStoreEditor
                        .getInt(Settings.KEY_BACKGROUND_ALPHA, Settings.DEFAULT_ALPHA));
            }
            // Apply
            BitmapDrawable finalBackgroundThemeDrawable = backgroundThemeDrawable;
            if (executorService.isShutdown()) return;
            owner.runOnUiThread(() -> {
                owner.getWindow().setBackgroundDrawable(finalBackgroundThemeDrawable);
                owner.updateToolBars();
            });
        });
    }
}
