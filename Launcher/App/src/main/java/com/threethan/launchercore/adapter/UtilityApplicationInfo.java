package com.threethan.launchercore.adapter;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

import com.threethan.launchercore.Core;

import java.util.Objects;

public class UtilityApplicationInfo extends ApplicationInfo {
    private final int drawableResId;
    public UtilityApplicationInfo(String packageName, int imageResId) {
        this.packageName = packageName;
        this.drawableResId = imageResId;
    }

    public Drawable getDrawable() {
        return Core.context().getDrawable(drawableResId);
    }
    public void launch() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtilityApplicationInfo that = (UtilityApplicationInfo) o;
        return Objects.equals(packageName, that.packageName);
    }
    @Override
    public int hashCode() {
        return Objects.hash(packageName);
    }
}
