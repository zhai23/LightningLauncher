package com.threethan.launchercore.lib;

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

/** @noinspection unused*/
public class ImageLib {

    public static Bitmap getResizedBitmap(Bitmap originalBitmap, int maxHeight) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;

        height = maxHeight;
        width = (int) (height * bitmapRatio);
        return Bitmap.createScaledBitmap(originalBitmap, width, height, true);
    }

    public static void saveBitmap(Bitmap bitmap, File destinationFile) {
        FileOutputStream fileOutputStream;
        try {
            //noinspection ResultOfMethodCallIgnored
            Objects.requireNonNull(destinationFile.getParentFile()).mkdirs();
            fileOutputStream = new FileOutputStream(destinationFile);
            bitmap.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
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

    /**
     * Quickly checks if two awt BufferedImages are identical.
     * May produce false positives, but never false negatives!
     * @param a First image
     * @param b Second image
     * @return True if images are probably identical
     */
    public static boolean isIdenticalFast(Bitmap a, Bitmap b) {
        if (a == null || b == null) return false;
        final int w = a.getWidth();
        final int h = a.getHeight();
        // If dimensions don't match, images are necessarily different
        //noinspection DuplicatedCode
        if (w != b.getWidth() || h != b.getHeight()) return false;
        // Sample some arbitrary points on the image and check if they're identical
        final int N_SAMPLES = 128; // Number of points to sample
        final float ym = 6.9F; // # should be relatively aperiodic for best result
        final float xm = 5.1F; // # should be relatively aperiodic for best result
        final float dx = (w * xm / (N_SAMPLES+1));
        final float dy = (h * ym / (N_SAMPLES+1));
        // Sample N_SAMPLES points in looping diagonals across the image,
        //  which should provide a semi-random but distributed set of samples
        //   with which to compare the images
        for (int i = 0; i < N_SAMPLES; i++) {
            final int y = (int)(i * dy) % h;
            final int x = (int) ((((int)(i * dy / h)) + dx * i) % w);
            if (a.getPixel(x,y) != b.getPixel(x,y)) return false;
        }
        return true;
    }
}
