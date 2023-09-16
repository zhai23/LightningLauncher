package com.threethan.launcher.support;

public class Addon {
    public String downloadName;
    public String packageName;
    public String tag;
    public String latestVersion;
    public Boolean accessibilityService;
    public boolean match(String name) {
        return tag.equals(name) || downloadName.equals(name) || packageName.equals(name);
    }
    public Addon(String tag, String downloadName, String packageName, String latestVersion, Boolean accessibilityService) {
        this.downloadName = downloadName;
        this.packageName = packageName;
        this.latestVersion = latestVersion;
        this.tag = tag;
        this.accessibilityService = accessibilityService;
    }
}
