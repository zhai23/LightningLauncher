package com.threethan.launcher.helper;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.FileLib;
import com.threethan.launcher.adapter.GroupsAdapter;
import com.threethan.launcher.support.SettingsManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
public abstract class Compat {
    public static final String KEY_COMPATIBILITY_VERSION = "KEY_COMPATIBILITY_VERSION";
    public static final int CURRENT_COMPATIBILITY_VERSION = 2;
    public static final boolean DEBUG_COMPATIBILITY = false;
    public static synchronized void checkCompatibilityUpdate(LauncherActivity launcherActivity) {
        if (DEBUG_COMPATIBILITY) Log.e("COMPATIBILITY", "CRITICAL WARNING: DEBUG_COMPATIBILITY IS ON");
        SharedPreferences sharedPreferences = launcherActivity.sharedPreferences;
        SharedPreferences.Editor sharedPreferenceEditor = launcherActivity.sharedPreferenceEditor;
        int storedVersion = DEBUG_COMPATIBILITY ? 0 : sharedPreferences.getInt(Compat.KEY_COMPATIBILITY_VERSION, -1);
        if (storedVersion == -1) {
            if (sharedPreferences.getInt(Settings.KEY_BACKGROUND, -1) == -1) return; // return if fresh install
            storedVersion = 0; // set version to 0 if coming from a version before this system was added
        }

        if (storedVersion == Compat.CURRENT_COMPATIBILITY_VERSION) return; //Return if no update

        try {
            if (storedVersion > Compat.CURRENT_COMPATIBILITY_VERSION)
                Log.e("CompatibilityUpdate Error", "Previous version greater than current!");
            // If updated
            for (int version = 0; version <= Compat.CURRENT_COMPATIBILITY_VERSION; version++) {
                if (SettingsManager.getVersionsWithBackgroundChanges().contains(version)) {
                    int backgroundIndex = sharedPreferences.getInt(Settings.KEY_BACKGROUND, Settings.DEFAULT_BACKGROUND);

                    if (backgroundIndex >= 0 && backgroundIndex < SettingsManager.BACKGROUND_DARK.length)
                        sharedPreferenceEditor.putBoolean(Settings.KEY_DARK_MODE, SettingsManager.BACKGROUND_DARK[backgroundIndex]);
                    else if (storedVersion == 0)
                        sharedPreferenceEditor.putBoolean(Settings.KEY_DARK_MODE, Settings.DEFAULT_DARK_MODE);
                }
                if (version == 0) {
                    SettingsManager settingsManager = launcherActivity.settingsManager;
                    if (sharedPreferences.getInt(Settings.KEY_BACKGROUND, Settings.DEFAULT_BACKGROUND) == 6)
                        sharedPreferenceEditor.putInt(Settings.KEY_BACKGROUND, -1);

                    final Map<String, String> apps = SettingsManager.getAppGroupMap();
                    final Set<String> appGroupsList = settingsManager.getAppGroups();
                    final String oldGroupName = "Tools";
                    final String newGroupName = "Apps";
                    appGroupsList.remove(oldGroupName);
                    appGroupsList.add(newGroupName);
                    Map<String, String> updatedAppList = new HashMap<>();

                    for (String packageName : apps.keySet())
                        if (Objects.requireNonNull(apps.get(packageName)).compareTo(oldGroupName) == 0)
                            updatedAppList.put(packageName, newGroupName);
                        else
                            updatedAppList.put(packageName, apps.get(packageName));

                    HashSet<String> selectedGroups = new HashSet<>();
                    selectedGroups.add(newGroupName);
                    settingsManager.setSelectedGroups(selectedGroups);
                    settingsManager.setAppGroups(appGroupsList);
                    SettingsManager.setAppGroupMap(updatedAppList);
                }
                if (version == 1) {
                    int bg = sharedPreferences.getInt(Settings.KEY_BACKGROUND, Settings.DEFAULT_BACKGROUND);
                    if (bg > 2) sharedPreferenceEditor.putInt(Settings.KEY_BACKGROUND, bg+1);
                    recheckSupported(launcherActivity);
                }
            }
            Log.i("Settings Updated", String.format("Updated from v%s to v%s (Settings versions are not the same as app versions)",
                    storedVersion, Compat.CURRENT_COMPATIBILITY_VERSION));
        } catch (Exception e) {
            // This *shouldn't* fail, but if it does we should not crash
            Log.e("CompatibilityUpdate Error", "An exception occurred when attempting to perform the compatibility update!");
            e.printStackTrace();
        }

        Compat.clearIconCache(launcherActivity);
        // Store the updated version
        sharedPreferenceEditor.putInt(Compat.KEY_COMPATIBILITY_VERSION, Compat.CURRENT_COMPATIBILITY_VERSION);
    }

    public static void recheckSupported(LauncherActivity launcherActivity) {
        final Map<String, String> appGroupMap = SettingsManager.getAppGroupMap();
        List<ApplicationInfo> apps = launcherActivity.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo app: apps) {
            final boolean supported = App.isSupported(app, launcherActivity) || App.isWebsite(app);
            if(!supported) appGroupMap.put(app.packageName, GroupsAdapter.UNSUPPORTED_GROUP);
            if(supported && Objects.equals(appGroupMap.get(app.packageName), GroupsAdapter.HIDDEN_GROUP)) appGroupMap.remove(app.packageName);
        }
        SettingsManager.setAppGroupMap(appGroupMap);
    }

    public static void clearIcons(LauncherActivity launcherActivity) {
        FileLib.delete(launcherActivity.getApplicationInfo().dataDir);
        clearIconCache(launcherActivity);
    }
    public static void clearIconCache(LauncherActivity launcherActivity) {
        IconRepo.downloadFinishedPackages.clear();

        Icon.cachedIcons.clear();
        launcherActivity.sharedPreferenceEditor.remove(SettingsManager.DONT_DOWNLOAD_ICONS);
        storeAndReload(launcherActivity);
    }

    public static void clearLabels(LauncherActivity launcherActivity) {
        SettingsManager.appLabelCache.clear();
        HashSet<String> setAll = Platform.getAllPackages(launcherActivity);
        SharedPreferences.Editor editor = launcherActivity.sharedPreferenceEditor;
        for (String packageName : setAll) editor.remove(packageName);
        storeAndReload(launcherActivity);
    }
    public static void clearSort(LauncherActivity launcherActivity) {
        SettingsManager.getAppGroupMap().clear();
        Set<String> appGroupsSet = launcherActivity.sharedPreferences.getStringSet(Settings.KEY_GROUPS, null);
        if (appGroupsSet == null) return;
        SharedPreferences.Editor editor = launcherActivity.sharedPreferenceEditor;
        for (String groupName : appGroupsSet) editor.remove(Settings.KEY_GROUP_APP_LIST+groupName);
        storeAndReload(launcherActivity);
    }

    private static void storeAndReload(LauncherActivity launcherActivity) {
        launcherActivity.sharedPreferenceEditor.apply();
        Compat.recheckSupported(launcherActivity);
        SettingsManager.storeValues();
        launcherActivity.reloadPackages();
        launcherActivity.refresh();
        SettingsManager.readValues();
    }
}
