package com.threethan.launcher.updater;

import android.app.Activity;

public class FileManagerUpdater extends AppUpdater {
    public static final String GIT_REPO = "TeamAmaze/AmazeFileManager";
    // Will be prompted to update from LL if version code is less than this

    // URL Constants
    @Override
    protected String getAppDownloadName() {
        // Name of apk on github, not including ".apk"
        return "app-fdroid-release";
    }

    @Override
    protected String getAppPackageName() {
        return "com.amaze.filemanager";
    }

    @Override
    protected String getAppDisplayName() {
        return "Amaze File Manager";
    }

    @Override
    protected String getGitRepo() {
        return GIT_REPO;
    }
    @Override
    protected void putIgnoredUpdateTag(String ignoredUpdateTag) {
    }
    @Override
    protected String getIgnoredUpdateTag() {
        return "";
    }

    public FileManagerUpdater(Activity activity) {
        super(activity);
    }
}
