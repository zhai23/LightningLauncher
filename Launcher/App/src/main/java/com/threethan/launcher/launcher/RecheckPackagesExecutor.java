package com.threethan.launcher.launcher;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.threethan.launcher.helper.Platform;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
    This task checks a list of installed packages asynchronously, then check if it differs from
    those known by the LauncherActivity which calls it. If so (package installed/uninstalled),
    it tells the LauncherActivity to reload it's list of packages with metadata.
 */

class RecheckPackagesExecutor {
    public void execute(LauncherActivity owner) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {

            PackageManager packageManager = owner.getPackageManager();
            List<ApplicationInfo> foundApps = packageManager.getInstalledApplications(0);

            if (Platform.installedApps == null) {
                Log.v("Lightning Launcher", "Package check called before initial load, " +
                        "will be ignored");
                return;
            }
            if (Platform.installedApps.size() != foundApps.size()) {
                owner.runOnUiThread(() -> {
                    Log.v("Lightning Launcher", "Package change detected!");
                    owner.reloadPackages();
                });
            }
        });
    }
}