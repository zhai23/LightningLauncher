package com.threethan.launcher.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.View;

import com.threethan.launcher.BuildConfig;
import com.threethan.launchercore.lib.ImageLib;

import java.io.File;
import java.util.function.Consumer;

public abstract class ActivityCapture {
    /**
     * Capture the current state of the screen to a bitmap using PixelCopy.
     * <br>
     * <i>This may not always work, and no feedback is provided on failure.</i>
     * <br>
     * This is intended for development use and promotional screenshots.
     * @param activity  Activity to capture
     * @param onSuccess Called with the screenshot bitmap object, if:
     *                  API >= 26 and the screenshot was taken successfully
     */
    public static void takeCapture(Activity activity, final Consumer<Bitmap> onSuccess) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity.getWindow() == null) return;

        final View root = activity.getWindow().getDecorView().getRootView();
        final Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        final Handler handler = new Handler(Looper.getMainLooper());

        PixelCopy.request(activity.getWindow(), bitmap, copyResult -> {
            if (copyResult == PixelCopy.SUCCESS)
                onSuccess.accept(bitmap);
        }, handler);
    }

    /**
     * Captures a screenshot, and automatically names it & saves it into the downloads folder
     * <i>Nothing happens if this fails.</i>
     * @param activity Activity to capture
     */
    public static void takeAndStoreCapture(Activity activity) {
        String fileName = BuildConfig.APPLICATION_ID + "_"
                + BuildConfig.VERSION_NAME
                + "_" + System.currentTimeMillis() + ".webp";
        File downloadsDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        takeCapture(activity, b -> ImageLib.saveBitmap(b, new File(downloadsDir, fileName)));
    }
}
