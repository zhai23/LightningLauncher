package com.threethan.launcher.updater;

import android.app.Activity;

import com.threethan.launcher.R;
import com.threethan.launcher.activity.dialog.BasicDialog;
import com.threethan.launcher.activity.support.DataStoreEditor;
import com.threethan.launchercore.Core;
import com.threethan.launchercore.util.CustomDialog;

public class LauncherUpdater extends AppUpdater {
    private static final String GIT_REPO_LAUNCHER = "threethan/LightningLauncher";

    @Override
    protected String getAppDisplayName() {
        return "Lightning Launcher";
    }

    // URL Constants
    @Override
    protected String getAppDownloadName() {
        return "LightningLauncher"; // Name of apk on github, not including ".apk"
    }

    @Override
    protected String getGitRepo() {
        return GIT_REPO_LAUNCHER;
    }
    private static final String KEY_IGNORED_UPDATE_TAG = "IGNORED_UPDATE_TAG";
    private static final String KEY_SHOULD_NOTIFY_UPDATES = "SHOULD_NOTIFY_UPDATES";
    @Override
    protected void putIgnoredUpdateTag(String ignoredUpdateTag) {
        new DataStoreEditor(activity).putString(KEY_IGNORED_UPDATE_TAG, ignoredUpdateTag);
    }
    @Override
    protected String getIgnoredUpdateTag() {
        return new DataStoreEditor(activity).getString(KEY_IGNORED_UPDATE_TAG, "");
    }

    public static void getShouldNotifyUpdates(io.reactivex.rxjava3.functions.Consumer<Boolean> then) {
        new DataStoreEditor(Core.context()).getBoolean(KEY_SHOULD_NOTIFY_UPDATES, true, then);
    }
    public static void setShouldNotifyUpdates(boolean value) {
        new DataStoreEditor(Core.context()).putBoolean(KEY_SHOULD_NOTIFY_UPDATES, value);
    }

    @Override
    public void skipAppUpdate(String versionTag) {
        new CustomDialog.Builder(activity)
                .setTitle(activity.getString(R.string.update_skip_title, versionTag))
                .setMessage(R.string.update_skip_content)
                .setPositiveButton(R.string.update_skip_confirm_button, (dialog, i) -> {
                    putIgnoredUpdateTag(versionTag);
                    BasicDialog.toast(activity.getString(R.string.update_skip_toast), versionTag, false);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.update_skip_all_button, (dialog, i) -> {
                    setShouldNotifyUpdates(false);
                    BasicDialog.toast(activity.getString(R.string.update_skip_all_toast));
                    dialog.dismiss();
                })
                .setNeutralButton(R.string.update_skip_cancel_button, ((dialog, i) -> dialog.dismiss()))
                .show();
    }

    @Override
    protected void showAppUpdateDialog(String currentVersion, String newVersion) {
        getShouldNotifyUpdates(s -> {
            if (s) super.showAppUpdateDialog(currentVersion, newVersion);
        });
    }

    public LauncherUpdater(Activity activity) {
        super(activity);
    }
}
