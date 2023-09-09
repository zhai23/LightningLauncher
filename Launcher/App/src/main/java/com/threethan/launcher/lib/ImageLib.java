package com.threethan.launcher.lib;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.esafirm.imagepicker.features.ImagePicker;

import java.io.File;
import java.io.FileOutputStream;

public class ImageLib {

    public static Bitmap getResizedBitmap(Bitmap originalBitmap, int maxSize) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(originalBitmap, width, height, true);
    }

    public static void saveBitmap(Bitmap bitmap, File destinationFile) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(destinationFile);
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showImagePicker(Activity activity, int requestCode) {
        ImagePicker imagePicker = ImagePicker.create(activity).single();
        imagePicker.showCamera(false);
        imagePicker.folderMode(true);
        imagePicker.start(requestCode);
    }

    public static Bitmap bitmapFromDrawable (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
