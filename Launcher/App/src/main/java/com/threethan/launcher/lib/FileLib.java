package com.threethan.launcher.lib;

import java.io.File;
import java.util.Objects;


// Contains functions which are not application-specific
public class FileLib {

    public static void delete(String path) {
        delete(new File(path));
    }
    public static void delete(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                delete(child);

        final boolean ignored = fileOrDirectory.delete();
    }
}
