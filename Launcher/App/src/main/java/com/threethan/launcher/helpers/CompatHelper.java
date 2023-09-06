package com.threethan.launcher.helpers;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.threethan.launcher.MainActivity;
import com.threethan.launcher.platforms.AbstractPlatform;
import com.threethan.launcher.ui.GroupsAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
public class CompatHelper {
    public static final String KEY_COMPATIBILITY_VERSION = "KEY_COMPATIBILITY_VERSION";
    public static final int CURRENT_COMPATIBILITY_VERSION = 2;
    public static final boolean DEBUG_COMPATIBILITY = false;
    public static synchronized void checkCompatibilityUpdate(MainActivity mainActivity) {
        Log.w("COMPATIBILITY", "DEBUG_COMPATIBILITY IS ON");
        SharedPreferences sharedPreferences = mainActivity.sharedPreferences;
        SharedPreferences.Editor sharedPreferenceEditor = mainActivity.sharedPreferenceEditor;
        int storedVersion = DEBUG_COMPATIBILITY ? 0 : sharedPreferences.getInt(CompatHelper.KEY_COMPATIBILITY_VERSION, -1);
        if (storedVersion == -1) {
            if (sharedPreferences.getInt(SettingsManager.KEY_BACKGROUND, -1) == -1) return; // return if fresh install
            storedVersion = 0; // set version to 0 if coming from a version before this system was added
        }

        if (storedVersion == CompatHelper.CURRENT_COMPATIBILITY_VERSION) return; //Return if no update

        try {
            if (storedVersion > CompatHelper.CURRENT_COMPATIBILITY_VERSION)
                Log.e("CompatibilityUpdate Error", "Previous version greater than current!");
            // If updated
            for (int version = 0; version <= CompatHelper.CURRENT_COMPATIBILITY_VERSION; version++) {
                if (SettingsManager.getVersionsWithBackgroundChanges().contains(version)) {
                    int backgroundIndex = sharedPreferences.getInt(SettingsManager.KEY_BACKGROUND, SettingsManager.DEFAULT_BACKGROUND);
                    if (backgroundIndex >= 0 && backgroundIndex < SettingsManager.BACKGROUND_DARK.length) {
                        sharedPreferenceEditor.putBoolean(SettingsManager.KEY_DARK_MODE, SettingsManager.BACKGROUND_DARK[backgroundIndex]);
                    } else if (storedVersion == 0) {
                        sharedPreferenceEditor.putBoolean(SettingsManager.KEY_DARK_MODE, SettingsManager.DEFAULT_DARK_MODE);
                    }
                    // updates may reference the specific version in the future
                }
                if (version == 0) {
                    SettingsManager settingsManager = mainActivity.settingsManager;
                    if (sharedPreferences.getInt(SettingsManager.KEY_BACKGROUND, SettingsManager.DEFAULT_BACKGROUND) == 6) {
                        sharedPreferenceEditor.putInt(SettingsManager.KEY_BACKGROUND, -1);
                    }
                    final Map<String, String> apps = SettingsManager.getAppGroupMap();
                    final Set<String> appGroupsList = settingsManager.getAppGroups();
                    final String oldGroupName = "Tools";
                    final String newGroupName = "Apps";
                    appGroupsList.remove(oldGroupName);
                    appGroupsList.add(newGroupName);
                    Map<String, String> updatedAppList = new HashMap<>();
                    for (String packageName : apps.keySet()) {
                        if (Objects.requireNonNull(apps.get(packageName)).compareTo(oldGroupName) == 0) {
                            updatedAppList.put(packageName, newGroupName);
                        } else {
                            updatedAppList.put(packageName, apps.get(packageName));
                        }
                    }
                    HashSet<String> selectedGroups = new HashSet<>();
                    selectedGroups.add(newGroupName);
                    settingsManager.setSelectedGroups(selectedGroups);
                    settingsManager.setAppGroups(appGroupsList);
                    SettingsManager.setAppGroupMap(updatedAppList);
                }
                if (version == 1) {
                    int bg = sharedPreferences.getInt(SettingsManager.KEY_BACKGROUND, SettingsManager.DEFAULT_BACKGROUND);
                    if (bg > 2) sharedPreferenceEditor.putInt(SettingsManager.KEY_BACKGROUND, bg+1);
                    recheckSupported(mainActivity);
                }
            }
            Log.i("Settings Updated", String.format("Updated from v%s to v%s (Settings versions are not the same as app versions)",
                    storedVersion, CompatHelper.CURRENT_COMPATIBILITY_VERSION));
        } catch (Exception e) {
            // This *shouldn't* fail, but if it does we should not crash
            Log.e("CompatibilityUpdate Error", "An exception occurred when attempting to perform the compatibility update!");
            e.printStackTrace();
        }

        CompatHelper.clearIconCache(mainActivity);
        // Store the updated version
        sharedPreferenceEditor
                .putInt(CompatHelper.KEY_COMPATIBILITY_VERSION, CompatHelper.CURRENT_COMPATIBILITY_VERSION)
                .putBoolean(SettingsManager.NEEDS_META_DATA, true)
                ;
    }

    public static void recheckSupported(MainActivity mainActivity) {
        final Map<String, String> appGroupMap = SettingsManager.getAppGroupMap();
        List<ApplicationInfo> apps = mainActivity.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo app: apps) {
            final boolean supported = AbstractPlatform.isSupportedApp(app, mainActivity) || AbstractPlatform.isWebsite(app);
            if(!supported) appGroupMap.put(app.packageName, GroupsAdapter.UNSUPPORTED_GROUP);
            if(supported && Objects.equals(appGroupMap.get(app.packageName), GroupsAdapter.HIDDEN_GROUP)) appGroupMap.remove(app.packageName);
        }
        SettingsManager.setAppGroupMap(appGroupMap);
    }

    public static void clearIcons(MainActivity mainActivity) {
        LibHelper.delete(mainActivity.getApplicationInfo().dataDir);
        clearIconCache(mainActivity);
    }
    public static void clearIconCache(MainActivity mainActivity) {
        AbstractPlatform.excludedIconPackages.clear();
        AbstractPlatform.cachedIcons.clear();
        AbstractPlatform.dontDownloadIconPackages.clear();
        mainActivity.sharedPreferenceEditor
                .remove(SettingsManager.NEEDS_META_DATA)
                .remove(SettingsManager.DONT_DOWNLOAD_ICONS)
                ;
        storeAndReload(mainActivity);
    }

    public static void clearLabels(MainActivity mainActivity) {
        SettingsManager.appLabelCache.clear();
        HashSet<String> setAll = AbstractPlatform.getAllPackages(mainActivity);
        SharedPreferences.Editor editor = mainActivity.sharedPreferenceEditor;
        for (String packageName : setAll) editor.remove(packageName);
        editor.putBoolean(SettingsManager.NEEDS_META_DATA, true);
        storeAndReload(mainActivity);
    }
    public static void clearSort(MainActivity mainActivity) {
        SettingsManager.getAppGroupMap().clear();
        Set<String> appGroupsSet = mainActivity.sharedPreferences.getStringSet(SettingsManager.KEY_APP_GROUPS, null);
        if (appGroupsSet == null) return;
        SharedPreferences.Editor editor = mainActivity.sharedPreferenceEditor;
        for (String groupName : appGroupsSet) editor.remove(SettingsManager.KEY_GROUP_APP_LIST +groupName);
        editor.putBoolean(SettingsManager.NEEDS_META_DATA, true);
        storeAndReload(mainActivity);
    }

    private static void storeAndReload(MainActivity mainActivity) {
        mainActivity.sharedPreferenceEditor.apply();
        CompatHelper.recheckSupported(mainActivity);
        SettingsManager.storeValues();
        mainActivity.reloadPackages();
        mainActivity.refreshInterface();
        SettingsManager.readValues();
    }


}
