package com.threethan.launcher.helper;

import static com.threethan.launcher.activity.support.SettingsManager.META_LABEL_SUFFIX;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.threethan.launcher.R;
import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launcher.activity.adapter.LauncherAppsAdapter;
import com.threethan.launcher.activity.dialog.BasicDialog;
import com.threethan.launcher.activity.support.DataStoreEditor;
import com.threethan.launcher.activity.support.SettingsManager;
import com.threethan.launcher.data.Settings;
import com.threethan.launchercore.Core;
import com.threethan.launchercore.lib.DelayLib;
import com.threethan.launchercore.lib.FileLib;
import com.threethan.launchercore.lib.StringLib;
import com.threethan.launchercore.metadata.IconLoader;
import com.threethan.launchercore.metadata.IconUpdater;
import com.threethan.launchercore.util.App;
import com.threethan.launchercore.util.Platform;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * checkCompatibilityUpdate is called by the launcher when it's started, and attempts to update
 * settings and other stored values to be compatible from previous versions.
 * <p>
 * This includes calling the function to migrate from sharedPreferences.
 * <p>
 * It also provides helper functions for resetting certain types of data
 */
public abstract class Compat {
    public static final String KEY_COMPATIBILITY_VERSION = "KEY_COMPATIBILITY_VERSION";
    public static final int CURRENT_COMPATIBILITY_VERSION = 10;
    public static final boolean DEBUG_COMPATIBILITY = false;
    private static final String TAG = "Compatibility";

    public static synchronized void checkCompatibilityUpdate(LauncherActivity launcherActivity) {
        Core.init(launcherActivity);
        if (DEBUG_COMPATIBILITY) Log.e(TAG, "CRITICAL WARNING: DEBUG_COMPATIBILITY IS ON");
        DataStoreEditor dataStoreEditor = launcherActivity.dataStoreEditor;

        int storedVersion = DEBUG_COMPATIBILITY ? 0 : dataStoreEditor.getInt(Compat.KEY_COMPATIBILITY_VERSION, -1);

        if (storedVersion == -1) {
            // Attempt migration
            DataStoreEditor dse1 = new DataStoreEditor(launcherActivity
                    .getApplicationContext());
            dse1.asyncWrite = false;
            dse1.migrateDefault(launcherActivity);
            DataStoreEditor dse2 = new DataStoreEditor(
                    launcherActivity.getApplicationContext(), "sort");
            dse2.asyncWrite = false;
            dse2.migrateDefault(launcherActivity);
            if (dataStoreEditor.getInt(Settings.KEY_BACKGROUND, -1) != -1)
                BasicDialog.toast(launcherActivity.getString(R.string.migrated));
            clearIconCache(launcherActivity);
        }

        // Update stored version in case it was migrated
        if (storedVersion == -1) {
            // Continue
            if (dataStoreEditor.getInt(Settings.KEY_BACKGROUND, -1) == -1) {
                // Store the updated version
                dataStoreEditor.putInt(Compat.KEY_COMPATIBILITY_VERSION, Compat.CURRENT_COMPATIBILITY_VERSION);
                return; // return if fresh install
            }
            storedVersion = 0; // set version to 0 if coming from a version before this system was added
        }

        if (storedVersion == Compat.CURRENT_COMPATIBILITY_VERSION) return; //Return if no update

        try {
            if (storedVersion > Compat.CURRENT_COMPATIBILITY_VERSION)
                Log.e(TAG, "Previous version greater than current!");
            // If updated
            for (int version = 0; version <= Compat.CURRENT_COMPATIBILITY_VERSION; version++) {
                if (SettingsManager.getVersionsWithBackgroundChanges().contains(version)) {
                    int backgroundIndex = dataStoreEditor.getInt(Settings.KEY_BACKGROUND,
                            Platform.isTv()
                                    ? Settings.DEFAULT_BACKGROUND_TV
                                    : Settings.DEFAULT_BACKGROUND_VR);

                    if (backgroundIndex >= 0 && backgroundIndex < SettingsManager.BACKGROUND_DARK.length)
                        dataStoreEditor.putBoolean(Settings.KEY_DARK_MODE, SettingsManager.BACKGROUND_DARK[backgroundIndex]);
                    else if (storedVersion == 0)
                        dataStoreEditor.putBoolean(Settings.KEY_DARK_MODE, Settings.DEFAULT_DARK_MODE);
                }

                switch (version) {
                    case (0):
                        if (dataStoreEditor.getInt(Settings.KEY_BACKGROUND,
                                Platform.isTv()
                                        ? Settings.DEFAULT_BACKGROUND_TV
                                        : Settings.DEFAULT_BACKGROUND_VR) == 6)
                            dataStoreEditor.putInt(Settings.KEY_BACKGROUND, -1);
                        // Rename group to new default
                        renameGroup(launcherActivity, "Tools", "Apps");
                        break;
                    case (1):
                        int bg = dataStoreEditor.getInt(Settings.KEY_BACKGROUND,
                                Platform.isTv()
                                        ? Settings.DEFAULT_BACKGROUND_TV
                                        : Settings.DEFAULT_BACKGROUND_VR);
                        if (bg > 2) dataStoreEditor.putInt(Settings.KEY_BACKGROUND, bg + 1);
                        break;
                    case (2):
                        String from = dataStoreEditor.getString("KEY_DEFAULT_GROUP_VR",
                                Settings.FALLBACK_GROUPS.get(App.Type.VR));
                        String to = StringLib.setStarred(from, true);
                        renameGroup(launcherActivity, from, to);
                        break;
                    case (3): // Should just clear icon cache, which is called anyways
                        break;
                    case (4):
                        // App launch out conversion, may not work well but isn't really important
                        // Wallpaper remap
                        int backgroundIndex = dataStoreEditor.getInt(Settings.KEY_BACKGROUND,
                                Platform.isTv()
                                        ? Settings.DEFAULT_BACKGROUND_TV
                                        : Settings.DEFAULT_BACKGROUND_VR);
                        if (backgroundIndex > 2)
                            dataStoreEditor.putInt(Settings.KEY_BACKGROUND, backgroundIndex - 1);
                    case (6):
                        // Remap old default group settings
                        final Map<String, App.Type> oldDefKeyToType = new HashMap<>();
                        oldDefKeyToType.put("KEY_DEFAULT_GROUP_2D", App.Type.PHONE);
                        oldDefKeyToType.put("KEY_DEFAULT_GROUP_VR", App.Type.VR);
                        oldDefKeyToType.put("KEY_DEFAULT_GROUP_TV", App.Type.TV);
                        oldDefKeyToType.put("KEY_DEFAULT_GROUP_WEB", App.Type.WEB);

                        for (String key : oldDefKeyToType.keySet()) {
                            String val = dataStoreEditor.getString(key, null);
                            if (val != null) {
                                dataStoreEditor.putString(Settings.KEY_DEFAULT_GROUP + oldDefKeyToType.get(key), val);
                                dataStoreEditor.removeBoolean(key);
                            }
                        }

                        final Map<String, App.Type> oldWideKeyToType = new HashMap<>();
                        oldWideKeyToType.put("KEY_WIDE_2D", App.Type.PHONE);
                        oldWideKeyToType.put("KEY_WIDE_VR", App.Type.VR);
                        oldWideKeyToType.put("KEY_WIDE_TV", App.Type.TV);
                        oldWideKeyToType.put("KEY_WIDE_WEB", App.Type.WEB);

                        for (String key : oldWideKeyToType.keySet()) {
                            if (dataStoreEditor.contains(key)) {
                                boolean val = dataStoreEditor.getBoolean(key, false);
                                dataStoreEditor.putBoolean(Settings.KEY_BANNER + oldWideKeyToType.get(key), val);
                                dataStoreEditor.removeBoolean(key);
                            }
                        }
                        break;
                    case (7):
                        clearIconCache(launcherActivity);
                    case (8):
                        // Clear old icon cache
                        for (File fromFile : Objects.requireNonNull(
                                new File (launcherActivity.getApplicationInfo().dataDir).listFiles()))
                            if (fromFile.getName().endsWith(".webp"))
                                //noinspection ResultOfMethodCallIgnored
                                fromFile.delete();
                    case (10):
                        clearIconCache(launcherActivity);
                        clearIcons(launcherActivity);
                }
            }
            Log.i(TAG, String.format("Settings Updated from v%s to v%s (Settings versions are not the same as app versions)",
                    storedVersion, Compat.CURRENT_COMPATIBILITY_VERSION));
        } catch (Exception e) {
            // This *shouldn't* fail, but if it does we should not crash
            Log.e(TAG, "An exception occurred when attempting to perform the compatibility update", e);
        }

        launcherActivity.needsUpdateCleanup = true;

        // Store the updated version
        dataStoreEditor.putInt(Compat.KEY_COMPATIBILITY_VERSION, Compat.CURRENT_COMPATIBILITY_VERSION);
    }
    public static void doUpdateCleanup(LauncherActivity launcherActivity) {
        clearIconCache(launcherActivity);
        launcherActivity.needsUpdateCleanup = false;
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

    // Clears all icons, including custom icons
    public static void clearIcons(LauncherActivity launcherActivity) {
        Log.i(TAG, "Icons are being cleared");
        FileLib.delete(launcherActivity.getApplicationInfo().dataDir + IconLoader.ICON_CUSTOM_FOLDER);
        launcherActivity.dataStoreEditor.removeStringSet(Settings.KEY_FORCED_BANNER);
        launcherActivity.dataStoreEditor.removeStringSet(Settings.KEY_FORCED_SQUARE);
        clearIconCache(launcherActivity);
    }
    // Clears all icons, except for custom icons, and sets them to be re-downloaded
    public static void clearIconCache(LauncherActivity launcherActivity) {
        Log.i(TAG, "Icon cache is being cleared");
        FileLib.delete(launcherActivity.getApplicationInfo().dataDir + IconLoader.ICON_CACHE_FOLDER);

        IconLoader.cachedIcons.clear();
        IconUpdater.nextCheckByPackageMs.clear();

        launcherActivity.launcherService.forEachActivity(a -> {
            if (a.getAppAdapter() != null) a.getAppAdapter().notifyAllChanged();
        });
    }
    // Clears any custom labels assigned to apps, including whether they've been starred
    public static void clearLabels(LauncherActivity launcherActivity) {
        Log.i(TAG, "Labels are being cleared");
        SettingsManager.appLabelCache.clear();
        for (String packageName : launcherActivity.getAllPackages()) {
            launcherActivity.dataStoreEditor.removeString(packageName);
            launcherActivity.dataStoreEditor.removeString(packageName+META_LABEL_SUFFIX);
        }

        launcherActivity.launcherService.forEachActivity(LauncherActivity::refreshAppList);

    }
    // Clears the categorization of apps & resets everything to selected default groups
    public static void clearSort(LauncherActivity launcherActivity) {
        Log.i(TAG, "App sort is being cleared");
        Set<String> appGroupsSet = launcherActivity.dataStoreEditor.getStringSet(Settings.KEY_GROUPS, new HashSet<>());
        for (String groupName : appGroupsSet)
            launcherActivity.dataStoreEditor.removeStringSet(Settings.KEY_GROUP_APP_LIST + groupName);
        SettingsManager.getAppGroupMap().clear();
        launcherActivity.settingsManager.resetGroupsAndSort();

        storeAndReload(launcherActivity);
        launcherActivity.launcherService.forEachActivity(LauncherActivity::forceRefreshPackages);
    }
    // Resets the group list to default, including default groups for sorting
    public static void resetDefaultGroups(LauncherActivity launcherActivity) {
        for (App.Type type : PlatformExt.getSupportedAppTypes())
            launcherActivity.dataStoreEditor.removeString(Settings.KEY_DEFAULT_GROUP + type);

        launcherActivity.settingsManager.resetGroupsAndSort();
        clearSort(launcherActivity);
        launcherActivity.launcherService.forEachActivity(LauncherActivity::resetAdapters);
    }
    // Stores any settings which may have been changed then refreshes any extent launcher activities
    private static void storeAndReload(LauncherActivity launcherActivity) {
        SettingsManager.setAppGroupMap(new ConcurrentHashMap<>());

        SettingsManager.writeGroupsAndSort();
        launcherActivity.launcherService.forEachActivity(a -> {
            a.refreshAppList();
            a.forceRefreshPackages();
        });
    }
    private static DataStoreEditor dataStoreEditor;
    public static DataStoreEditor getDataStore() {
        if (LauncherActivity.getForegroundInstance() != null
        && LauncherActivity.getForegroundInstance().dataStoreEditor != null) {
            dataStoreEditor = LauncherActivity.getForegroundInstance().dataStoreEditor;
        } else if (dataStoreEditor == null) {
            Log.w(TAG, "Failed to grab dataStoreEditor from instance, using fallback");
            dataStoreEditor = new DataStoreEditor(Core.context());
        }
        return dataStoreEditor;
    }

    public static void resetIcon(ApplicationInfo app, Consumer<Drawable> callback) {
        IconLoader.cachedIcons.remove(app.packageName);
        IconUpdater.nextCheckByPackageMs.remove(app.packageName);

        File cFile = IconLoader.iconCustomFileForApp(app);
        if (cFile.exists()) //noinspection ResultOfMethodCallIgnored
            cFile.delete();

        IconLoader.loadIcon(app, d -> {
            LauncherActivity foregroundInstance = LauncherActivity.getForegroundInstance();
            if (foregroundInstance != null && foregroundInstance.launcherService != null) {
                foregroundInstance.launcherService.forEachActivity(a -> {
                    LauncherAppsAdapter appAdapter = a.getAppAdapter();
                    if (appAdapter != null) a.runOnUiThread(() -> appAdapter.notifyItemChanged(app));
                });
            }
            callback.accept(d);
        });
    }

    public static void restartFully() {
        LauncherActivity foregroundInstance = LauncherActivity.getForegroundInstance();
        if (foregroundInstance != null && foregroundInstance.launcherService != null) {
            foregroundInstance.launcherService.forEachActivity(Activity::finishAffinity);
            foregroundInstance.launcherService.stopSelf();
            DelayLib.delayed(() -> System.exit(0));
        } else System.exit(0);
    }
}
