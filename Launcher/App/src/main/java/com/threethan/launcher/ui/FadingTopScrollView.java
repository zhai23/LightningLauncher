package com.threethan.launcher.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class FadingTopScrollView extends ScrollView {

    public FadingTopScrollView(Context context) {
        super(context);
    }

    public FadingTopScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FadingTopScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override public float getBottomFadingEdgeStrength() {
        return 0.0f;
    }

    @Override public float getTopFadingEdgeStrength() {
        return 1.0f;
    }

}