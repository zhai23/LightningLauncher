package com.threethan.launcher.activity.chainload;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.threethan.launcher.helper.PlatformExt;
import com.threethan.launchercore.lib.DelayLib;
import com.threethan.launchercore.util.Launch;
import com.threethan.launchercore.util.Platform;

import java.util.Objects;

// These activities are used for advanced custom window sizes

public class ChainLoadActivity extends Launch.LaunchingActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        ApplicationInfo launchApp = Objects.requireNonNull(extras.getParcelable("app"));

        // Get normal launch intent
        PackageManager pm = getPackageManager();
        Intent normalIntent = pm.getLaunchIntentForPackage(launchApp.packageName);

        if (normalIntent != null) {
            if (Platform.getVrOsVersion() >= 74 || PlatformExt.isOldVrOs()) {
                normalIntent.setFlags(0);
                startActivityFromChild(this, normalIntent, 0);
            } else {
                normalIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(normalIntent);
            }
        }
        Intent relaunch = pm.getLaunchIntentForPackage(getPackageName());

        if (PlatformExt.useNewVrOsMultiWindow()) setOnPostDestroy(()
                -> DelayLib.delayed(()
                -> startActivity(relaunch)));

        finishAffinity();
    }
}
