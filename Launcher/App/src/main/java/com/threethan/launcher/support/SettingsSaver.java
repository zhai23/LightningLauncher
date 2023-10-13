package com.threethan.launcher.support;

import android.content.ContextWrapper;
import android.os.Environment;

import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.FileLib;

import java.io.File;
import java.util.Objects;

// This class is a stub for if/when I actually get around to implementing backups

public abstract class SettingsSaver {
    public static String SETTINGS_FILE_NAME = "ExportedSettings.xml";
    public static void save(LauncherActivity activity) {
        File prefs = new File(activity.getFilesDir().getParent()
                + "/shared_prefs/" +
                activity.getPackageName() + "_preferences.xml");
        File exportPath = activity.getExternalFilesDir("");
        File export = new File(exportPath,SETTINGS_FILE_NAME);
        assert exportPath != null;
        final boolean ignored = Objects.requireNonNull(exportPath.getParentFile()).mkdirs();

        FileLib.delete(export);
        FileLib.copy(prefs, export);

        Dialog.toast("Saved settings to","Android/Data/com.threethan.launcher/ExportedSettings.xml");
    }
    public synchronized static void load(LauncherActivity activity) {
        File prefs = new File(activity.getFilesDir().getParent()
                + "/shared_prefs/" +
                activity.getPackageName() + "_preferences.xml");
        File exportPath = activity.getExternalFilesDir("");
        File export = new File(exportPath,SETTINGS_FILE_NAME);
        assert exportPath != null;
        final boolean ignored = Objects.requireNonNull(exportPath.getParentFile()).mkdirs();

        FileLib.delete(prefs);
        FileLib.copy(export, prefs);

        Dialog.toast("Imported settings,","launcher will close!");

        activity.postDelayed(() -> {
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
        }, 1000);

    }
    public static boolean canLoad(LauncherActivity activity) {
        File exportPath = activity.getExternalFilesDir("");
        File export = new File(exportPath,SETTINGS_FILE_NAME);
        return export.exists();
    }
}
