package com.threethan.launchercore.lib;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;


/** @noinspection unused*/
public class FileLib {

    public static void delete(String path) {
        delete(new File(path));
    }
    /** @noinspection UnusedReturnValue*/
    public static boolean delete(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                delete(child);

        return fileOrDirectory.delete();
    }

    public static boolean copy(File src, File dst) {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return true;
        } catch (IOException e) {
            Log.w("FileLib", "Failed to copy file", e);
            return false;
        }
    }
}
