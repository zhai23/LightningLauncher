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
    public static String toTitleCase(String string) {
        if (string == null) return null;
        boolean whiteSpace = true;
        StringBuilder builder = new StringBuilder(string);
        for (int i = 0; i < builder.length(); ++i) {
            char c = builder.charAt(i);
            if (whiteSpace) {
                if (!Character.isWhitespace(c)) {
                    builder.setCharAt(i, Character.toTitleCase(c));
                    whiteSpace = false;
                }
            } else if (Character.isWhitespace(c)) whiteSpace = true;
            else builder.setCharAt(i, Character.toLowerCase(c));
        }
        return builder.toString();
    }
}
