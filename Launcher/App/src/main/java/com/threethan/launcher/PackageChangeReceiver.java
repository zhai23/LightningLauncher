package com.threethan.launcher;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.threethan.launcher.activity.LauncherActivity;

public class PackageChangeReceiver extends BroadcastReceiver {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        LauncherActivity inst = LauncherActivity.getForegroundInstance();
        if (inst != null)
            inst.forceRefreshPackages();
    }
}
