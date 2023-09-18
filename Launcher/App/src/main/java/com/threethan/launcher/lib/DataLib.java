package com.threethan.launcher.lib;

import java.io.File;
import java.util.Map;
import java.util.Objects;


// Contains functions which are not application-specific
public class DataLib {

    public static void delete(String path) {
        delete(new File(path));
    }
    public static void delete(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                delete(child);

        final boolean ignored = fileOrDirectory.delete();
    }
    public static <T, E> T keyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet())
            if (Objects.equals(value, entry.getValue()))
                return entry.getKey();
        return null;
    }
}
