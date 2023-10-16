package com.threethan.launcher.launcher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.support.SettingsManager;

import java.io.File;
import java.lang.ref.WeakReference;

/*
    BackgroundTask

    This task loads an image background asynchronously, then sets it to the specified view when done
    Used only by LauncherActivity
 */

/** @noinspection deprecation */
class BackgroundTask extends AsyncTask<Object, Void, Object> {

    Drawable backgroundThemeDrawable;
    WeakReference<LauncherActivity> ownerRef;
    @Override
    protected Object doInBackground(Object... objects) {
        LauncherActivity owner = (LauncherActivity) objects[0];
        int background = owner.sharedPreferences.getInt(Settings.KEY_BACKGROUND,
                Platform.isTv(owner)
                        ? Settings.DEFAULT_BACKGROUND_TV
                        : Settings.DEFAULT_BACKGROUND_VR);
        if (background >= 0 && background < SettingsManager.BACKGROUND_DRAWABLES.length) {
//            backgroundThemeDrawable = ContextCompat.getDrawable(owner, SettingsManager.BACKGROUND_DRAWABLES[background]);

            // Create a cropped image asset for the window background
            Bitmap imageBitmap = BitmapFactory.decodeResource(owner.getResources(), SettingsManager.BACKGROUND_DRAWABLES[background]);

            float aspectScreen = owner.getWindowManager().getDefaultDisplay().getWidth() /
                    (float) owner.getWindowManager().getDefaultDisplay().getHeight();
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
            backgroundThemeDrawable = new BitmapDrawable(imageBitmap);

        } else {
            File file = new File(owner.getApplicationInfo().dataDir, Settings.CUSTOM_BACKGROUND_PATH);
            try {
                Bitmap backgroundBitmap = ImageLib.bitmapFromFile(owner, file);
                backgroundThemeDrawable = new BitmapDrawable(owner.getResources(), backgroundBitmap);
            } catch (Exception e) { e.printStackTrace(); }
        }
        ownerRef = new WeakReference<>(owner);
        return null;
    }

    @Override
    protected void onPostExecute(Object _n) {
        LauncherActivity owner = ownerRef.get();
        if (owner != null) owner.getWindow().setBackgroundDrawable(backgroundThemeDrawable);
        if (owner != null) owner.updateToolBars();
    }

}
