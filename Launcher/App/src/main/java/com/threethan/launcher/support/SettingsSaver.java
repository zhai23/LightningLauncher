package com.threethan.launcher.support;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.DataLib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        copyFile(ff, new File(docs.getPath(),"LightningLauncher.xml"));
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

    /** @noinspection IOStreamConstructor*/
    private static void copyFile(File fIn, File fOut) {
        try {
            InputStream in = new FileInputStream(fIn);
            OutputStream out = new FileOutputStream(fOut);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v("PATH", fOut.getAbsolutePath());
    }
}
