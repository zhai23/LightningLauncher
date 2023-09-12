package com.threethan.launcher.launcher;

// Used by the service to not override the default
public class LauncherActivityDefault extends LauncherActivityEditable {
    @Override
    public String getId() { return "default"; }
}
