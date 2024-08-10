package com.threethan.launcher.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;

import com.threethan.launcher.BuildConfig;
import com.threethan.launchercore.lib.ImageLib;

import java.io.File;

public abstract class ActivityCapture {

    public static Bitmap takeCapture(Activity activity) {
        View root = activity.getWindow().getDecorView().getRootView();
        root.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(root.getDrawingCache());
        root.setDrawingCacheEnabled(false);
        return bitmap;
    }
    public static void takeAndStoreCapture(Activity activity) {
        String fileName = BuildConfig.APPLICATION_ID + "_"
                + BuildConfig.VERSION_NAME
                + "_" + System.currentTimeMillis() + ".png";
        File downloadsDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        ImageLib.saveBitmap(takeCapture(activity), new File(downloadsDir, fileName));
    }

}
