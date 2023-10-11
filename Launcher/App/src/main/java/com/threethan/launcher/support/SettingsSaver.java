package com.threethan.launcher.support;

import android.app.Activity;
import android.content.ContextWrapper;
import android.os.Environment;
import android.preference.PreferenceManager;

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

public abstract class SettingsSaver {
    public static void save(LauncherActivity activity) {
        File ff = new File(activity.getFilesDir().getParent()
                + "/shared_prefs/" +
                activity.getPackageName() + "_preferences.xml");
        File docs = new ContextWrapper(activity).getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        //noinspection ResultOfMethodCallIgnored
        docs.mkdirs();
        copyFile(ff.getPath(), docs.getPath()+"/LightningLauncher.xml");
    }


    //TODO: This doesn't work
    public synchronized static void load(LauncherActivity activity) {
        File ff = new File(activity.getFilesDir().getParent()
                + "/shared_prefs/" +
                activity.getPackageName() + "_preferences.xml");
        File docs = new ContextWrapper(activity).getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (docs == null) return;
        DataLib.delete(ff.getParent());
        activity.launcherService.finishAllActivities();

        //noinspection ResultOfMethodCallIgnored
        ff.mkdirs();
        copyFile(docs.getPath()+"/LightningLauncher.xml", ff.getPath());

    }
    public static boolean canLoad(LauncherActivity activity) {
        ContextWrapper cw = new ContextWrapper(activity);
        File docs = new File(Objects.requireNonNull(
                cw.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).getPath()+"/LightningLauncher.xml");
        return docs.exists();
    }

    /** @noinspection IOStreamConstructor*/
    private static void copyFile(String inPath, String outPath) {
        try {
            File f1 = new File(inPath);
            File f2 = new File(outPath);
            InputStream in = new FileInputStream(f1);
            OutputStream out = new FileOutputStream(f2);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
