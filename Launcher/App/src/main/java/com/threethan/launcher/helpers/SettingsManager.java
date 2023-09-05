package com.threethan.launcher.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import com.threethan.launcher.MainActivity;
import com.threethan.launcher.R;
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
import java.util.Set;

/** @noinspection deprecation*/
public class SettingsManager {
    public static final String KEY_SCALE = "KEY_CUSTOM_SCALE";
    public static final String KEY_MARGIN = "KEY_CUSTOM_MARGIN";
    public static final int DEFAULT_SCALE = 112;
    public static final int DEFAULT_MARGIN = 32;

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
    public static final String KEY_WIDE_WEB = "KEY_WIDE_WEB";
    public static final boolean DEFAULT_WIDE_VR = true;
    public static final boolean DEFAULT_WIDE_2D = false;
    public static final boolean DEFAULT_WIDE_WEB = false;

    // show names by display type
    public static final String KEY_SHOW_NAMES_ICON = "KEY_CUSTOM_NAMES";
    public static final String KEY_SHOW_NAMES_WIDE = "KEY_CUSTOM_NAMES_WIDE";
    public static final boolean DEFAULT_SHOW_NAMES_ICON = true;
    public static final boolean DEFAULT_SHOW_NAMES_WIDE = true;

    private static SettingsManager instance;
    private static final String KEY_APP_GROUPS = "prefAppGroups";
    private static final String KEY_APP_LIST = "prefAppList";
    private static final String KEY_LAUNCH_OUT = "prefLaunchOutList";
    private static final String KEY_SELECTED_GROUPS = "prefSelectedGroups";
    public static final String KEY_WEBSITE_LIST = "prefWebAppNames";

    // group
    public static final String KEY_GROUP_2D = "KEY_DEFAULT_GROUP_2D";
    public static final String KEY_GROUP_VR = "KEY_DEFAULT_GROUP_VR";
    public static final String KEY_GROUP_WEB = "KEY_DEFAULT_GROUP_WEB";
    public static final String DEFAULT_GROUP_2D = "Apps";
    public static final String DEFAULT_GROUP_VR = "Games";
    public static final String DEFAULT_GROUP_WEB = "Apps";

    // theme
    public static final String KEY_BACKGROUND = "KEY_CUSTOM_THEME";
    public static final String KEY_DARK_MODE = "KEY_DARK_MODE";
    public static final String KEY_GROUPS_ENABLED = "KEY_GROUPS_ENABLED";
    public static final int DEFAULT_BACKGROUND = 0;
    public static final boolean DEFAULT_DARK_MODE = true;
    public static final boolean DEFAULT_GROUPS_ENABLED = true;

    public static final int[] BACKGROUND_DRAWABLES = {
            R.drawable.bg_px_blue,
            R.drawable.bg_px_grey,
            R.drawable.bg_px_red,
            R.drawable.bg_px_white,
            R.drawable.bg_px_orange,
            R.drawable.bg_px_green,
            R.drawable.bg_px_purple,
            R.drawable.bg_meta,
    };
    public static final int[] BACKGROUND_COLORS = {
            Color.parseColor("#25374f"),
            Color.parseColor("#eaebea"),
            Color.parseColor("#f89b94"),
            Color.parseColor("#d9d4da"),
            Color.parseColor("#f9ce9b"),
            Color.parseColor("#e4eac8"),
            Color.parseColor("#74575c"),
            Color.parseColor("#202a36"),
    };
    public static final boolean[] BACKGROUND_DARK = {
            true,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
    };

    public static final List<Integer> VERSIONS_WITH_BACKGROUND_CHANGES = Collections.singletonList(1);

    //storage
    private static SharedPreferences sharedPreferences = null;
    private static Map<String, String> appGroupMap = new HashMap<>();
    private static Set<String> appGroupsSet = new HashSet<>();
    private static Set<String> selectedGroupsSet = new HashSet<>();
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

    public static HashMap<ApplicationInfo, String> appNameMap = new HashMap<>();
    public static String getAppDisplayName(Context context, ApplicationInfo appInfo) {
        if (appNameMap.containsKey(appInfo)) return appNameMap.get(appInfo);
        String name = getAppDisplayNameInternal(context, appInfo);
        appNameMap.put(appInfo, name);
        return name;
    }
        private static String getAppDisplayNameInternal(Context context, ApplicationInfo appInfo) {
        String name = PreferenceManager.getDefaultSharedPreferences(context).getString(appInfo.packageName, "");
        if (!name.isEmpty()) return name;

        try {
            String label = appInfo.loadLabel(context.getPackageManager()).toString();
            if (!label.isEmpty()) return label;
        } catch (Exception ignored) {}
        return appInfo.packageName;
    }
    public void setAppDisplayName(Context context, ApplicationInfo appInfo, String newName) {
        appNameMap.put(appInfo, newName);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(appInfo.packageName, newName).apply();
    }
    public static boolean getAppLaunchOut(String pkg) {
        return (appsToLaunchOut.contains(pkg));
    }

    public static void setAppLaunchOut(String pkg, boolean shouldLaunchOut) {
        if (shouldLaunchOut) appsToLaunchOut.add(pkg);
        else appsToLaunchOut.remove(pkg);
    }

    public static Map<String, String> getAppGroupMap() {
        readValues();
        return appGroupMap;
    }

    public static void setAppGroupMap(Map<String, String> value) {
        appGroupMap = value;
        storeValues();
    }

    public List<ApplicationInfo> getInstalledApps(MainActivity mainActivity, List<String> selected, boolean first, List<ApplicationInfo> myApps) {

        // Get list of installed apps
        Map<String, String> apps = getAppGroupMap();

        Log.v("LauncherStartup", "X1 - Start Groups");

        // Sort into groups
        for (ApplicationInfo app : myApps) {
            if (!appGroupMap.containsKey(app.packageName)) {
                if (!AbstractPlatform.isSupportedApp(app, mainActivity)) appGroupMap.put(app.packageName, GroupsAdapter.UNSUPPORTED_GROUP);
                else {
                    final boolean isVr = AbstractPlatform.isVirtualRealityApp(app, mainActivity);
                    final boolean isWeb = AbstractPlatform.isWebsite(app);
                    appGroupMap.put(app.packageName, getDefaultGroup(isVr, isWeb));
                }
            }
        }

        Log.v("LauncherStartup", "X2 - End Groups");

        // Since this goes over all apps & checks if they're vr, we can safely decide we don't need meta data for them on subsequent launchers
        mainActivity.sharedPreferences.edit().putBoolean(SettingsManager.NEEDS_META_DATA, false).apply();

        // Save changes to app list
        setAppGroupMap(appGroupMap);

        Log.v("LauncherStartup", "X3 - Map Packages");

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


        Log.v("LauncherStartup", "X4 - Mapped Packages");


        // Sort by Package Name

        // Create new list of apps
        ArrayList<ApplicationInfo> sortedApps = new ArrayList<>(appMap.values());
        // Compare on app name (fast)
        Log.v("LauncherStartup", "X5 - Start Sort");
        sortedApps.sort(Comparator.comparing(a -> getAppDisplayName(mainActivity, a).toLowerCase()));
        Log.v("LauncherStartup", "X6 - Finish Sort");


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

    public static String getDefaultGroup(boolean vr, boolean web) {
        final String key = web ? KEY_GROUP_WEB : vr ? KEY_GROUP_2D : KEY_GROUP_VR;
        final String def = web ? DEFAULT_GROUP_WEB : vr ? DEFAULT_GROUP_2D : DEFAULT_GROUP_VR;
        final String group = sharedPreferences.getString(key, def);
        if (!appGroupsSet.contains(group) && !group.equals(GroupsAdapter.UNSUPPORTED_GROUP)) return GroupsAdapter.HIDDEN_GROUP;
        return group;
    }

    public synchronized static void readValues() {
        try {
            Set<String> defaultGroupsSet = new HashSet<>();
            defaultGroupsSet.add(DEFAULT_GROUP_VR);
            defaultGroupsSet.add(DEFAULT_GROUP_2D);
            appGroupsSet = sharedPreferences.getStringSet(KEY_APP_GROUPS, defaultGroupsSet);
            selectedGroupsSet = sharedPreferences.getStringSet(KEY_SELECTED_GROUPS, defaultGroupsSet);
            appsToLaunchOut = sharedPreferences.getStringSet(KEY_LAUNCH_OUT, defaultGroupsSet);

            appGroupMap.clear();

            appGroupsSet.add(GroupsAdapter.HIDDEN_GROUP);
            appGroupsSet.add(GroupsAdapter.UNSUPPORTED_GROUP);
            for (String group : appGroupsSet) {
                Set<String> appListSet = new HashSet<>();
                appListSet = sharedPreferences.getStringSet(KEY_APP_LIST+group, appListSet);

                for (String app : appListSet) {
                    appGroupMap.put(app, group);
                }
            }

            appsToLaunchOut = sharedPreferences.getStringSet(KEY_LAUNCH_OUT, defaultGroupsSet);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private synchronized static void storeValues() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet(KEY_APP_GROUPS, appGroupsSet);
            editor.putStringSet(KEY_SELECTED_GROUPS, selectedGroupsSet);
            editor.putStringSet(KEY_LAUNCH_OUT, appsToLaunchOut);

            Map<String, Set<String>> appListSetMap = new HashMap<>();
            for (String group : appGroupsSet) {
                appListSetMap.put(group, new HashSet<>());
            }
            for (String pkg : appGroupMap.keySet()) {
                Set<String> group = appListSetMap.get(appGroupMap.get(pkg));
                if (group == null) {
                    Log.v("Missing group! Maybe in transit?", pkg);
                    group = appListSetMap.get(getDefaultGroup(false, false));
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

    public static String toTitleCase(String string) {
        if (string == null) return null;
        boolean whiteSpace = true;
        StringBuilder builder = new StringBuilder(string);
        for (int i = 0; i < builder.length(); ++i) {
            char c = builder.charAt(i);
            if (whiteSpace) {
                if (!Character.isWhitespace(c)) {
                    builder.setCharAt(i, Character.toTitleCase(c));
                    whiteSpace = false;
                }
            } else if (Character.isWhitespace(c)) whiteSpace = true;
            else builder.setCharAt(i, Character.toLowerCase(c));
        }
        return builder.toString();
    }
}