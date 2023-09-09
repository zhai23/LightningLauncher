package com.threethan.launcher.adapter;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.threethan.launcher.helper.Icon;
import com.threethan.launcher.launcher.LauncherActivity;

/**
 * @noinspection deprecation, rawtypes
 */
class LoadIconTask extends AsyncTask {
    @SuppressLint("StaticFieldLeak")
    private ImageView imageView;
    private Drawable appIcon;

    @Override
    protected Object doInBackground(Object[] objects) {
        final ApplicationInfo currentApp = (ApplicationInfo) objects[1];
        final LauncherActivity launcherActivityContext = (LauncherActivity) objects[2];
        imageView = (ImageView) objects[3];

        ImageView[] imageViews = {imageView};
        appIcon = Icon.loadIcon(launcherActivityContext, currentApp, imageViews);

        return null;
    }

    @Override
    protected void onPostExecute(Object _n) {
        imageView.setImageDrawable(appIcon);
    }
}
