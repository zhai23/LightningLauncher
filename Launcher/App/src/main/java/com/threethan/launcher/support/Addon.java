package com.threethan.launcher.support;

import android.content.pm.PackageManager;

public class Addon {
    public String downloadName;
    public String packageName;
    public String tag;
    public String latestVersion;
    public boolean match(String name) {
        return tag.equals(name) || downloadName.equals(name) || packageName.equals(name);
    }
    public Addon(String tag, String downloadName, String packageName, String latestVersion) {
        this.downloadName = downloadName;
        this.packageName = packageName;
        this.latestVersion = latestVersion;
        this.tag = tag;
    }
}
