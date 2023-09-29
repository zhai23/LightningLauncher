package com.threethan.launcher.launcher;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import com.threethan.launcher.helper.Platform;

import java.lang.ref.WeakReference;
import java.util.List;

/*
    RecheckPackagesTask

    This task checks a list of installed packages asynchronously, then check if it differs from
    those known by the LauncherActivity which calls it. If so (package installed/uninstalled),
    it tells the LauncherActivity to reload it's list of packages with metadata.
 */

/** @noinspection deprecation */
class RecheckPackagesTask extends AsyncTask<Object, Void, Object> {
    List<ApplicationInfo> foundApps;
    WeakReference<LauncherActivity> ownerRef;
    boolean changeFound;

    @Override
    protected Object doInBackground(Object[] objects) {
        LauncherActivity owner = (LauncherActivity) objects[0];

        PackageManager packageManager = owner.getPackageManager();
        foundApps = packageManager.getInstalledApplications(0);

        changeFound = Platform.installedApps == null ||
                Platform.installedApps.size() != foundApps.size();

        ownerRef = new WeakReference<>(owner);
        return null;

    }
    @Override
    protected void onPostExecute(Object _n) {
        if (changeFound && ownerRef.get() != null) {
            ownerRef.get().reloadPackages();
            ownerRef.get().refreshInterface();
        }
    }
}