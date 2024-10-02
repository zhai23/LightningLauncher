package com.threethan.launcher.helper;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.threethan.launcher.activity.LauncherActivity;

public class PackageChangeReceiver extends BroadcastReceiver {
    @SuppressLint("UnsafeProtectedBroadcastReceiver") // No security risk to a package refresh
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            LauncherActivity.getForegroundInstance().launcherService
                    .forEachActivity(LauncherActivity::refreshPackages);
        } catch (Exception ignored) {}
    }
}
