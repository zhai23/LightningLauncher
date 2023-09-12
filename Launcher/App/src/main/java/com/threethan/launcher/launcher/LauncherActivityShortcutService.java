package com.threethan.launcher.launcher;

// Used by the service to not override the default
public class LauncherActivityShortcutService extends LauncherActivityEditable {
    @Override
    public String getId() { return "shortcut service"; }
}
