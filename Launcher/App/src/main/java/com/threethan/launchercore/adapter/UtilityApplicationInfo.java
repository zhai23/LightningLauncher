package com.threethan.launchercore.adapter;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

import androidx.appcompat.content.res.AppCompatResources;

import com.threethan.launchercore.Core;

import java.util.Objects;

public class UtilityApplicationInfo extends ApplicationInfo {
    private final int stringResId;
    private final int drawableResId;
    private final Runnable onLaunch;
    public UtilityApplicationInfo(String packageName, int stringResId, int imageResId, Runnable onLaunch) {
        this.packageName = packageName;
        this.stringResId = stringResId;
        this.drawableResId = imageResId;
        this.onLaunch = onLaunch;
    }

    public String getString() {
        return Core.context().getString(stringResId);
    }

    public Drawable getDrawable() {
        return AppCompatResources.getDrawable(Core.context(), drawableResId);
    }
    public void launch() {
        onLaunch.run();
    }

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
