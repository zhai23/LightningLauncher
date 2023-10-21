package com.threethan.launcher.helper;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.FileLib;
import com.threethan.launcher.lib.StringLib;
import com.threethan.launcher.support.SettingsManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/*
    Compat

    checkCompatibilityUpdate is called by the launcher when it's started, and attempts to update
    settings and other stored values to be compatible from previous versions

    It also provides helper functions for resetting certain types of data
 */

public abstract class Compat {
    public static final String KEY_COMPATIBILITY_VERSION = "KEY_COMPATIBILITY_VERSION";
    public static final int CURRENT_COMPATIBILITY_VERSION = 6;
    public static final boolean DEBUG_COMPATIBILITY = false;
    private static final String TAG = "Compatibility";

    public static synchronized void checkCompatibilityUpdate(LauncherActivity launcherActivity) {
        if (DEBUG_COMPATIBILITY) Log.e(TAG, "CRITICAL WARNING: DEBUG_COMPATIBILITY IS ON");
        SharedPreferences sharedPreferences = launcherActivity.sharedPreferences;
        SharedPreferences.Editor sharedPreferenceEditor = launcherActivity.sharedPreferences.edit();
        int storedVersion = DEBUG_COMPATIBILITY ? 0 : sharedPreferences.getInt(Compat.KEY_COMPATIBILITY_VERSION, -1);
        if (storedVersion == -1) {
            if (sharedPreferences.getInt(Settings.KEY_BACKGROUND, -1) == -1) return; // return if fresh install
            storedVersion = 0; // set version to 0 if coming from a version before this system was added
        }

        if (storedVersion == Compat.CURRENT_COMPATIBILITY_VERSION) return; //Return if no update

        try {
            if (storedVersion > Compat.CURRENT_COMPATIBILITY_VERSION)
                Log.e(TAG, "Previous version greater than current!");
            // If updated
            for (int version = 0; version <= Compat.CURRENT_COMPATIBILITY_VERSION; version++) {
                if (SettingsManager.getVersionsWithBackgroundChanges().contains(version)) {
                    int backgroundIndex = sharedPreferences.getInt(Settings.KEY_BACKGROUND,
                            Platform.isTv(launcherActivity)
                                    ? Settings.DEFAULT_BACKGROUND_TV
                                    : Settings.DEFAULT_BACKGROUND_VR);

                    if (backgroundIndex >= 0 && backgroundIndex < SettingsManager.BACKGROUND_DARK.length)
                        sharedPreferenceEditor.putBoolean(Settings.KEY_DARK_MODE, SettingsManager.BACKGROUND_DARK[backgroundIndex]);
                    else if (storedVersion == 0)
                        sharedPreferenceEditor.putBoolean(Settings.KEY_DARK_MODE, Settings.DEFAULT_DARK_MODE);
                }
                switch (version) {
                    case (0):
                        if (sharedPreferences.getInt(Settings.KEY_BACKGROUND,
                                Platform.isTv(launcherActivity)
                                        ? Settings.DEFAULT_BACKGROUND_TV
                                        : Settings.DEFAULT_BACKGROUND_VR) == 6)
                            sharedPreferenceEditor.putInt(Settings.KEY_BACKGROUND, -1);
                        // Rename group to new default
                        renameGroup(launcherActivity, "Tools", "Apps");
                        break;
                    case (1):
                        int bg = sharedPreferences.getInt(Settings.KEY_BACKGROUND,
                                Platform.isTv(launcherActivity)
                                        ? Settings.DEFAULT_BACKGROUND_TV
                                        : Settings.DEFAULT_BACKGROUND_VR);
                        if (bg > 2) sharedPreferenceEditor.putInt(Settings.KEY_BACKGROUND, bg + 1);
                        recheckSupported(launcherActivity);
                        break;
                    case (2):
                        String from = sharedPreferences.getString("KEY_DEFAULT_GROUP_VR", Settings.FALLBACK_GROUPS.get(App.Type.TYPE_VR));
                        String to = StringLib.setStarred(from, true);
                        renameGroup(launcherActivity, from, to);
                        break;
                    case (3): // Should just clear icon cache, which is called anyways
                        break;
                    case (4):
                        // App launch out conversion, may not work well but isn't really important
                        final String KEY_OLD_LAUNCH_OUT = "prefLaunchOutList";
                        final Set<String> launchOutSet = sharedPreferences.getStringSet(KEY_OLD_LAUNCH_OUT, Collections.emptySet());
                        for (String app : launchOutSet) sharedPreferenceEditor.putBoolean(Settings.KEY_LAUNCH_OUT_PREFIX + app, true);
                        // Wallpaper remap
                        int backgroundIndex = sharedPreferences.getInt(Settings.KEY_BACKGROUND,
                                Platform.isTv(launcherActivity)
                                        ? Settings.DEFAULT_BACKGROUND_TV
                                        : Settings.DEFAULT_BACKGROUND_VR);
                        if (backgroundIndex > 2)
                            sharedPreferenceEditor.putInt(Settings.KEY_BACKGROUND, backgroundIndex - 1);
                    case (6):
                        // Remap old default group settings
                        final Map<String, App.Type> oldDefKeyToType = new HashMap<>();
                        oldDefKeyToType.put("KEY_DEFAULT_GROUP_2D", App.Type.TYPE_PHONE);
                        oldDefKeyToType.put("KEY_DEFAULT_GROUP_VR", App.Type.TYPE_VR);
                        oldDefKeyToType.put("KEY_DEFAULT_GROUP_TV", App.Type.TYPE_TV);
                        oldDefKeyToType.put("KEY_DEFAULT_GROUP_WEB", App.Type.TYPE_WEB);

                        for (String key : oldDefKeyToType.keySet()) {
                            String val = sharedPreferences.getString(key, null);
                            if (val != null) {
                                sharedPreferenceEditor.putString(Settings.KEY_DEFAULT_GROUP + oldDefKeyToType.get(key), val);
                                sharedPreferenceEditor.remove(key);
                            }
                        }

                        final Map<String, App.Type> oldWideKeyToType = new HashMap<>();
                        oldWideKeyToType.put("KEY_WIDE_2D", App.Type.TYPE_PHONE);
                        oldWideKeyToType.put("KEY_WIDE_VR", App.Type.TYPE_VR);
                        oldWideKeyToType.put("KEY_WIDE_TV", App.Type.TYPE_TV);
                        oldWideKeyToType.put("KEY_WIDE_WEB", App.Type.TYPE_WEB);

                        for (String key : oldWideKeyToType.keySet()) {
                            if (sharedPreferences.contains(key)) {
                                boolean val = sharedPreferences.getBoolean(key, false);
                                sharedPreferenceEditor.putBoolean(Settings.KEY_BANNER + oldDefKeyToType.get(key), val);
                                sharedPreferenceEditor.remove(key);
                            }
                        }
                        recheckSupported(launcherActivity);
                        break;

                }
            }
            Log.i(TAG, String.format("Settings Updated from v%s to v%s (Settings versions are not the same as app versions)",
                    storedVersion, Compat.CURRENT_COMPATIBILITY_VERSION));
        } catch (Exception e) {
            // This *shouldn't* fail, but if it does we should not crash
            Log.e(TAG, "An exception occurred when attempting to perform the compatibility update!");
            e.printStackTrace();
        }

        Compat.clearIconCache(launcherActivity);
        // Store the updated version
        sharedPreferenceEditor.putInt(Compat.KEY_COMPATIBILITY_VERSION, Compat.CURRENT_COMPATIBILITY_VERSION);
        sharedPreferenceEditor.apply();
    }

    public static void renameGroup(LauncherActivity launcherActivity, String from, String to) {
        SettingsManager settingsManager = launcherActivity.settingsManager;

        final Map<String, String> apps = SettingsManager.getAppGroupMap();
        final Set<String> appGroupsList = SettingsManager.getAppGroups();
        appGroupsList.remove(from);
        appGroupsList.add(to);
        Map<String, String> updatedAppList = new HashMap<>();

        for (String packageName : apps.keySet())
            if (Objects.requireNonNull(apps.get(packageName)).compareTo(from) == 0)
                updatedAppList.put(packageName, to);
            else
                updatedAppList.put(packageName, apps.get(packageName));

        HashSet<String> selectedGroups = new HashSet<>();
        selectedGroups.add(to);
        settingsManager.setSelectedGroups(selectedGroups);
        settingsManager.setAppGroups(appGroupsList);
        SettingsManager.setAppGroupMap(updatedAppList);
    }
    public static void recheckSupported(LauncherActivity launcherActivity) {
        final Map<String, String> appGroupMap = SettingsManager.getAppGroupMap();
        List<ApplicationInfo> apps = launcherActivity.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        App.invalidateCaches(launcherActivity);
        for (ApplicationInfo app: apps) {
            final boolean supported = App.isSupported(app, launcherActivity);
//            Log.v("SUPPORTED: "+supported, app.packageName);
            if(!supported) appGroupMap.put(app.packageName, Settings.UNSUPPORTED_GROUP);
            else if (Objects.equals(appGroupMap.get(app.packageName), Settings.UNSUPPORTED_GROUP))
                appGroupMap.remove(app.packageName);
        }
        SettingsManager.setAppGroupMap(appGroupMap);
    }

    // Clears all icons, including custom icons
    public static void clearIcons(LauncherActivity launcherActivity) {
        Log.i(TAG, "Icons are being cleared");
        FileLib.delete(launcherActivity.getApplicationInfo().dataDir);
        launcherActivity.sharedPreferenceEditor.remove(SettingsManager.DONT_DOWNLOAD_ICONS); 
        clearIconCache(launcherActivity);
    }
    // Clears all icons, except for custom icons, and sets them to be re-downloaded
    public static void clearIconCache(LauncherActivity launcherActivity) {
        Log.i(TAG, "Icon cache is being cleared");

        launcherActivity.isKillable = false;
        launcherActivity.launcherService.clearAdapterCachesAll();

        IconRepo.downloadFinishedPackages.clear();
        Icon.cachedIcons.clear();
        storeAndReload(launcherActivity);
    }
    // Clears any custom labels assigned to apps, including whether they've been starred
    public static void clearLabels(LauncherActivity launcherActivity) {
        Log.i(TAG, "Labels are being cleared");
        SettingsManager.appLabelCache.clear();
        HashSet<String> setAll = launcherActivity.getAllPackages();
        SharedPreferences.Editor editor = launcherActivity.sharedPreferenceEditor;
        for (String packageName : setAll) editor.remove(packageName);
        storeAndReload(launcherActivity);
        if (launcherActivity.launcherService != null)
            launcherActivity.launcherService.clearAdapterCachesAll();
    }
    // Clears the categorization of apps & resets everything to selected default groups
    public static void clearSort(LauncherActivity launcherActivity) {
        Log.i(TAG, "App sort is being cleared");
        SettingsManager.getAppGroupMap().clear();
        Set<String> appGroupsSet = launcherActivity.sharedPreferences.getStringSet(Settings.KEY_GROUPS, null);
        if (appGroupsSet == null) return;
        SharedPreferences.Editor editor = launcherActivity.sharedPreferenceEditor;
        for (String groupName : appGroupsSet) editor.remove(Settings.KEY_GROUP_APP_LIST+groupName);
        storeAndReload(launcherActivity);
        launcherActivity.refreshAppDisplayListsAll();

    }
    // Resets the group list to default, including default groups for sorting
    public static void resetDefaultGroups(LauncherActivity launcherActivity) {
        for (App.Type type : Platform.getSupportedAppTypes(launcherActivity))
            launcherActivity.sharedPreferenceEditor.remove(Settings.KEY_DEFAULT_GROUP + type);

        launcherActivity.settingsManager.resetGroups();
        clearSort(launcherActivity);

        launcherActivity.refreshInterfaceAll();
    }
    // Stores any settings which may have been changed then refreshes any extant launcher activities
    private static void storeAndReload(LauncherActivity launcherActivity) {
        launcherActivity.sharedPreferenceEditor.apply();
        Compat.recheckSupported(launcherActivity);
        SettingsManager.writeValues();
        launcherActivity.reloadPackages();
        launcherActivity.refreshInterfaceAll();
        SettingsManager.readValues();
    }
}
