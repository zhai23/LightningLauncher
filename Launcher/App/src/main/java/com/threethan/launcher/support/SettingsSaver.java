package com.threethan.launcher.support;

import android.content.ContextWrapper;
import android.os.Environment;

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.FileLib;

import java.io.File;
import java.util.Objects;

// This class is a stub for if/when I actually get around to implementing backups

public abstract class SettingsSaver {
    public static void save(LauncherActivity activity) {
        File ff = new File(activity.getFilesDir().getParent()
                + "/shared_prefs/" +
                activity.getPackageName() + "_preferences.xml");
        File docs = new ContextWrapper(activity).getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        assert docs != null;
        //noinspection ResultOfMethodCallIgnored
        Objects.requireNonNull(docs.getParentFile()).mkdirs();
        FileLib.copy(ff, new File(docs.getPath(),"LightningLauncher.xml"));
    }


    //TODO: This doesn't work
    public synchronized static void load(LauncherActivity activity) {
        // TODO: Actually serialize stuff
    }
    public static boolean canLoad(LauncherActivity activity) {
        ContextWrapper cw = new ContextWrapper(activity);
        File docs = new File(Objects.requireNonNull(
                cw.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).getPath()+"/LightningLauncher.xml");
        return docs.exists();
    }
}
