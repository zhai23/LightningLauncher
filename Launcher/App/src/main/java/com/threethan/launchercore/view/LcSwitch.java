package com.threethan.launchercore.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.Switch;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.threethan.launcher.R;

@SuppressLint("UseSwitchCompatOrMaterialCode")
public class LcSwitch extends Switch {
    public LcSwitch(Context context) {
        super(context);
        init(null);
    }

    public LcSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public LcSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    protected void init(AttributeSet attrs) {
        if (attrs != null) {
            //noinspection resource
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.LcSwitch,
                    0, 0);
            try {
                // Get custom attributes here
                int textColor = a.getColor(R.styleable.LcSwitch_android_textColor, Color.WHITE);
                int textSize = a.getDimensionPixelSize(R.styleable.LcSwitch_android_textSize, (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics()));
                int background = a.getResourceId(R.styleable.LcSwitch_android_background, R.drawable.lc_bkg_button);

                // Apply the custom attributes
                setTextColor(textColor);
                setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                setBackground(getDrawable(background));
                setThumbDrawable(getDrawable(R.drawable.switch_thumb_custom));
                setTrackDrawable(getDrawable(R.drawable.switch_track_custom));
            } finally {
                a.recycle();
            }
        }
        LcToolTipHelper.init(this, attrs);
    }


    private Drawable getDrawable(@DrawableRes int res) {
        return ResourcesCompat.getDrawable(getResources(), res, getContext().getTheme());
    }

    @Override
    public void setTooltipText(@Nullable CharSequence tooltipText) {
        LcToolTipHelper.init(this, tooltipText);
    }
}
