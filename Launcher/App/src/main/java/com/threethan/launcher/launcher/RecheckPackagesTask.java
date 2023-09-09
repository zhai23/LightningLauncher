package com.threethan.launcher.launcher;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

/** @noinspection deprecation */
class RecheckPackagesTask extends AsyncTask<Object, Void, Object> {

    List<ApplicationInfo> foundApps;
    @SuppressLint("StaticFieldLeak")
    LauncherActivity owner;
    boolean changeFound;
    @Override
    protected Object doInBackground(Object[] objects) {
        owner = (LauncherActivity) objects[0];

        PackageManager packageManager = owner.getPackageManager();
        foundApps = packageManager.getInstalledApplications(0);

        changeFound = owner.installedApps == null || owner.installedApps.size() != foundApps.size();
        return null;
    }
    @Override
    protected void onPostExecute(Object _n) {
        if (changeFound) {
            Log.i("PackageCheck", "Package change detected!");
            owner.reloadPackagesWithMeta();
            owner.refresh();
        }
    }
}
