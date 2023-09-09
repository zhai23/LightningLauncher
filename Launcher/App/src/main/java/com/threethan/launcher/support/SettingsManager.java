package com.threethan.launcher.support;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.FileLib;
import com.threethan.launcher.adapter.GroupsAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsManager extends Settings {
    public static List<Integer> getVersionsWithBackgroundChanges() {
        List<Integer> out = new ArrayList<>();
        out.add(1);
        out.add(2);
        return out;
    }

    //storage
    private static SharedPreferences sharedPreferences = null;
    private static SharedPreferences.Editor sharedPreferenceEditor = null;
    @SuppressLint("StaticFieldLeak")
    private static LauncherActivity launcherActivity = null;
    private static Map<String, String> appGroupMap = new HashMap<>();
    private static Set<String> appGroupsSet = new HashSet<>();
    private static Set<String> selectedGroupsSet = new HashSet<>();
    private static Set<String> appsToLaunchOut = new HashSet<>();

    private static SettingsManager instance;

    private SettingsManager(LauncherActivity activity) {
        launcherActivity = activity;
        sharedPreferences = launcherActivity.sharedPreferences;
        sharedPreferenceEditor = launcherActivity.sharedPreferenceEditor;
    }

    public static synchronized SettingsManager getInstance(LauncherActivity context) {
        if (SettingsManager.instance == null) SettingsManager.instance = new SettingsManager(context);
        return SettingsManager.instance;
    }

    public static HashMap<ApplicationInfo, String> appLabelCache = new HashMap<>();
    public static String getAppLabel(ApplicationInfo appInfo) {
        if (appLabelCache.containsKey(appInfo)) return appLabelCache.get(appInfo);
        String name = checkAppLabel(appInfo);
        setAppLabel(appInfo, name);
        return name;
    }
    private static String checkAppLabel(ApplicationInfo appInfo) {
        String name = sharedPreferences.getString(appInfo.packageName, "");
        if (!name.isEmpty()) return name;
        if (App.isWebsite(appInfo)) {
            name = appInfo.packageName.split("//")[1];
            String[] split = name.split("\\.");
            if      (split.length <= 1) name = appInfo.packageName;
            else if (split.length == 2) name = split[0];
            else                        name = split[1];

            if (!name.isEmpty()) return FileLib.toTitleCase(name);
        }
        try {
            PackageManager pm = launcherActivity.getPackageManager();
            String label = appInfo.loadLabel(pm).toString();
            if (!label.isEmpty()) return label;
            // Try to load this app's real app info
            label = (String) pm.getApplicationInfo(appInfo.packageName, 0).loadLabel(pm);
            if (!label.isEmpty()) return label;
        } catch (Exception ignored) {}
        return appInfo.packageName;
    }
    public static void setAppLabel(ApplicationInfo appInfo, String newName) {
        appLabelCache.put(appInfo, newName);
        sharedPreferenceEditor.putString(appInfo.packageName, newName);
    }
    public static boolean getAppLaunchOut(String pkg) {
        return (appsToLaunchOut.contains(pkg));
    }

    public static void setAppLaunchOut(String pkg, boolean shouldLaunchOut) {
        if (shouldLaunchOut) appsToLaunchOut.add(pkg);
        else appsToLaunchOut.remove(pkg);
    }

    public static Map<String, String> getAppGroupMap() {
        if (appGroupMap.isEmpty()) readValues();
        return appGroupMap;
    }

    public static void setAppGroupMap(Map<String, String> value) {
        appGroupMap = value;
        queueStoreValues();
    }

    public List<ApplicationInfo> getInstalledApps(LauncherActivity launcherActivity, List<String> selected, List<ApplicationInfo> myApps) {

        // Get list of installed apps
        Map<String, String> apps = getAppGroupMap();

        // If we need meta data, fetch everything now
        if (launcherActivity.sharedPreferences.getBoolean(SettingsManager.NEEDS_META_DATA, true)) {
            // Sort into groups
            for (ApplicationInfo app : myApps) {
                if (!App.isSupported(app, launcherActivity)) appGroupMap.put(app.packageName, GroupsAdapter.UNSUPPORTED_GROUP);
                else {
                    final boolean isVr = App.isVirtualReality(app, launcherActivity);
                    final boolean isWeb = App.isWebsite(app);
                    appGroupMap.put(app.packageName, getDefaultGroup(isVr, isWeb));
                }
            }
        }

        // Sort into groups
        for (ApplicationInfo app : myApps) {
            if (!appGroupMap.containsKey(app.packageName)) {
                if (!App.isSupported(app, launcherActivity)) appGroupMap.put(app.packageName, GroupsAdapter.UNSUPPORTED_GROUP);
                else {
                    final boolean isVr = App.isVirtualReality(app, launcherActivity);
                    final boolean isWeb = App.isWebsite(app);
                    appGroupMap.put(app.packageName, getDefaultGroup(isVr, isWeb));
                }
            }
        }


        // Save changes to app list
        setAppGroupMap(appGroupMap);

        //Log.v("LauncherStartup", "X3 - Map Packages");

        // Map Packages
        Map<String, ApplicationInfo> appMap = new LinkedHashMap<>();
        for (ApplicationInfo applicationInfo : myApps) {
            String pkg = applicationInfo.packageName;
            if (apps.containsKey(pkg) && selected.contains(apps.get(pkg))) {
                try {
                    appMap.put(pkg, applicationInfo);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        //Log.v("LauncherStartup", "X4 - Mapped Packages");


        // Sort by Package Name

        // Create new list of apps
        ArrayList<ApplicationInfo> sortedApps = new ArrayList<>(appMap.values());
        // Compare on app name (fast)
        //Log.v("LauncherStartup", "X5 - Start Sort");
        sortedApps.sort(Comparator.comparing(a -> getAppLabel(a).toLowerCase()));
        //Log.v("LauncherStartup", "X6 - Finish Sort");


        // Sort Done!
        return sortedApps;
    }

    public Set<String> getAppGroups() {
        if (appGroupsSet.isEmpty()) readValues();
        return appGroupsSet;
    }

    public void setAppGroups(Set<String> appGroups) {
        appGroupsSet = appGroups;
        queueStoreValues();
    }

    public Set<String> getSelectedGroups() {
        if (selectedGroupsSet.isEmpty()) readValues();
        return selectedGroupsSet;
    }

    public void setSelectedGroups(Set<String> appGroups) {
        selectedGroupsSet = appGroups;
        queueStoreValues();
    }

    public ArrayList<String> getAppGroupsSorted(boolean selected) {
        if ((selected ? selectedGroupsSet : appGroupsSet).isEmpty()) readValues();
        ArrayList<String> sortedAppGroupMap = new ArrayList<>(selected ? selectedGroupsSet : appGroupsSet);
        sortedAppGroupMap.sort(Comparator.comparing(String::toUpperCase));

        // Move vr group to start
        final String vrGroup = getDefaultGroup(true, false);
        if (sortedAppGroupMap.contains(vrGroup)) {
            sortedAppGroupMap.remove(vrGroup);
            sortedAppGroupMap.add(0, vrGroup);
        }
        // Move hidden group to end
        if (sortedAppGroupMap.contains(GroupsAdapter.HIDDEN_GROUP)) {
            sortedAppGroupMap.remove(GroupsAdapter.HIDDEN_GROUP);
            sortedAppGroupMap.add(GroupsAdapter.HIDDEN_GROUP);
        }

        sortedAppGroupMap.remove(GroupsAdapter.UNSUPPORTED_GROUP);

        return sortedAppGroupMap;
    }

    public void resetGroups(){
        SharedPreferences.Editor editor = sharedPreferenceEditor;
        for (String group : appGroupsSet) {
            editor.remove(KEY_GROUP_APP_LIST + group);
        }
        editor.remove(KEY_GROUPS);
        editor.remove(KEY_SELECTED_GROUPS);
        editor.remove(KEY_GROUP_APP_LIST);
        editor.remove(KEY_GROUP_2D);
        editor.remove(KEY_GROUP_VR);
    }

    public static String getDefaultGroup(boolean vr, boolean web) {
        final String key = web ? KEY_GROUP_WEB : (vr ? KEY_GROUP_VR : KEY_GROUP_2D);
        final String def = web ? DEFAULT_GROUP_WEB : (vr ? DEFAULT_GROUP_VR : DEFAULT_GROUP_2D);
        final String group = sharedPreferences.getString(key, def);
        if (!appGroupsSet.contains(group)) return GroupsAdapter.HIDDEN_GROUP;
        return group;
    }


    public synchronized static void readValues() {
        try {
            Set<String> defaultGroupsSet = new HashSet<>();
            defaultGroupsSet.add(DEFAULT_GROUP_VR);
            defaultGroupsSet.add(DEFAULT_GROUP_2D);
            appGroupsSet = sharedPreferences.getStringSet(KEY_GROUPS, defaultGroupsSet);
            selectedGroupsSet = sharedPreferences.getStringSet(KEY_SELECTED_GROUPS, defaultGroupsSet);
            appsToLaunchOut = sharedPreferences.getStringSet(KEY_LAUNCH_OUT, defaultGroupsSet);

            appGroupMap.clear();

            appGroupsSet.add(GroupsAdapter.HIDDEN_GROUP);
            appGroupsSet.add(GroupsAdapter.UNSUPPORTED_GROUP);
            for (String group : appGroupsSet) {
                Set<String> appListSet = new HashSet<>();
                appListSet = sharedPreferences.getStringSet(KEY_GROUP_APP_LIST +group, appListSet);
                for (String app : appListSet) appGroupMap.put(app, group);
            }
            appsToLaunchOut = sharedPreferences.getStringSet(KEY_LAUNCH_OUT, defaultGroupsSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void queueStoreValues() {
        if (launcherActivity.mainView != null) launcherActivity.post(SettingsManager::storeValues);
    }
    public synchronized static void storeValues() {
        try {
            SharedPreferences.Editor editor = sharedPreferenceEditor;
            editor.putStringSet(KEY_GROUPS, appGroupsSet);
            editor.putStringSet(KEY_SELECTED_GROUPS, selectedGroupsSet);
            editor.putStringSet(KEY_LAUNCH_OUT, appsToLaunchOut);

            Map<String, Set<String>> appListSetMap = new HashMap<>();
            for (String group : appGroupsSet) appListSetMap.put(group, new HashSet<>());
            for (String pkg : appGroupMap.keySet()) {
                Set<String> group = appListSetMap.get(appGroupMap.get(pkg));
                if (group == null) {
                    Log.i("Missing group! Maybe in transit?", pkg);
                    group = appListSetMap.get(getDefaultGroup(false, false));
                }
                if (group == null) {
                    Log.e("Group was null", pkg);
                    return;
                }
                group.add(pkg);
            }
            for (String group : appGroupsSet) {
                editor.putStringSet(KEY_GROUP_APP_LIST + group, appListSetMap.get(group));
            }
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

    public boolean selectGroup(String name) {
        Set<String> selectedGroups = getSelectedGroups();
        if (selectedGroups.size() == 1 && selectedGroups.contains(name)) return false; // Cancel if clicking same group

        Set<String> selectFirst = new HashSet<>();
        selectFirst.add(name);
        setSelectedGroups(selectFirst);
        return true;
    }

    public static boolean getRunning(String pkgName) {
        if (!App.isWebsite(pkgName)) return false;
        try {
            return launcherActivity.wService.hasWebView(pkgName);
        } catch (Exception ignored) {
            return false;
        }
    }
    public static void stopRunning (String pkgName) {
        try {
            launcherActivity.wService.killWebView(pkgName);
        } catch (Exception ignored) {}
    }
 }