package com.threethan.launchercore.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import eightbitlab.com.blurview.BlurAlgorithm;
import eightbitlab.com.blurview.BlurController;
import eightbitlab.com.blurview.RenderScriptBlur;

public abstract class TranslucentBlur {
    public static BlurAlgorithm getInstance(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new RenderEffectTranslucentBlur(context);
        } else {
            //noinspection deprecation
            return new RenderScriptBlur(context);
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public static class RenderEffectTranslucentBlur implements BlurAlgorithm {

        private final RenderNode node = new RenderNode("BlurViewNode");

        private int height, width;
        private float lastBlurRadius = 1f;

        @Nullable
        public BlurAlgorithm fallbackAlgorithm;
        private final Context context;

        public RenderEffectTranslucentBlur(Context context) {
            this.context = context;
        }

        @Override
        public Bitmap blur(@NonNull Bitmap bitmap, float blurRadius) {
            lastBlurRadius = blurRadius;

            if (bitmap.getHeight() != height || bitmap.getWidth() != width) {
                height = bitmap.getHeight();
                width = bitmap.getWidth();
                node.setPosition(0, 0, width, height);
            }
            Canvas canvas = node.beginRecording();
            canvas.drawBitmap(bitmap, 0, 0, null);
            node.endRecording();
            node.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.MIRROR));
            // returning not blurred bitmap, because the rendering relies on the RenderNode
            return bitmap;
        }

        @Override
        public void destroy() {
            node.discardDisplayList();
            if (fallbackAlgorithm != null) {
                fallbackAlgorithm.destroy();
            }
        }

        @Override
        public boolean canModifyBitmap() {
            return true;
        }

        @NonNull
        @Override
        public Bitmap.Config getSupportedBitmapConfig() {
            return Bitmap.Config.ARGB_8888;
        }

        @Override
        public float scaleFactor() {
            return BlurController.DEFAULT_SCALE_FACTOR;
        }

        @Override
        public void render(@NonNull Canvas canvas, @NonNull Bitmap bitmap) {
            canvas.drawColor(0x0, PorterDuff.Mode.CLEAR);

            if (canvas.isHardwareAccelerated()) {
                canvas.drawRenderNode(node);
            } else {
                if (fallbackAlgorithm == null) {
                    //noinspection deprecation
                    fallbackAlgorithm = new RenderScriptBlur(context);
                }
                fallbackAlgorithm.blur(bitmap, lastBlurRadius);
                fallbackAlgorithm.render(canvas, bitmap);
            }

        }
    }

}


