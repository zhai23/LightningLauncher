package com.threethan.launcher.helper;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;

import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.StringLib;
import com.threethan.launcher.support.SettingsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
    Platform

    This abstract class stores lists of apps.
    It also provides a few helper functions for adding websites, and decides if we're in VR
 */

public abstract class Platform {
    public static List<ApplicationInfo> installedApps;
    public static List<ApplicationInfo> appListBanner;
    public static List<ApplicationInfo> appListSquare;
    public static int changeIndex = 0; //Used to track changes, specifically adding websites
    public static void clearPackageLists(LauncherActivity launcherActivity) {
        App.invalidateCaches(launcherActivity);
        changeIndex ++;
    }
    public static String findWebsite(SharedPreferences sharedPreferences, String url) {
        url = StringLib.fixUrl(url);

        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        if (!webApps.contains(url)) return null;
        else return SettingsManager.getAppGroupMap().get(url);
    }
    public static void addWebsite(SharedPreferences sharedPreferences, String url) {
        url = StringLib.fixUrl(url);

        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        webApps = new HashSet<>(webApps); // Copy since we're not supposed to modify directly
        webApps.add(url);

        sharedPreferences.edit()
                .putStringSet(Settings.KEY_WEBSITE_LIST, webApps)
                .apply();

        changeIndex ++;
    }

    protected static Boolean isTv;
    public static boolean isVr(Activity activity) {
        // Quest reports itself as UI_MODE_NORMAL
        return !isTv(activity);
    }
    public static boolean isQuest(Activity activity) {
        // Quest reports itself as UI_MODE_NORMAL
        return !isTv(activity);
    }
    public static boolean isTv(Activity activity) {
        if (isTv != null) return isTv;
        UiModeManager uiModeManager = (UiModeManager) activity.getSystemService(Context.UI_MODE_SERVICE);
        isTv = uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
        return isTv;
    }

    // Get a list of valid app types depending on platform
    private static List<App.Type> cachedSupportedAppTypes;
    public static List<App.Type> getSupportedAppTypes(Activity activity) {
        if (cachedSupportedAppTypes != null) return  cachedSupportedAppTypes;

        final List<App.Type> validTypes = new ArrayList<>();

        // These must be in order of priority
        if (Platform.isQuest(activity)) validTypes.add(App.Type.TYPE_PANEL);

        if (Platform.isTv(activity)) validTypes.add(App.Type.TYPE_TV);
        if (Platform.isVr(activity)) validTypes.add(App.Type.TYPE_VR);

        validTypes.add(App.Type.TYPE_WEB);
        validTypes.add(App.Type.TYPE_PHONE);

        cachedSupportedAppTypes = validTypes;
        return validTypes;
    }
}