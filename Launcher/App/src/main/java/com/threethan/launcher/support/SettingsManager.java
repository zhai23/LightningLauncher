package com.threethan.launcher.support;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.AppData;
import com.threethan.launcher.helper.DataStoreEditor;
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
    private static DataStoreEditor dataStoreEditor = null;
    private final WeakReference<LauncherActivity> myLauncherActivityRef;
    private static WeakReference<LauncherActivity> anyLauncherActivityRef = null;
    private static ConcurrentHashMap<String, String> appGroupMap = new ConcurrentHashMap<>();
    private static Set<String> appGroupsSet = Collections.synchronizedSet(new HashSet<>());
    private Set<String> selectedGroupsSet = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Context, SettingsManager> instanceByContext = Collections.synchronizedMap(new HashMap<>());
    private SettingsManager(LauncherActivity activity) {
        myLauncherActivityRef = new WeakReference<>(activity);
        anyLauncherActivityRef = new WeakReference<>(activity);
        dataStoreEditor = activity.dataStoreEditor;
        // Conditional defaults (hacky)
        Settings.DEFAULT_DETAILS_LONG_PRESS = Platform.isTv(activity);
    }
    public static LauncherActivity getAnyLauncherActivity() {
        return anyLauncherActivityRef.get();
    }

    public static synchronized SettingsManager getInstance(LauncherActivity context) {
        if (instanceByContext.containsKey(context)) return SettingsManager.instanceByContext.get(context);
        instanceByContext.put(context, new SettingsManager(context));
        return instanceByContext.get(context);
    }

    public static HashMap<ApplicationInfo, String> appLabelCache = new HashMap<>();
    public static String getAppLabel(ApplicationInfo app) {
        if (appLabelCache.containsKey(app)) return appLabelCache.get(app);
        return checkAppLabel(app);
    }
    public static String getSortableAppLabel(ApplicationInfo app) {
        return  (App.isBanner(app) ? "0" : "1") + StringLib.forSort(getAppLabel(app));
    }
    private static String checkAppLabel(ApplicationInfo app) {
        dataStoreEditor.getString(app.packageName, "",
                name -> setAppLabel(app, processAppLabel(app, name)));

        return app.packageName;
    }
    private static @Nullable String processAppLabel(ApplicationInfo app, String name) {
        if (!name.isEmpty()) return name;
        if (AppData.labelOverrides.containsKey(app.packageName))
            return AppData.labelOverrides.get(app.packageName);
        if (App.isWebsite(app) || StringLib.isSearchUrl(app.packageName)) {
            try {
                name = app.packageName.split("//")[1];
                String[] split = name.split("\\.");
                if (split.length <= 1) name = app.packageName;
                else if (split.length == 2) name = split[0];
                else name = split[1];

                if (StringLib.isSearchUrl(app.packageName))
                    name += " Search";

                if (!name.isEmpty()) return StringLib.toTitleCase(name);
            } catch (Exception ignored) {
            }
        }
        try {
            PackageManager pm = anyLauncherActivityRef.get().getPackageManager();
            String label = app.loadLabel(pm).toString();
            if (!label.isEmpty()) return label;
            // Try to load this app's real app info
            label = (String) app.loadLabel(pm);
            if (!label.isEmpty()) return label;
        } catch (NullPointerException ignored) {}
        return null;
    }

    public static void setAppLabel(ApplicationInfo app, String newName) {
        if (newName == null) return;
        getAnyLauncherActivity().getAppAdapter().notifyAppChanged(app);
        appLabelCache.put(app, newName);
        dataStoreEditor.putString(app.packageName, newName);
    }
    public static boolean getAppLaunchOut(String pkg) {
        return dataStoreEditor.getBoolean(Settings.KEY_LAUNCH_OUT_PREFIX+pkg,
                dataStoreEditor.getBoolean(Settings.KEY_DEFAULT_LAUNCH_OUT, DEFAULT_DEFAULT_LAUNCH_OUT));
    }
    public static void setAppLaunchOut(String pkg, boolean shouldLaunchOut) {
        dataStoreEditor.putBoolean(Settings.KEY_LAUNCH_OUT_PREFIX+pkg, shouldLaunchOut);
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
            if (!App.isSupported(app))
                appGroupMap.put(app.packageName, Settings.UNSUPPORTED_GROUP);
            else if (!appGroupMap.containsKey(app.packageName) ||
                    Objects.equals(appGroupMap.get(app.packageName), Settings.UNSUPPORTED_GROUP)){
                appGroupMap.put(app.packageName, App.getDefaultGroupFor(App.getType(launcherActivity, app)));
            }
        }

        // Save changes to app list
        setAppGroupMap(appGroupMap);

        // Map Packages
        Map<String, ApplicationInfo> appMap = new ConcurrentHashMap<>();
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
//         Compare on app label
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            sortedApps.sort(Comparator.comparing(SettingsManager::getSortableAppLabel));
        else
            Log.w("OLD API", "Your android version is too old so apps will not be sorted.");

        // Sort Done!
        return sortedApps;
    }

    public static Set<String> getAppGroups() {
        if (appGroupsSet.isEmpty()) readValues();
        return appGroupsSet;
    }

    public void setAppGroups(Set<String> appGroups) {
        appGroupsSet = Collections.synchronizedSet(appGroups);
        queueStoreValues();
    }

    public static Set<String> getDefaultGroupsSet() {
        LauncherActivity launcherActivity = anyLauncherActivityRef.get();
        if (launcherActivity == null) return Collections.emptySet();

        Set<String> defaultGroupsSet = new HashSet<>();
        for (App.Type type : Platform.getSupportedAppTypes(launcherActivity))
            defaultGroupsSet.add(App.getDefaultGroupFor(type));

        return (defaultGroupsSet);
    }
    public Set<String> getSelectedGroups() {
        if (selectedGroupsSet.isEmpty()) {
            selectedGroupsSet.addAll(dataStoreEditor.getStringSet(KEY_SELECTED_GROUPS, getDefaultGroupsSet()));
        }
        if (myLauncherActivityRef.get() != null &&
                myLauncherActivityRef.get().groupsEnabled || myLauncherActivityRef.get().isEditing()) {

            // Deselect hidden
            if (myLauncherActivityRef.get() != null && !myLauncherActivityRef.get().isEditing()
                    && dataStoreEditor.getBoolean(Settings.KEY_AUTO_HIDE_EMPTY, Settings.DEFAULT_AUTO_HIDE_EMPTY)) {
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
                && dataStoreEditor.getBoolean(Settings.KEY_AUTO_HIDE_EMPTY, Settings.DEFAULT_AUTO_HIDE_EMPTY)) {
            for (Object group: sortedGroupList.toArray()) {
                if (!appGroupMap.containsValue((String) group)) sortedGroupList.remove((String) group);
            }
        }

        return sortedGroupList;
    }

    public void resetGroups(){
        SharedPreferences.Editor editor = dataStoreEditor;
        for (String group : appGroupsSet) editor.remove(KEY_GROUP_APP_LIST + group);
        appGroupsSet.clear();
        appGroupMap.clear();
        editor.remove(KEY_GROUPS);
        editor.remove(KEY_SELECTED_GROUPS);
        for (String group : getAppGroups())
            editor.remove(group);

        readValues();
        writeValues();

        Log.i("Groups (SettingsManager)", "Groups have been reset");
    }

    public static synchronized void readValues() {
        try {
            appGroupsSet.clear();
            appGroupsSet.addAll(dataStoreEditor.getStringSet(KEY_GROUPS, getDefaultGroupsSet()));

            appGroupMap.clear();

            appGroupsSet.add(Settings.HIDDEN_GROUP);
            appGroupsSet.add(Settings.UNSUPPORTED_GROUP);
            for (String group : appGroupsSet) {
                Set<String> appListSet = new HashSet<>();
                appListSet = dataStoreEditor.getStringSet(KEY_GROUP_APP_LIST + group, appListSet);
                for (String app : appListSet) appGroupMap.put(app, group);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    synchronized private void queueStoreValues() {
        if (myLauncherActivityRef.get() != null && myLauncherActivityRef.get().mainView != null) {
            myLauncherActivityRef.get().post(SettingsManager::writeValues);
            dataStoreEditor.putStringSet(KEY_SELECTED_GROUPS, selectedGroupsSet);
        }
        else writeValues();
    }
    private static void queueStoreValuesStatic() {
        if (anyLauncherActivityRef == null) {
            Log.w("SettingsManager", "queueValues called too soon");
            return;
        }
        if (anyLauncherActivityRef.get() != null && anyLauncherActivityRef.get().mainView != null) {
            anyLauncherActivityRef.get().post(SettingsManager::writeValues);
        }
        else writeValues();
    }
    public synchronized static void writeValues() {
        try {
            SharedPreferences.Editor editor = dataStoreEditor;
            editor.putStringSet(KEY_GROUPS, appGroupsSet);

            Map<String, Set<String>> appListSetMap = new HashMap<>();
            for (String group : appGroupsSet) appListSetMap.put(group, new HashSet<>());
            for (String pkg : appGroupMap.keySet()) {
                Set<String> group = appListSetMap.get(appGroupMap.get(pkg));
                if (group == null) group = appListSetMap.get(
                        App.getDefaultGroupFor(App.Type.TYPE_SUPPORTED));
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

    public static String getDefaultGroupFor(App.Type type) {
        String key = Settings.KEY_DEFAULT_GROUP + type;
        if (!Settings.FALLBACK_GROUPS.containsKey(type)) type = App.Type.TYPE_PHONE;
        String def = Settings.FALLBACK_GROUPS.get(type);

        return SettingsManager.dataStoreEditor.getString(key, def);
    }

    public static boolean isTypeBanner(App.Type type) {
        String key = Settings.KEY_BANNER + type;
        if (!Settings.FALLBACK_BANNER.containsKey(type)) type = App.Type.TYPE_PHONE;
        boolean def = Boolean.TRUE.equals(Settings.FALLBACK_BANNER.get(type));

        return SettingsManager.dataStoreEditor.getBoolean(key, def);
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

    public static boolean getAdvancedLaunching(LauncherActivity activity) {
        return activity.dataStoreEditor.getBoolean(Settings.KEY_ADVANCED_SIZING,
                Settings.DEFAULT_ADVANCED_SIZING);
    }
 }