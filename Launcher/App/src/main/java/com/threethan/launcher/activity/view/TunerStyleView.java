package com.threethan.launcher.activity.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.threethan.launcher.activity.LauncherActivity;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class TunerStyleView extends View {
    public static List<WeakReference<TunerStyleView>> instances = new LinkedList<>();

    public TunerStyleView(Context context) {
        super(context);
        init();
    }

    public TunerStyleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TunerStyleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        instances.add(new WeakReference<>(this));
        setVisibility(visible ? VISIBLE : INVISIBLE);
    }
    private static boolean visible = false;
    public static void setAllVisible(boolean visible) {
        TunerStyleView.visible = visible;
        try {
            Objects.requireNonNull(LauncherActivity.getForegroundInstance())
                    .launcherService.forEachActivity(a -> a.runOnUiThread(() -> {
                for (WeakReference<TunerStyleView> instance : instances) {
                    try {
                        instance.get().setVisibility(visible ? VISIBLE : INVISIBLE);
                    } catch (Exception ignored) {}
                }
            }));
        } catch (Exception ignored) {}
    }
}
