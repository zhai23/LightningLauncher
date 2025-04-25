package com.threethan.launcher.updater;

import static com.threethan.launcher.updater.RemotePackageUpdater.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

public class InstallReceiver extends BroadcastReceiver {
    private static Runnable onFail = null;
    private static Runnable onSuccess = null;

    public static void setOnSuccess(Runnable onSuccess) {
        InstallReceiver.onSuccess = onSuccess;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE);

        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                assert confirmIntent != null;
                context.startActivity(confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case PackageInstaller.STATUS_SUCCESS:
                if (onSuccess != null) onSuccess.run();
                break;
            case PackageInstaller.STATUS_FAILURE:
            case PackageInstaller.STATUS_FAILURE_ABORTED:
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
            case PackageInstaller.STATUS_FAILURE_INVALID:
            case PackageInstaller.STATUS_FAILURE_STORAGE:
            case PackageInstaller.STATUS_FAILURE_TIMEOUT:
                String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                Log.e(TAG, "Session-based install failed with status "
                        + status + ": " + message);
                if (onFail != null) onFail.run();
                onFail = null;
                break;
        }
    }
}
