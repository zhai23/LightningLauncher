package com.threethan.launcher.helper;

public abstract class Debug {
    // This function is useful to see where something was called from
    // (it probably shouldn't be referenced in shipped builds)
    public static void printStackTrace() {
        try {
            throw new RuntimeException("Debug Exception");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
