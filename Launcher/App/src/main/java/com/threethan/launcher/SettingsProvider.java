package com.threethan.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.threethan.launcher.platforms.AbstractPlatform;
import com.threethan.launcher.ui.GroupsAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @noinspection deprecation*/
public class SettingsProvider {
    public static final String KEY_CUSTOM_NAMES = "KEY_CUSTOM_NAMES";
    public static final String KEY_CUSTOM_NAMES_WIDE = "KEY_CUSTOM_NAMES_WIDE";
    public static final String KEY_CUSTOM_SCALE = "KEY_CUSTOM_SCALE";
    public static final String KEY_CUSTOM_MARGIN = "KEY_CUSTOM_MARGIN";
    public static final String KEY_CUSTOM_THEME = "KEY_CUSTOM_THEME";
    public static final String KEY_EDIT_MODE = "KEY_EDIT_MODE";
    public static final String KEY_DARK_MODE = "KEY_DARK_MODE";
    public static final String KEY_SEEN_LAUNCH_OUT_POPUP = "KEY_SEEN_LAUNCH_OUT_POPUP";
    public static final String NEEDS_META_DATA = "NEEDS_META_DATA";
    public static final String KEY_VR_SET = "KEY_VR_SET";
    public static final String KEY_2D_SET = "KEY_2D_SET";
    public static final String KEY_SUPPORTED_SET = "KEY_SUPPORTED_SET";
    public static final String KEY_UNSUPPORTED_SET = "KEY_UNSUPPORTED_SET";

    public static final String KEY_WIDE_VR = "KEY_WIDE_VR";
    public static final String KEY_WIDE_2D = "KEY_WIDE_2D";
    private static SettingsProvider instance;
    private final String KEY_APP_GROUPS = "prefAppGroups";
    private final String KEY_APP_LIST = "prefAppList";
    private final String KEY_LAUNCH_OUT = "prefLaunchOutList";
    private final String KEY_SELECTED_GROUPS = "prefSelectedGroups";
    //defaults
    static final boolean DEFAULT_DARK_MODE = true;
    static final boolean DEFAULT_NAMES = true;
    static final boolean DEFAULT_NAMES_WIDE = true;
    static final int DEFAULT_SCALE = 112;
    static final int DEFAULT_MARGIN = 32;
    static final int DEFAULT_THEME = 0;

    //compat
    public final String KEY_COMPATIBILITY_VERSION = "KEY_COMPATIBILITY_VERSION";
    public final int DEFAULT_COMPATIBILITY_VERSION = 0;
    public final int CURRENT_COMPATIBILITY_VERSION = 0;
    public final String KEY_WALLPAPER_VERSION = "KEY_COMPATIBILITY_VERSION";
    public final int DEFAULT_WALLPAPER_VERSION = 0;
    public final int CURRENT_WALLPAPER_VERSION = 0;

    //storage
    private final SharedPreferences sharedPreferences;
    private Map<String, String> appListMap = new HashMap<>();
    private Set<String> appGroupsSet = new HashSet<>();
    private Set<String> selectedGroupsSet = new HashSet<>();
    private static Set<String> appsToLaunchOut = new HashSet<>();

    private SettingsProvider(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized SettingsProvider getInstance(Context context) {
        if (SettingsProvider.instance == null) {
            SettingsProvider.instance = new SettingsProvider(context);
        }
        return SettingsProvider.instance;
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

    public Map<String, String> getAppList(Context context) {
        readValues(context);
        return appListMap;
    }

    public void setAppList(Map<String, String> appList, Context context) {
        appListMap = appList;
        storeValues(context);
    }

    public ArrayList<ApplicationInfo> getInstalledApps(MainActivity mainActivity, List<String> selected, boolean first, List<ApplicationInfo> allApps) {

        // Get list of installed apps
        Map<String, String> apps = getAppList(mainActivity);

        //Start Auto Sort"

        //Sort
        if (appGroupsSet.contains(mainActivity.getString(R.string.default_apps_group)) && appGroupsSet.contains(mainActivity.getString(R.string.android_apps_group))) {
            // Sort if groups are present
            for (ApplicationInfo app : allApps) {
                if (!appListMap.containsKey(app.packageName)) {
//                    Log.i("Automatically Sorted", app.packageName);
                    final boolean isVr = AbstractPlatform.isVirtualRealityApp(app, mainActivity);
                    appListMap.put(app.packageName, isVr ? mainActivity.getString(R.string.default_apps_group) : mainActivity.getString(R.string.android_apps_group));
                }
            }
            // Since this goes over all apps & checks if they're vr, we can safely decide we don't need meta data for them on subsequent launchers
            mainActivity.sharedPreferences.edit().putBoolean(SettingsProvider.NEEDS_META_DATA, false).apply();
        }

        // Save Auto Sort


        ArrayList<ApplicationInfo> installedApplications = new ArrayList<>(allApps);

        // Save changes to app list
        setAppList(appListMap, mainActivity);

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

    public Set<String> getAppGroups(Context context) {
        readValues(context);
        return appGroupsSet;
    }

    public void setAppGroups(Set<String> appGroups, Context context) {
        appGroupsSet = appGroups;
        storeValues(context);
    }

    public Set<String> getSelectedGroups(Context context) {
        readValues(context);
        return selectedGroupsSet;
    }

    public void setSelectedGroups(Set<String> appGroups, Context context) {
        selectedGroupsSet = appGroups;
        storeValues(context);
    }

    public ArrayList<String> getAppGroupsSorted(boolean selected, Context context) {
        readValues(context);
        ArrayList<String> sortedApplicationList = new ArrayList<>(selected ? selectedGroupsSet : appGroupsSet);
        sortedApplicationList.sort((a, b) -> {
            if (GroupsAdapter.HIDDEN_GROUP.equals(a)) return 1;
            if (GroupsAdapter.HIDDEN_GROUP.equals(b)) return -1;
            return a.toUpperCase().compareTo(b.toUpperCase());
        });

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
        editor.apply();
    }

     synchronized void readValues(Context context) {
        try {
            Set<String> defaultGroupsSet = new HashSet<>();
            defaultGroupsSet.add(context.getString(R.string.default_apps_group));
            defaultGroupsSet.add(context.getString(R.string.android_apps_group));
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

    private synchronized void storeValues(Context context) {
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
                    Log.w("Package Didn't have a group! It will be added to tools.", pkg);
                    group = appListSetMap.get(context.getString(R.string.android_apps_group));
                }
                assert group != null;
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

    public String addGroup(Context context) {
        String newGroupName = "New";
        List<String> existingGroups = getAppGroupsSorted(false, context);
        if (existingGroups.contains(newGroupName)) {
            int index = 1;
            while (existingGroups.contains(newGroupName + " " + index)) {
                index++;
            }
            newGroupName = newGroupName + " " + index;
        }
        existingGroups.add(newGroupName);
        setAppGroups(new HashSet<>(existingGroups), context);
        return newGroupName;
    }

    public void selectGroup(String name, Context context) {
        Set<String> selectFirst = new HashSet<>();
        selectFirst.add(name);
        setSelectedGroups(selectFirst, context);
    }

    public void setAppDisplayName(Context context, ApplicationInfo appInfo, String newName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(appInfo.packageName, newName);
        editor.apply();
    }
}