package com.threethan.launcher.support;

import androidx.datastore.DataStoreFile;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.FileLib;

import java.io.File;
import java.util.Objects;

// This class is a stub for if/when I actually get around to implementing backups

public abstract class SettingsSaver {
    public static String CONFIG_FILE_NAME = "ExportedConfiguration.xml";
    public static void save(LauncherActivity activity) {

        File prefs = DataStoreFile.dataStoreFile(activity, "default.preferences_pb");
        File exportPath = activity.getExternalFilesDir("");
        File export = new File(exportPath, CONFIG_FILE_NAME);
        assert exportPath != null;

        boolean ignored1 = Objects.requireNonNull(export.getParentFile()).mkdirs();
        FileLib.delete(export);

        if (FileLib.copy(prefs, export))
            Dialog.toast(activity.getString(R.string.saved_settings),
                "Android/Data/"+activity.getPackageName()+"/"+SettingsSaver.CONFIG_FILE_NAME,
                false);
        else Dialog.toast(activity.getString(R.string.saved_settings_error));
    }
    public synchronized static void load(LauncherActivity activity) {
        File prefs = DataStoreFile.dataStoreFile(activity, "default");
        File exportPath = activity.getExternalFilesDir("");
        File export = new File(exportPath, CONFIG_FILE_NAME);
        assert exportPath != null;
        final boolean ignored = Objects.requireNonNull(exportPath.getParentFile()).mkdirs();

        FileLib.delete(prefs);
        if (FileLib.copy(export, prefs)) {
            Dialog.toast(activity.getString(R.string.loaded_settings1),
                    activity.getString(R.string.loaded_settings2),
                    false);
            activity.postDelayed(() -> {
                int pid = android.os.Process.myPid();
                android.os.Process.killProcess(pid);
            }, 1000);
        } else Dialog.toast(activity.getString(R.string.saved_settings_error));
    }
    public static boolean canLoad(LauncherActivity activity) {
        File exportPath = activity.getExternalFilesDir("");
        File export = new File(exportPath, CONFIG_FILE_NAME);
        return export.exists();
    }
}
