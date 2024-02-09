package com.threethan.launcher.lib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

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
        FileOutputStream fileOutputStream;
        try {
            //noinspection ResultOfMethodCallIgnored
            Objects.requireNonNull(destinationFile.getParentFile()).mkdirs();
            fileOutputStream = new FileOutputStream(destinationFile);
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap bitmapFromDrawable (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable)drawable).getBitmap();

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Nullable
    public static Bitmap bitmapFromFile (File file) {
        return BitmapFactory.decodeFile(file.getPath());
    }

    @Nullable
    public static Bitmap bitmapFromStream(InputStream stream) {
        final Bitmap bitmap = BitmapFactory.decodeStream(stream);
        try { stream.close(); } catch (IOException ignored) {}
        return bitmap;
    }
}
