package com.threethan.launchercore.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

public class LcBlurCanvas extends LcContainerView {
    private int frameCount;
    private final ViewTreeObserver.OnPreDrawListener listener = () -> {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            if (getChildCount() == 0) return true;
            int height = getChildAt(0).getHeight();
            int width = getChildAt(0).getWidth();

            renderNode.setPosition(0, 0, width, height);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Canvas canvas = renderNode.beginRecording();

                // Draw window background
                drawWindowBackground(canvas);

                // Draw child
                getChildAt(0).draw(canvas);

                // Canvas overlay
                canvas.drawColor(overlayColor);

                renderNode.endRecording();

                renderNode.setRenderEffect(RenderEffect.createBlurEffect(20, 20, Shader.TileMode.CLAMP));

            } else if (frameCount++ % 4 == 0) {
                // Reduces rate & resolution blur for older devices
                final int DOWN_SAMPLE = 10;
                Canvas canvas = renderNode.beginRecording();

                Bitmap bitmap = Bitmap.createBitmap(width/DOWN_SAMPLE, height/DOWN_SAMPLE, Bitmap.Config.ARGB_8888);
                Canvas canvas1 = new Canvas(bitmap);
                canvas1.scale(1f/DOWN_SAMPLE, 1f/DOWN_SAMPLE);
                // Draw window background
                drawWindowBackground(canvas1);
                getChildAt(0).draw(canvas1);

                // Blur bitmap
                blurBitmap(bitmap,50f/DOWN_SAMPLE);

                canvas.scale(DOWN_SAMPLE, DOWN_SAMPLE);
                Paint paint = new Paint();
                paint.setFilterBitmap(true);
                canvas.drawBitmap(bitmap, 0, 0, paint);

                canvas.drawColor(overlayColor);
                renderNode.endRecording();
            }
        }


        return true;
    };

    /** @noinspection deprecation, SameParameterValue */
    private void blurBitmap(Bitmap bitmap, float radius) {
        RenderScript rs = RenderScript.create(getContext());
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation in = Allocation.createFromBitmap(rs, bitmap);
        Allocation out = Allocation.createFromBitmap(rs, bitmap);
        blur.setInput(in);
        blur.setRadius(Math.min(Math.max(radius, 0.1f), 25));
        blur.forEach(out);
        out.copyTo(bitmap);
    }

    private void drawWindowBackground(Canvas canvas) {
        if (getContext() instanceof Activity activity) {
            try {
                Drawable windowBackground = activity.getWindow().getDecorView().getBackground();
                if (windowBackground != null) {
                    windowBackground.draw(canvas);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public LcBlurCanvas(Context context) {
        super(context);
    }

    public LcBlurCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LcBlurCanvas(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private static int overlayColor = Color.TRANSPARENT;

    public static void setOverlayColor(int overlayColor) {
        LcBlurCanvas.overlayColor = overlayColor;
    }

    public static RenderNode renderNode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? new RenderNode("BackgroundNode") : null;

    @Override
    public void addView(View child, int index, LayoutParams params) {
        super.addView(child, index, params);
        child.getViewTreeObserver().addOnPreDrawListener(listener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (getChildCount() > 0)
            getChildAt(0).getViewTreeObserver().removeOnPreDrawListener(listener);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getChildCount() > 0)
            getChildAt(0).getViewTreeObserver().addOnPreDrawListener(listener);
    }
}
