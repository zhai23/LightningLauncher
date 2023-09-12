package com.threethan.launcher.launcher;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.support.SettingsManager;

import java.io.File;

/** @noinspection deprecation */
class BackgroundTask extends AsyncTask<Object, Void, Object> {

    Drawable backgroundThemeDrawable;
    @SuppressLint("StaticFieldLeak")
    LauncherActivity owner;
    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected Object doInBackground(Object... objects) {
        owner = (LauncherActivity) objects[0];
        int background = owner.sharedPreferences.getInt(Settings.KEY_BACKGROUND, Settings.DEFAULT_BACKGROUND);
        if (background >= 0 && background < SettingsManager.BACKGROUND_DRAWABLES.length) {
            backgroundThemeDrawable = owner.getDrawable(SettingsManager.BACKGROUND_DRAWABLES[background]);
        } else {
            File file = new File(owner.getApplicationInfo().dataDir, Settings.CUSTOM_BACKGROUND_PATH);
            try {
                Bitmap backgroundBitmap = ImageLib.bitmapFromFile(owner, file);
                backgroundThemeDrawable = new BitmapDrawable(owner.getResources(), backgroundBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object _n) {
        owner.post(() -> owner.backgroundImageView.setImageDrawable(backgroundThemeDrawable));
    }

}
