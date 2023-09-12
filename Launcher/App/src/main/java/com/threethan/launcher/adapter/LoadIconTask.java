package com.threethan.launcher.adapter;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.threethan.launcher.helper.Icon;
import com.threethan.launcher.launcher.LauncherActivity;

/** @noinspection deprecation*/
class LoadIconTask extends AsyncTask <Object, Void, Object> {
    @SuppressLint("StaticFieldLeak")
    private ImageView iconImageView;
    private Drawable appIcon;

    @Override
    protected Object doInBackground(Object[] objects) {
        final ApplicationInfo currentApp = (ApplicationInfo) objects[1];
        final LauncherActivity launcherActivityContext = (LauncherActivity) objects[2];
        iconImageView = (ImageView) objects[3];
        appIcon = Icon.loadIcon(launcherActivityContext, currentApp, iconImageView);
        return null;
    }
    @Override
    protected void onPostExecute(Object _n) {
        iconImageView.setImageDrawable(appIcon);
    }
}
