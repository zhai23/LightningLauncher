package com.threethan.launchercore.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

public class LcBlurCanvas extends LcContainerView {
    private final ViewTreeObserver.OnPreDrawListener listener = () -> {
        if (getChildCount() == 0) return true;
        int height = getChildAt(0).getHeight();
        int width = getChildAt(0).getWidth();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            renderNode.setPosition(0, 0, width, height);
            Canvas canvas = renderNode.beginRecording();

            // Draw window background
            if (getContext() instanceof Activity activity) {
                try {
                    Drawable windowBackground = activity.getWindow().getDecorView().getBackground();
                    if (windowBackground != null) {
                        windowBackground.draw(canvas);
                    }
                } catch (Exception ignored) {}
            }

            // Draw child
            getChildAt(0).draw(canvas);

            // Canvas overlay
            canvas.drawColor(overlayColor);

            renderNode.endRecording();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                renderNode.setRenderEffect(RenderEffect.createBlurEffect(20, 20, Shader.TileMode.CLAMP));

        }

        return true;
    };

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
