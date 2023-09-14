package com.threethan.launcher.support;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.adapter.GroupsAdapter;
import com.threethan.launcher.lib.StringLib;

import java.lang.ref.WeakReference;
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
import java.util.concurrent.ConcurrentHashMap;

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
    private static WeakReference<LauncherActivity> launcherActivityRef = null;
    private static ConcurrentHashMap<String, String> appGroupMap = new ConcurrentHashMap<>();
    private static Set<String> appGroupsSet = Collections.synchronizedSet(new HashSet<>());
    private static Set<String> selectedGroupsSet = Collections.synchronizedSet(new HashSet<>());
    private static Set<String> appsToLaunchOut = Collections.synchronizedSet(new HashSet<>());
    private static SettingsManager instance;

    private SettingsManager(LauncherActivity activity) {
        launcherActivityRef = new WeakReference<>(activity);
        sharedPreferences = activity.sharedPreferences;
        sharedPreferenceEditor = activity.sharedPreferenceEditor;
    }

    public static synchronized SettingsManager getInstance(LauncherActivity context) {
        if (SettingsManager.instance == null) SettingsManager.instance = new SettingsManager(context);
        return SettingsManager.instance;
    }

    public static HashMap<ApplicationInfo, String> appLabelCache = new HashMap<>();
    public static String getAppLabel(ApplicationInfo app) {
        if (appLabelCache.containsKey(app)) return appLabelCache.get(app);
        String name = checkAppLabel(app);
        setAppLabel(app, name);
        return name;
    }
    private static String checkAppLabel(ApplicationInfo app) {
        String name = sharedPreferences.getString(app.packageName, "");
        if (!name.isEmpty()) return name;
        if (App.isWebsite(app)) {
            try {
                name = app.packageName.split("//")[1];
                String[] split = name.split("\\.");
                if (split.length <= 1) name = app.packageName;
                else if (split.length == 2) name = split[0];
                else name = split[1];

                if (!name.isEmpty()) return StringLib.toTitleCase(name);
            } catch (Exception ignored) {}
        }
        try {
            PackageManager pm = launcherActivityRef.get().getPackageManager();
            String label = app.loadLabel(pm).toString();
            if (!label.isEmpty()) return label;
            // Try to load this app's real app info
            label = (String) pm.getApplicationInfo(app.packageName, 0).loadLabel(pm);
            if (!label.isEmpty()) return label;
        } catch (Exception ignored) {}
        return app.packageName;
    }
    public static void setAppLabel(ApplicationInfo app, String newName) {
        appLabelCache.put(app, newName);
        sharedPreferenceEditor.putString(app.packageName, newName);
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
    public void setAppGroup(String packageName, String group) {
        getAppGroupMap();
        appGroupMap.put(packageName, group);
        queueStoreValues();
    }

    public static void setAppGroupMap(Map<String, String> value) {
        appGroupMap = new ConcurrentHashMap<>(value);
        queueStoreValues();
    }

    public List<ApplicationInfo> getInstalledApps(LauncherActivity launcherActivity, List<String> selected, List<ApplicationInfo> myApps) {

        // Get list of installed apps
        Map<String, String> apps = getAppGroupMap();

        if (myApps == null) {
            Log.e("LightningLauncher", "Got null app list");
            return new ArrayList<>();
        }
        // Sort into groups
        for (ApplicationInfo app : myApps) {
            if (!App.isSupported(app, launcherActivity))
                appGroupMap.put(app.packageName, GroupsAdapter.UNSUPPORTED_GROUP);
            else if (!appGroupMap.containsKey(app.packageName) ||
                    Objects.equals(appGroupMap.get(app.packageName), GroupsAdapter.UNSUPPORTED_GROUP)){
                final boolean isVr = App.isVirtualReality(app, launcherActivity);
                final boolean isWeb = App.isWebsite(app);
                appGroupMap.put(app.packageName, getDefaultGroup(isVr, isWeb));
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

        // Create new list of apps
        ArrayList<ApplicationInfo> sortedApps = new ArrayList<>(appMap.values());
        // Compare on app label
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            sortedApps.sort(Comparator.comparing(a -> StringLib.forSort(getAppLabel(a))));
        else
            Log.w("OLD API", "Your android version is too old so apps will not be sorted.");

        // Sort Done!
        return sortedApps;
    }

    public Set<String> getAppGroups() {
        if (appGroupsSet.isEmpty()) readValues();
        return appGroupsSet;
    }

    public void setAppGroups(Set<String> appGroups) {
        appGroupsSet = Collections.synchronizedSet(appGroups);
        queueStoreValues();
    }

    public Set<String> getSelectedGroups() {
        if (selectedGroupsSet.isEmpty()) readValues();
        return selectedGroupsSet;
    }

    public void setSelectedGroups(Set<String> appGroups) {
        selectedGroupsSet = Collections.synchronizedSet(appGroups);
        queueStoreValues();
    }

    public ArrayList<String> getAppGroupsSorted(boolean selected) {
        if ((selected ? selectedGroupsSet : appGroupsSet).isEmpty()) readValues();
        ArrayList<String> sortedGroupMap = new ArrayList<>(selected ? selectedGroupsSet : appGroupsSet);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            sortedGroupMap.sort(Comparator.comparing(StringLib::forSort));
        else
            Log.e("OLD API", "Your android version is too old so groups can not be sorted," +
                    "which may cause serious issues!");

        // Move hidden group to end
        if (sortedGroupMap.contains(GroupsAdapter.HIDDEN_GROUP)) {
            sortedGroupMap.remove(GroupsAdapter.HIDDEN_GROUP);
            sortedGroupMap.add(GroupsAdapter.HIDDEN_GROUP);
        }

        sortedGroupMap.remove(GroupsAdapter.UNSUPPORTED_GROUP);

        return sortedGroupMap;
    }

    public void resetGroups(){
        SharedPreferences.Editor editor = sharedPreferenceEditor;
        for (String group : appGroupsSet) editor.remove(KEY_GROUP_APP_LIST + group);
        appGroupsSet.clear();
        appGroupMap.clear();
        editor.remove(KEY_GROUPS);
        editor.remove(KEY_SELECTED_GROUPS);
        editor.remove(KEY_GROUP_2D);
        editor.remove(KEY_GROUP_VR);
        editor.remove(KEY_GROUP_WEB);
        editor.remove(KEY_VR_SET);
        editor.remove(KEY_2D_SET);
        editor.apply();
        readValues();
        Log.i("Groups (SettingsManager)", "Groups have been reset");
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
            appGroupsSet.clear();
            appGroupsSet.addAll(sharedPreferences.getStringSet(KEY_GROUPS, defaultGroupsSet));
            selectedGroupsSet.clear();
            selectedGroupsSet.addAll(sharedPreferences.getStringSet(KEY_SELECTED_GROUPS, defaultGroupsSet));
            appsToLaunchOut.clear();
            appsToLaunchOut.addAll(sharedPreferences.getStringSet(KEY_LAUNCH_OUT, defaultGroupsSet));

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
        if (launcherActivityRef.get().mainView != null) launcherActivityRef.get().post(SettingsManager::storeValues);
        else storeValues();
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
                if (group == null) group = appListSetMap.get(getDefaultGroup(false, false));
                if (group == null) {
                    Log.e("Group was null", pkg);
                    return;
                }
                group.add(pkg);
            }
            for (String group : appGroupsSet) {
                editor.putStringSet(KEY_GROUP_APP_LIST + group, appListSetMap.get(group));
            }
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String addGroup() {
        String newGroupName = "New";
        List<String> existingGroups = getAppGroupsSorted(false);
        if (
               existingGroups.contains(StringLib.setStarred(newGroupName, false)) ||
               existingGroups.contains(StringLib.setStarred(newGroupName, true))
        ) {
            int index = 2;
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
            return launcherActivityRef.get().wService.hasWebView(pkgName);
        } catch (Exception ignored) {
            return false;
        }
    }
    public static void stopRunning (String pkgName) {
        try {
            launcherActivityRef.get().wService.killWebView(pkgName);
        } catch (Exception ignored) {}
    }
 }