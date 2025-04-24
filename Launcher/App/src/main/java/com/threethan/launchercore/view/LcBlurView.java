package com.threethan.launchercore.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * FrameLayout that blurs its underlying content.
 * Can have children and draw them over blurred background.
 */
public class LcBlurView extends FrameLayout {
    public LcBlurView(Context context) {
        super(context);
    }

    public LcBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LcBlurView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void draw(Canvas canvas) {
        int[] position = new int[2];
        getLocationInWindow(position);

        // Clear canvas
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        // Draw blurred content
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            canvas.translate(-position[0], -position[1]);
            canvas.drawRenderNode(LcBlurCanvas.renderNode);
            canvas.translate(position[0], position[1]);
        }

        super.draw(canvas);
    }
}
