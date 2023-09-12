package com.threethan.launcher.adapter;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.threethan.launcher.helper.Icon;
import com.threethan.launcher.launcher.LauncherActivity;

import java.lang.ref.WeakReference;

/** @noinspection deprecation*/
class LoadIconTask extends AsyncTask <Object, Void, Object> {
    private WeakReference<ImageView> iconImageViewRef;
    private Drawable appIcon;
    @Override
    protected Object doInBackground(Object[] objects) {
        final ApplicationInfo currentApp = (ApplicationInfo) objects[1];
        final LauncherActivity launcherActivityContext = (LauncherActivity) objects[2];
        final ImageView iconImageView = (ImageView) objects[3];
        iconImageViewRef = new WeakReference<>(iconImageView);
        appIcon = Icon.loadIcon(launcherActivityContext, currentApp, iconImageView);
        return null;
    }
    @Override
    protected void onPostExecute(Object _n) {
        iconImageViewRef.get().setImageDrawable(appIcon);
    }
}
