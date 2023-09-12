package com.threethan.launcher.helper;

import android.content.SharedPreferences;

import com.threethan.launcher.launcher.LauncherActivity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class Platform {
    public static void clearPackageLists() {
        App.set2d.clear();
        App.setVr.clear();
    }
    public static HashSet<String> getAllPackages(LauncherActivity launcherActivity) {
        final SharedPreferences sharedPreferences = launcherActivity.sharedPreferences;
        if (App.setVr.isEmpty()) {
            App.setVr = sharedPreferences.getStringSet(Settings.KEY_VR_SET, App.setVr);
            App.set2d = sharedPreferences.getStringSet(Settings.KEY_2D_SET, App.set2d);
        }
        HashSet<String> setAll = new HashSet<>(App.set2d);
        setAll.addAll(App.setVr);
        return setAll;
    }
    public static void addWebsite(SharedPreferences sharedPreferences, String url, String group) {
        if (!url.contains("//")) url = "https://" + url;

        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        webApps = new HashSet<>(webApps); // Copy since we're not supposed to modify directly
        webApps.add(url);

        Set<String> groupApps = sharedPreferences.getStringSet(Settings.KEY_GROUP_APP_LIST+group, Collections.emptySet());
        groupApps = new HashSet<>(groupApps); // Copy since we're not supposed to modify directly
        groupApps.add(url);

        sharedPreferences.edit()
                .putStringSet(Settings.KEY_WEBSITE_LIST, webApps)
                .putStringSet(Settings.KEY_GROUP_APP_LIST+group, groupApps)
                .apply();

    }
}