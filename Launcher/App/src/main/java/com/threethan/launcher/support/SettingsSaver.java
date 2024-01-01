package com.threethan.launcher.support;

import android.app.Activity;

import androidx.datastore.DataStoreFile;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.FileLib;

import java.io.File;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Backs up and loads settings between the internal {@link androidx.datastore.core.DataStore}
 * and an arbitrary user-accessible file in android/data/
 */
public abstract class SettingsSaver {
    private static final String DATA_STORE_NAME = "default";
    public static final String EXPORT_FILE_NAME = "ExportedConfiguration.xml";

    /**
     * Saves the contents of the DataStore to a file
     * @param activity used for getting package name and data store paths
     */
    public static void save(Activity activity) {

        File prefs = DataStoreFile.dataStoreFile(activity, DATA_STORE_NAME+".preferences_pb");
        File exportPath = activity.getExternalFilesDir("");
        File export = new File(exportPath, EXPORT_FILE_NAME);
        assert exportPath != null;

        boolean ignored1 = Objects.requireNonNull(export.getParentFile()).mkdirs();
        FileLib.delete(export);

        if (FileLib.copy(prefs, export))
            Dialog.toast(activity.getString(R.string.saved_settings),
                "Android/Data/"+activity.getPackageName()+"/"+SettingsSaver.EXPORT_FILE_NAME,
                false);
        else Dialog.toast(activity.getString(R.string.saved_settings_error));
    }
    /**
     * Loads the contents of the DataStore from a file.
     * <p>
     * Warning: No error checking is done!
     * @param activity used for getting package name and data store paths
     */
    public synchronized static void load(Activity activity) {
        File prefs = DataStoreFile.dataStoreFile(activity, DATA_STORE_NAME);
        File exportPath = activity.getExternalFilesDir("");
        File export = new File(exportPath, EXPORT_FILE_NAME);
        assert exportPath != null;
        final boolean ignored = Objects.requireNonNull(exportPath.getParentFile()).mkdirs();

        FileLib.delete(prefs);
        if (FileLib.copy(export, prefs)) {
            Dialog.toast(activity.getString(R.string.loaded_settings1),
                    activity.getString(R.string.loaded_settings2),
                    false);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    int pid = android.os.Process.myPid();
                    android.os.Process.killProcess(pid);
                }
            }, 1500);
        } else Dialog.toast(activity.getString(R.string.saved_settings_error));
    }

    /**
     * Check if there is a valid file from which we can load a backup
     * @param activity used for getting package name and data store paths
     * @return If the file exists in the correct location
     */
    public static boolean canLoad(LauncherActivity activity) {
        File exportPath = activity.getExternalFilesDir("");
        File export = new File(exportPath, EXPORT_FILE_NAME);
        return export.exists();
    }
}
