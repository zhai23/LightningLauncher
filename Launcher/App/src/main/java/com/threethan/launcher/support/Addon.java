package com.threethan.launcher.support;

import android.os.Build;

/*
    Addon

    Stores basic information about an addon
 */
public class Addon {
    public String downloadName;
    public String packageName;
    public String tag;
    public String overrideUrl;
    public String latestVersion;
    public Boolean accessibilityService;
    public boolean match(String name) {
        return tag.equals(name) || downloadName.equals(name) || packageName.equals(name);
    }
    public Addon(String tag, String downloadName, String packageName, String latestVersion,
                 Boolean accessibilityService) {
        this.downloadName = downloadName;
        this.packageName = packageName;
        this.latestVersion = latestVersion;
        this.tag = tag;
        this.accessibilityService = accessibilityService;
        this.overrideUrl = null;
    }

    public Addon(String tag, String downloadName, String packageName, String latestVersion,
                 Boolean accessibilityService, String overrideUrl) {
        this.downloadName = downloadName;
        this.packageName = packageName;
        this.latestVersion = latestVersion;
        this.tag = tag;
        this.accessibilityService = accessibilityService;
        this.overrideUrl = overrideUrl;
    }

    public Addon(String tag, String downloadName, String packageName, String latestVersion,
                 Boolean accessibilityService, String overrideUrl, boolean hasSeparateArm64) {
        if (hasSeparateArm64) {
            final boolean is64bit = Build.SUPPORTED_64_BIT_ABIS.length > 0;
            this.downloadName = is64bit ? downloadName + "_Arm64" : downloadName;
        }
        else this.downloadName = downloadName;
        this.packageName = packageName;
        this.latestVersion = latestVersion;
        this.tag = tag;
        this.accessibilityService = accessibilityService;
        this.overrideUrl = overrideUrl;
    }
}
