package com.threethan.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import com.threethan.launcher.platforms.AbstractPlatform;
import com.threethan.launcher.ui.GroupsAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** @noinspection deprecation*/
public class SettingsManager {
    public static final String KEY_SCALE = "KEY_CUSTOM_SCALE";
    public static final String KEY_MARGIN = "KEY_CUSTOM_MARGIN";
    static final int DEFAULT_SCALE = 112;
    static final int DEFAULT_MARGIN = 32;

    public static final String KEY_EDIT_MODE = "KEY_EDIT_MODE";
    public static final String KEY_SEEN_LAUNCH_OUT_POPUP = "KEY_SEEN_LAUNCH_OUT_POPUP";
    public static final String KEY_SEEN_HIDDEN_GROUPS_POPUP = "KEY_SEEN_HIDDEN_GROUPS_POPUP";
    public static final String NEEDS_META_DATA = "NEEDS_META_DATA";
    public static final String KEY_VR_SET = "KEY_VR_SET";
    public static final String KEY_2D_SET = "KEY_2D_SET";
    public static final String KEY_SUPPORTED_SET = "KEY_SUPPORTED_SET";
    public static final String KEY_UNSUPPORTED_SET = "KEY_UNSUPPORTED_SET";

    // banner-style display by app type
    public static final String KEY_WIDE_VR = "KEY_WIDE_VR";
    public static final String KEY_WIDE_2D = "KEY_WIDE_2D";
    public static final boolean DEFAULT_WIDE_VR = true;
    public static final boolean DEFAULT_WIDE_2D = false;

    // show names by display type
    public static final String KEY_SHOW_NAMES_ICON = "KEY_CUSTOM_NAMES";
    public static final String KEY_SHOW_NAMES_WIDE = "KEY_CUSTOM_NAMES_WIDE";
    static final boolean DEFAULT_SHOW_NAMES_ICON = true;
    static final boolean DEFAULT_SHOW_NAMES_WIDE = true;

    private static SettingsManager instance;
    private final String KEY_APP_GROUPS = "prefAppGroups";
    private final String KEY_APP_LIST = "prefAppList";
    private final String KEY_LAUNCH_OUT = "prefLaunchOutList";
    private final String KEY_SELECTED_GROUPS = "prefSelectedGroups";

    // group
    public static final String KEY_GROUP_2D = "KEY_DEFAULT_GROUP_2D";
    public static final String KEY_GROUP_VR = "KEY_DEFAULT_GROUP_VR";
    public static final String DEFAULT_GROUP_2D = "Apps";
    public static final String DEFAULT_GROUP_VR = "Games";

    // theme
    public static final String KEY_BACKGROUND = "KEY_CUSTOM_THEME";
    public static final String KEY_DARK_MODE = "KEY_DARK_MODE";
    public static final String KEY_GROUPS_ENABLED = "KEY_GROUPS_ENABLED";
    static final int DEFAULT_BACKGROUND = 0;
    static final boolean DEFAULT_DARK_MODE = true;
    static final boolean DEFAULT_GROUPS_ENABLED = true;

    static final int[] BACKGROUND_DRAWABLES = {
            R.drawable.bg_px_blue,
            R.drawable.bg_px_grey,
            R.drawable.bg_px_red,
            R.drawable.bg_px_white,
            R.drawable.bg_px_orange,
            R.drawable.bg_px_green,
            R.drawable.bg_px_purple,
            R.drawable.bg_meta,
    };
    static final int[] BACKGROUND_COLORS = {
            Color.parseColor("#25374f"),
            Color.parseColor("#eaebea"),
            Color.parseColor("#f89b94"),
            Color.parseColor("#d9d4da"),
            Color.parseColor("#f9ce9b"),
            Color.parseColor("#e4eac8"),
            Color.parseColor("#74575c"),
            Color.parseColor("#202a36"),
    };
    static final boolean[] BACKGROUND_DARK = {
            true,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
    };

    //compat
    public final String KEY_COMPATIBILITY_VERSION = "KEY_COMPATIBILITY_VERSION";
    public static int CURRENT_COMPATIBILITY_VERSION = 1;
    private static final List<Integer> VERSIONS_WITH_BACKGROUND_CHANGES = Collections.singletonList(1);

    //storage
    private final SharedPreferences sharedPreferences;
    private Map<String, String> appListMap = new HashMap<>();
    private Set<String> appGroupsSet = new HashSet<>();
    private Set<String> selectedGroupsSet = new HashSet<>();
    private static Set<String> appsToLaunchOut = new HashSet<>();

    private SettingsManager(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (SettingsManager.instance == null) {
            SettingsManager.instance = new SettingsManager(context);
        }
        return SettingsManager.instance;
    }

    public static String getAppDisplayName(Context context, String pkg, CharSequence label) {
        String name = PreferenceManager.getDefaultSharedPreferences(context).getString(pkg, "");
        if (!name.isEmpty()) {
            return name;
        }

        String retVal = label.toString();
        if (retVal.equals("")) {
            retVal = pkg;
        }
        return retVal;
    }

    public static boolean getAppLaunchOut(String pkg) {
        return (appsToLaunchOut.contains(pkg));
    }

    public static void setAppLaunchOut(String pkg, boolean shouldLaunchOut) {
        if (shouldLaunchOut) appsToLaunchOut.add(pkg);
        else appsToLaunchOut.remove(pkg);
    }

    public Map<String, String> getAppList() {
        readValues();
        return appListMap;
    }

    public void setAppList(Map<String, String> appList) {
        appListMap = appList;
        storeValues();
    }

    public ArrayList<ApplicationInfo> getInstalledApps(MainActivity mainActivity, List<String> selected, boolean first, List<ApplicationInfo> allApps) {

        // Get list of installed apps
        Map<String, String> apps = getAppList();

        //Start Auto Sort"

        // Sort into groups
        for (ApplicationInfo app : allApps) {
            if (!appListMap.containsKey(app.packageName)) {
                final boolean isVr = AbstractPlatform.isVirtualRealityApp(app, mainActivity);
                appListMap.put(app.packageName, getDefaultGroup(isVr));
            }
        }
        // Since this goes over all apps & checks if they're vr, we can safely decide we don't need meta data for them on subsequent launchers
        mainActivity.sharedPreferences.edit().putBoolean(SettingsManager.NEEDS_META_DATA, false).apply();

        // Save Auto Sort


        ArrayList<ApplicationInfo> installedApplications = new ArrayList<>(allApps);

        // Save changes to app list
        setAppList(appListMap);

        // Map Packages

        // Put them into a map with package name as keyword for faster handling
        String packageName = mainActivity.getApplicationContext().getPackageName();
        Map<String, ApplicationInfo> appMap = new LinkedHashMap<>();
        for (ApplicationInfo installedApplication : installedApplications) {
            String pkg = installedApplication.packageName;

            boolean showAll = selected.isEmpty();
            boolean isNotAssigned = !apps.containsKey(pkg) && first;
            boolean isInGroup = apps.containsKey(pkg) && selected.contains(apps.get(pkg));

            if (showAll || isNotAssigned || isInGroup) {
                if (AbstractPlatform.isSupportedApp(installedApplication, mainActivity) && !pkg.equals(packageName)) {
                    appMap.put(pkg, installedApplication);
                }
            }
        }

        // Sort by Package Name

        // Create new list of apps
        ArrayList<ApplicationInfo> sortedApps = new ArrayList<>(appMap.values());
        PackageManager packageManager = mainActivity.getPackageManager();
        // Compare on app name (fast)
        sortedApps.sort(Comparator.comparing(a -> ((String) a.loadLabel(packageManager))));
        // Sort Done!
        return sortedApps;
    }

    public Set<String> getAppGroups() {
        readValues();
        return appGroupsSet;
    }

    public void setAppGroups(Set<String> appGroups) {
        appGroupsSet = appGroups;
        storeValues();
    }

    public Set<String> getSelectedGroups() {
        readValues();
        return selectedGroupsSet;
    }

    public void setSelectedGroups(Set<String> appGroups) {
        selectedGroupsSet = appGroups;
        storeValues();
    }

    public ArrayList<String> getAppGroupsSorted(boolean selected) {
        readValues();
        ArrayList<String> sortedApplicationList = new ArrayList<>(selected ? selectedGroupsSet : appGroupsSet);
        sortedApplicationList.sort(Comparator.comparing(String::toUpperCase));

        // Move vr group to start
        if (sortedApplicationList.contains(getDefaultGroup(true))) {
            sortedApplicationList.remove(getDefaultGroup(true));
            sortedApplicationList.add(0, getDefaultGroup(true));
        }
        // Move hidden group to end
        if (sortedApplicationList.contains(GroupsAdapter.HIDDEN_GROUP)) {
            sortedApplicationList.remove(GroupsAdapter.HIDDEN_GROUP);
            sortedApplicationList.add(GroupsAdapter.HIDDEN_GROUP);
        }
        return sortedApplicationList;
    }

    public void resetGroups(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String group : appGroupsSet) {
            editor.remove(KEY_APP_LIST + group);
        }
        editor.remove(KEY_APP_GROUPS);
        editor.remove(KEY_SELECTED_GROUPS);
        editor.remove(KEY_APP_LIST);
        editor.remove(KEY_GROUP_2D);
        editor.remove(KEY_GROUP_VR);
        editor.apply();
    }

    public String getDefaultGroup(boolean vr) {
        final String group = vr ? sharedPreferences.getString(KEY_GROUP_VR, DEFAULT_GROUP_VR) : sharedPreferences.getString(KEY_GROUP_2D, DEFAULT_GROUP_2D);
        if (!appGroupsSet.contains(group)) return GroupsAdapter.HIDDEN_GROUP;
        else return group;
    }

    synchronized void readValues() {
        try {
            Set<String> defaultGroupsSet = new HashSet<>();
            defaultGroupsSet.add(DEFAULT_GROUP_VR);
            defaultGroupsSet.add(DEFAULT_GROUP_2D);
            appGroupsSet = sharedPreferences.getStringSet(KEY_APP_GROUPS, defaultGroupsSet);
            selectedGroupsSet = sharedPreferences.getStringSet(KEY_SELECTED_GROUPS, defaultGroupsSet);
            appsToLaunchOut = sharedPreferences.getStringSet(KEY_LAUNCH_OUT, defaultGroupsSet);

            appListMap.clear();

            appGroupsSet.add(GroupsAdapter.HIDDEN_GROUP);
            for (String group : appGroupsSet) {
                Set<String> appListSet = new HashSet<>();
                appListSet = sharedPreferences.getStringSet(KEY_APP_LIST+group, appListSet);

                for (String app : appListSet) {
                    appListMap.put(app, group);
                }
            }

            appsToLaunchOut = sharedPreferences.getStringSet(KEY_LAUNCH_OUT, defaultGroupsSet);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private synchronized void storeValues() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet(KEY_APP_GROUPS, appGroupsSet);
            editor.putStringSet(KEY_SELECTED_GROUPS, selectedGroupsSet);
            editor.putStringSet(KEY_LAUNCH_OUT, appsToLaunchOut);

            Map<String, Set<String>> appListSetMap = new HashMap<>();
            for (String group : appGroupsSet) {
                appListSetMap.put(group, new HashSet<>());
            }
            for (String pkg : appListMap.keySet()) {
                Set<String> group = appListSetMap.get(appListMap.get(pkg));
                if (group == null) {
                    Log.w("Package Didn't have a group! It will be added to the default 2D group.", pkg);
                    group = appListSetMap.get(getDefaultGroup(false));
                }
                if (group == null) {
                    Log.e("Group was null", pkg);
                    return;
                }
                group.add(pkg);
            }
            for (String group : appGroupsSet) {
                editor.putStringSet(KEY_APP_LIST + group, appListSetMap.get(group));
            }
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String addGroup() {
        String newGroupName = "New";
        List<String> existingGroups = getAppGroupsSorted(false);
        if (existingGroups.contains(newGroupName)) {
            int index = 1;
            while (existingGroups.contains(newGroupName + " " + index)) {
                index++;
            }
            newGroupName = newGroupName + " " + index;
        }
        existingGroups.add(newGroupName);
        setAppGroups(new HashSet<>(existingGroups));
        return newGroupName;
    }

    public void selectGroup(String name) {
        Set<String> selectFirst = new HashSet<>();
        selectFirst.add(name);
        setSelectedGroups(selectFirst);
    }

    public void setAppDisplayName(Context context, ApplicationInfo appInfo, String newName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(appInfo.packageName, newName);
        editor.apply();
    }
    synchronized void checkCompatibilityUpdate(MainActivity mainActivity) {
        int storedVersion = sharedPreferences.getInt(KEY_COMPATIBILITY_VERSION, -1);
        if (storedVersion == -1) {
            if (sharedPreferences.getInt(KEY_BACKGROUND, -1) == -1) return; // return if fresh install
            storedVersion = 0; // set version to 0 if coming from a version before this system was added
        }

        if (storedVersion == CURRENT_COMPATIBILITY_VERSION) return; //Return if no update

        try {
            if (storedVersion > CURRENT_COMPATIBILITY_VERSION)
                Log.e("CompatibilityUpdate Error", "Previous version greater than current!");
            // If updated
            for (int version = 0; version <= CURRENT_COMPATIBILITY_VERSION; version++) {
                if (VERSIONS_WITH_BACKGROUND_CHANGES.contains(version)) {
                    int backgroundIndex = sharedPreferences.getInt(KEY_BACKGROUND, DEFAULT_BACKGROUND);
                    if (backgroundIndex >= 0 && backgroundIndex < BACKGROUND_DARK.length) {
                        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, BACKGROUND_DARK[backgroundIndex]).apply();
                    } else if (storedVersion == 0) {
                        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE).apply();
                    }
                    // updates may reference the specific version in the future
                }
                if (version == 0) {
                    SettingsManager settingsManager = mainActivity.settingsManager;
                    if (sharedPreferences.getInt(KEY_BACKGROUND, DEFAULT_BACKGROUND) == 6) {
                        sharedPreferences.edit().putInt(KEY_BACKGROUND, -1).apply();
                    }
                    final Map<String, String> apps = settingsManager.getAppList();
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
                    HashSet<String> selectedGroup = new HashSet<>();
                    selectedGroup.add(newGroupName);
                    settingsManager.setSelectedGroups(selectedGroup);
                    settingsManager.setAppGroups(appGroupsList);
                    settingsManager.setAppList(updatedAppList);
                    mainActivity.refreshInterface();
                }
            }
            Log.i("Settings Updated", String.format("Updated from v%s to v%s (Settings versions are not the same as app versions)",
                    storedVersion, CURRENT_COMPATIBILITY_VERSION));
        } catch (Exception e) {
            // This *shouldn't* fail, but if it does we should not crash
            Log.e("CompatibilityUpdate Error", "An exception occurred when attempting to perform the compatibility update!");
            e.printStackTrace();
        }

        // Clear the icon cache (failsafe)
        AbstractPlatform.clearIconCache();

        // Store the updated version
        sharedPreferences.edit().putInt(KEY_COMPATIBILITY_VERSION, CURRENT_COMPATIBILITY_VERSION).apply();
    }
}