package com.threethan.launcher.helper;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import com.threethan.launcher.lib.StringLib;
import com.threethan.launcher.support.SettingsManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Platform {
    public static List<ApplicationInfo> installedApps;
    public static List<ApplicationInfo> appListBanner;
    public static List<ApplicationInfo> appListSquare;
    public static void clearPackageLists() {
        App.set2d.clear();
        App.setVr.clear();
    }
    public static String findWebsite(SharedPreferences sharedPreferences, String url) {
        url = StringLib.fixUrl(url);

        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        if (!webApps.contains(url)) return null;
        else return SettingsManager.getAppGroupMap().get(url);
    }
    public static void addWebsite(SharedPreferences sharedPreferences, String url) {
        url = StringLib.fixUrl(url);

        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        webApps = new HashSet<>(webApps); // Copy since we're not supposed to modify directly
        webApps.add(url);

        sharedPreferences.edit()
                .putStringSet(Settings.KEY_WEBSITE_LIST, webApps)
                .apply();
    }
}