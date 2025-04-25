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

    /** */ // Fix requires higher API
    public static boolean copy(File fIn, File fOut) {
        try {
            InputStream in = new FileInputStream(fIn);
            //noinspection ResultOfMethodCallIgnored
            Objects.requireNonNull(fOut.getParentFile()).mkdirs();
            OutputStream out = new FileOutputStream(fOut);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
            return true;
        } catch (IOException e) {
            Log.w("Error when copying file", e);
            return false;
        }
    }
}
