package com.threethan.launcher.support;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
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

/*
    SettingsManager

    An instance of this class is tied to each launcher activity, and it is used to get and store
    most (but not all) preferences. It handles the conversion of data between types that are usable
    and types which can be stored to shared preferences.

    It handles customizable properties (label, launch mode) as well grouping.

    It also provides a number of static methods which are used by various other classes .
 */
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
    private final WeakReference<LauncherActivity> myLauncherActivityRef;
    private static WeakReference<LauncherActivity> anyLauncherActivityRef = null;
    private static ConcurrentHashMap<String, String> appGroupMap = new ConcurrentHashMap<>();
    private static Set<String> appGroupsSet = Collections.synchronizedSet(new HashSet<>());
    private Set<String> selectedGroupsSet = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Context, SettingsManager> instanceByContext = Collections.synchronizedMap(new HashMap<>());

    private SettingsManager(LauncherActivity activity) {
        myLauncherActivityRef = new WeakReference<>(activity);
        anyLauncherActivityRef = new WeakReference<>(activity);
        sharedPreferences = activity.sharedPreferences;
        sharedPreferenceEditor = activity.sharedPreferenceEditor;
        // Conditional defaults (hacky)
        Settings.DEFAULT_DETAILS_LONG_PRESS = Platform.isTv(activity);
    }

    public static synchronized SettingsManager getInstance(LauncherActivity context) {
        if (instanceByContext.containsKey(context)) return SettingsManager.instanceByContext.get(context);
        instanceByContext.put(context, new SettingsManager(context));
        return instanceByContext.get(context);
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
            PackageManager pm =anyLauncherActivityRef.get().getPackageManager();
            String label = app.loadLabel(pm).toString();
            if (!label.isEmpty()) return label;
            // Try to load this app's real app info
            label = (String) pm.getApplicationInfo(app.packageName, 0).loadLabel(pm);
            if (!label.isEmpty()) return label;
        } catch (NullPointerException | PackageManager.NameNotFoundException ignored) {}
        return app.packageName;
    }
    public static void setAppLabel(ApplicationInfo app, String newName) {
        appLabelCache.put(app, newName);
        sharedPreferenceEditor.putString(app.packageName, newName);
    }
    public static boolean getAppLaunchOut(String pkg) {
        return sharedPreferences.getBoolean(Settings.KEY_LAUNCH_OUT_PREFIX+pkg,
                sharedPreferences.getBoolean(Settings.KEY_DEFAULT_LAUNCH_OUT, DEFAULT_DEFAULT_LAUNCH_OUT));
    }
    public static void setAppLaunchOut(String pkg, boolean shouldLaunchOut) {
        sharedPreferenceEditor.putBoolean(Settings.KEY_LAUNCH_OUT_PREFIX+pkg, shouldLaunchOut).apply();
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
        queueStoreValuesStatic();
    }

    public List<ApplicationInfo> getInstalledApps(LauncherActivity launcherActivity, List<String> selectedGroups, List<ApplicationInfo> myApps) {

        // Get list of installed apps
        Map<String, String> apps = getAppGroupMap();

        if (myApps == null) {
            Log.e("LightningLauncher", "Got null app list");
            return new ArrayList<>();
        }
        // Sort into groups
        for (ApplicationInfo app : myApps) {
            if (!App.isSupported(app, launcherActivity))
                appGroupMap.put(app.packageName, Settings.UNSUPPORTED_GROUP);
            else if (!appGroupMap.containsKey(app.packageName) ||
                    Objects.equals(appGroupMap.get(app.packageName), Settings.UNSUPPORTED_GROUP)){
                final boolean isVr = App.isVirtualReality(app, launcherActivity);
                final boolean isTv = App.isAndroidTv(app, launcherActivity);
                final boolean isWeb = App.isWebsite(app);
                appGroupMap.put(app.packageName, getDefaultGroup(isVr, isTv, isWeb));
            }
        }

        // Sort into groups
        for (ApplicationInfo app : myApps) {
            if (!appGroupMap.containsKey(app.packageName)) {
                if (!App.isSupported(app, launcherActivity)) appGroupMap.put(app.packageName, Settings.UNSUPPORTED_GROUP);
                else {
                    final boolean isVr = App.isVirtualReality(app, launcherActivity);
                    final boolean isTv = App.isAndroidTv(app, launcherActivity);
                    final boolean isWeb = App.isWebsite(app);
                    appGroupMap.put(app.packageName, getDefaultGroup(isVr, isTv, isWeb));
                }
            }
        }

        // Save changes to app list
        setAppGroupMap(appGroupMap);

        // Map Packages
        Map<String, ApplicationInfo> appMap = new LinkedHashMap<>();
        for (ApplicationInfo applicationInfo : myApps) {
            String pkg = applicationInfo.packageName;
            if (apps.containsKey(pkg) && selectedGroups.contains(apps.get(pkg))) {
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
        if (selectedGroupsSet.isEmpty()) {
            Set<String> defaultGroupsSet = new HashSet<>();
            defaultGroupsSet.add(DEFAULT_GROUP_VR);
            defaultGroupsSet.add(DEFAULT_GROUP_TV);
            defaultGroupsSet.add(DEFAULT_GROUP_2D);
//            defaultGroupsSet.add(DEFAULT_GROUP_WEB);

            selectedGroupsSet.addAll(sharedPreferences.getStringSet(KEY_SELECTED_GROUPS, defaultGroupsSet));
        }
        if (myLauncherActivityRef.get() != null &&
                myLauncherActivityRef.get().groupsEnabled || myLauncherActivityRef.get().isEditing()) {

            // Deselect hidden
            if (myLauncherActivityRef.get() != null && !myLauncherActivityRef.get().isEditing()
                    && sharedPreferences.getBoolean(Settings.KEY_AUTO_HIDE_EMPTY, Settings.DEFAULT_AUTO_HIDE_EMPTY)) {
                for (Object group : selectedGroupsSet.toArray()) {
                    if (!appGroupMap.containsValue((String) group))
                        selectedGroupsSet.remove((String) group);
                }
            }
            return selectedGroupsSet;
        } else {
            Set<String> retSet = new HashSet<>(appGroupsSet);
            retSet.remove(Settings.HIDDEN_GROUP);
            return retSet;
        }
    }

    public void setSelectedGroups(Set<String> appGroups) {
        selectedGroupsSet = Collections.synchronizedSet(appGroups);
        queueStoreValues();
    }

    public ArrayList<String> getAppGroupsSorted(boolean selected) {
        if ((selected ? selectedGroupsSet : appGroupsSet).isEmpty()) readValues();
        ArrayList<String> sortedGroupList = new ArrayList<>(selected ? getSelectedGroups() : getAppGroups());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            sortedGroupList.sort(Comparator.comparing(StringLib::forSort));
        else
            Log.e("OLD API", "Your android version is too old (<7.0) " +
                    "so groups can not be sorted," +
                    "which may cause serious issues!");

        // Move hidden group to end
        if (sortedGroupList.contains(Settings.HIDDEN_GROUP)) {
            sortedGroupList.remove(Settings.HIDDEN_GROUP);
            sortedGroupList.add(Settings.HIDDEN_GROUP);
        }

        sortedGroupList.remove(Settings.UNSUPPORTED_GROUP);

        if (myLauncherActivityRef.get() != null && !myLauncherActivityRef.get().isEditing()
                && sharedPreferences.getBoolean(Settings.KEY_AUTO_HIDE_EMPTY, Settings.DEFAULT_AUTO_HIDE_EMPTY)) {
            for (Object group: sortedGroupList.toArray()) {
                if (!appGroupMap.containsValue((String) group)) sortedGroupList.remove((String) group);
            }
        }

        return sortedGroupList;
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

    public static String getDefaultGroup(boolean vr, boolean tv, boolean web) {
        final String key = web ? KEY_GROUP_WEB : (tv ? KEY_GROUP_TV : (vr ? KEY_GROUP_VR : KEY_GROUP_2D));
        final String def = web ? DEFAULT_GROUP_WEB : (tv ? DEFAULT_GROUP_TV : (vr ? DEFAULT_GROUP_VR : DEFAULT_GROUP_2D));
        final String group = sharedPreferences.getString(key, def);
        if (!appGroupsSet.contains(group)) return Settings.HIDDEN_GROUP;
        return group;
    }


    public static synchronized void readValues() {
        try {
            Set<String> defaultGroupsSet = new HashSet<>();
            defaultGroupsSet.add(DEFAULT_GROUP_VR);
            defaultGroupsSet.add(DEFAULT_GROUP_TV);
            defaultGroupsSet.add(DEFAULT_GROUP_2D);
//            defaultGroupsSet.add(DEFAULT_GROUP_WEB);
            appGroupsSet.clear();
            appGroupsSet.addAll(sharedPreferences.getStringSet(KEY_GROUPS, defaultGroupsSet));

            appGroupMap.clear();

            appGroupsSet.add(Settings.HIDDEN_GROUP);
            appGroupsSet.add(Settings.UNSUPPORTED_GROUP);
            for (String group : appGroupsSet) {
                Set<String> appListSet = new HashSet<>();
                appListSet = sharedPreferences.getStringSet(KEY_GROUP_APP_LIST +group, appListSet);
                for (String app : appListSet) appGroupMap.put(app, group);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    synchronized private void queueStoreValues() {
        if (myLauncherActivityRef.get() != null && myLauncherActivityRef.get().mainView != null) {
            myLauncherActivityRef.get().post(SettingsManager::storeValues);
            sharedPreferenceEditor.putStringSet(KEY_SELECTED_GROUPS, selectedGroupsSet);
        }
        else storeValues();
    }
    private static void queueStoreValuesStatic() {
        if (anyLauncherActivityRef.get() != null && anyLauncherActivityRef.get().mainView != null) {
            anyLauncherActivityRef.get().post(SettingsManager::storeValues);
        }
        else storeValues();
    }
    public synchronized static void storeValues() {
        try {
            SharedPreferences.Editor editor = sharedPreferenceEditor;
            editor.putStringSet(KEY_GROUPS, appGroupsSet);

            Map<String, Set<String>> appListSetMap = new HashMap<>();
            for (String group : appGroupsSet) appListSetMap.put(group, new HashSet<>());
            for (String pkg : appGroupMap.keySet()) {
                Set<String> group = appListSetMap.get(appGroupMap.get(pkg));
                if (group == null) group = appListSetMap.get(getDefaultGroup(false, false,false));
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
            return anyLauncherActivityRef.get().browserService.hasWebView(pkgName);
        } catch (NullPointerException ignored) {
            return false;
        }
    }
    public static void stopRunning (String pkgName) {
        try {
            anyLauncherActivityRef.get().browserService.killWebView(pkgName);
        } catch (NullPointerException ignored) {}
    }
 }