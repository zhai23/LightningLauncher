package com.threethan.launchercore.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;

public abstract class LcToolTipHelper {
    private static final int DEFAULT_TOOLTIP_DELAY_MS = 250;

    public static void init(View view, @Nullable AttributeSet attrs) {
        int tooltipTextResId = -1;
        CharSequence tooltipText = null;
        final PopupWindow[] popupWindow = {null};
        if (attrs != null) {
            //noinspection resource
            TypedArray a = view.getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.LcText,
                    0, 0);
            try {
                tooltipTextResId = a.getResourceId(R.styleable.LcText_android_tooltipText, -1);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    view.setTooltipText(null);
            } finally {
                a.recycle();
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && view.getTooltipText() != null) {
            tooltipText = view.getTooltipText();
            view.setTooltipText(null);
        }

        if (tooltipTextResId == -1 && tooltipText == null) return;
        int finalTooltipTextResId = tooltipTextResId;
        CharSequence finalTooltipText = tooltipText;
        Runnable showToolTip = () -> {
            if (popupWindow[0] != null) return;
            LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("InflateParams") View tooltipView = inflater.inflate(R.layout.lc_tooltip, null);
            TextView text = tooltipView.findViewById(R.id.tooltipText);
            if (finalTooltipText != null) text.setText(finalTooltipText);
            else text.setText(finalTooltipTextResId);
            popupWindow[0] = new PopupWindow(tooltipView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tooltipView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            popupWindow[0].showAsDropDown(view, view.getWidth()/2-tooltipView.getMeasuredWidth()/2, -5);
        };
        final Handler handler = new Handler(Looper.getMainLooper());
        view.setOnHoverListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                handler.postDelayed(showToolTip, DEFAULT_TOOLTIP_DELAY_MS);
            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                handler.removeCallbacks(showToolTip);
                if (popupWindow[0] != null) popupWindow[0].dismiss();
            }
            return false;
        });
    }
}
