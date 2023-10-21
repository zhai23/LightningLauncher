package com.threethan.launcher.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.threethan.launcher.R;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.support.SettingsManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*
    App

    This abstract class is provides info about applications, and helper functions for non-launching
    intents (info, uninstall).

    Functions prefixed with "check" check a property of an app using its metadata
    Functions prefixed with "is" are wrappers around "check" functions which cache values
 */



public abstract class App {
    static Map<Type, Set<String>> categoryIncludedApps = new ConcurrentHashMap<>();
    static Map<Type, Set<String>> categoryExcludedApps = new ConcurrentHashMap<>();

    public enum Type {
        TYPE_PHONE, TYPE_VR, TYPE_TV, TYPE_PANEL, TYPE_WEB, TYPE_SUPPORTED, TYPE_UNSUPPORTED
    }
    private static boolean checkVirtualReality(ApplicationInfo applicationInfo) {
        if (applicationInfo.metaData == null) return false;
        for (String key : applicationInfo.metaData.keySet()) {
            if (key.startsWith("notch.config")) return true;
            if (key.contains("com.oculus.supportedDevices")) return true;
            if (key.contains("vr.application.mode")) return true;
        }
        return false;
    }
    private static boolean checkAndroidTv
            (ApplicationInfo applicationInfo, LauncherActivity launcherActivity) {
        PackageManager pm = launcherActivity.getPackageManager();

        // First check for banner
        if (applicationInfo.banner != 0) return true;
        // Then check for intent
        Intent tvIntent = new Intent();
        tvIntent.setAction(Intent.CATEGORY_LEANBACK_LAUNCHER);
        tvIntent.setPackage(applicationInfo.packageName);
        return (tvIntent.resolveActivity(pm) != null);

    }
    protected synchronized static boolean isAppOfType
            (ApplicationInfo applicationInfo, App.Type appType, LauncherActivity launcherActivity) {

            final SharedPreferences sharedPreferences = launcherActivity.sharedPreferences;
            final SharedPreferences.Editor sharedPreferenceEditor = launcherActivity.sharedPreferenceEditor;

            if (!categoryIncludedApps.containsKey(appType) ||
                    Objects.requireNonNull(categoryIncludedApps.get(appType)).isEmpty()) {
                categoryIncludedApps.put(appType, Collections.synchronizedSet(new HashSet<>()));
                categoryExcludedApps.put(appType, Collections.synchronizedSet(new HashSet<>()));
                Objects.requireNonNull(categoryIncludedApps.get(appType))
                        .addAll(sharedPreferences.getStringSet(Settings.KEY_INCLUDED_SET + appType
                                , new HashSet<>()));
                Objects.requireNonNull(categoryExcludedApps.get(appType))
                        .addAll(sharedPreferences.getStringSet(Settings.KEY_EXCLUDED_SET + appType
                                , new HashSet<>()));
            } else {
                if (Objects.requireNonNull(categoryIncludedApps.get(appType))
                        .contains(applicationInfo.packageName)) return true;
                if (Objects.requireNonNull(categoryExcludedApps.get(appType))
                        .contains(applicationInfo.packageName)) return false;
            }
            boolean isType = false;

            // this function shouldn't be called until checking higher priorities first
            switch (appType) {
                case TYPE_VR:
                    isType = checkVirtualReality(applicationInfo);
                    break;
                case TYPE_TV:
                    isType = checkAndroidTv(applicationInfo, launcherActivity);
                    break;
                case TYPE_PANEL:
                    isType = checkPanelApp(applicationInfo, launcherActivity);
                    break;
                case TYPE_WEB:
                    isType = isWebsite(applicationInfo);
                    break;
                case TYPE_PHONE:
                    isType = true;
                    break;
                case TYPE_SUPPORTED:
                    isType = checkSupported(applicationInfo, launcherActivity);
                    break;
            }

            if (isType) {
                Objects.requireNonNull(categoryIncludedApps.get(appType))
                        .add(applicationInfo.packageName);
                sharedPreferenceEditor.putStringSet(Settings.KEY_INCLUDED_SET + appType,
                        categoryIncludedApps.get(appType));
            } else {
                Objects.requireNonNull(categoryExcludedApps.get(appType))
                        .add(applicationInfo.packageName);
                sharedPreferenceEditor.putStringSet(Settings.KEY_EXCLUDED_SET + appType,
                        categoryIncludedApps.get(appType));
            }
            sharedPreferenceEditor.apply();

            return isType;
    }


    private static boolean checkPanelApp
            (ApplicationInfo applicationInfo, LauncherActivity launcherActivity) {
        //noinspection SuspiciousMethodCalls
        if (AppData.getPanelAppList().contains(applicationInfo)) return true;

        if (AppData.AUTO_DETECT_PANEL_APPS) {
            PackageManager pm = launcherActivity.getPackageManager();
            Intent panelIntent = new Intent("com.oculus.vrshell.SHELL_MAIN");
            panelIntent.setPackage(applicationInfo.packageName);
            return (pm.resolveService(panelIntent, 0) != null);
        } else return false;
    }

    synchronized public static boolean isSupported(ApplicationInfo app, LauncherActivity launcherActivity) {
        return isAppOfType(app, Type.TYPE_SUPPORTED, launcherActivity);
    }
    private static String[] unsupportedPrefixes;
    private static boolean checkSupported(ApplicationInfo app, LauncherActivity launcherActivity) {

        if (isWebsite(app)) return true;

        if (app.metaData != null) {
            boolean isVr = isAppOfType(app, Type.TYPE_VR, launcherActivity);
            if (!isVr && app.metaData.keySet().contains("com.oculus.environmentVersion")) return false;
        }
        if (Launch.getLaunchIntent(launcherActivity, app) == null) return false;

        if (unsupportedPrefixes == null) unsupportedPrefixes = launcherActivity.getResources().getStringArray(R.array.unsupported_app_prefixes);
        for (String prefix : unsupportedPrefixes)
            if (app.packageName.startsWith(prefix))
                return false;
        return true;
    }

    public static boolean isBanner(LauncherActivity launcherActivity, ApplicationInfo applicationInfo) {
        return typeIsBanner(getType(launcherActivity, applicationInfo));
    }
    /** @noinspection SuspiciousMethodCalls*/
    public static boolean isWebsite(ApplicationInfo applicationInfo) {
        return (isWebsite(applicationInfo.packageName) &&
                !AppData.getPanelAppList().contains(applicationInfo));
    }
    public static boolean isWebsite(String packageName) {
        return (packageName.contains("//"));
    }

    // Invalidate the values caches for isBlank functions
    public static void invalidateCaches(LauncherActivity launcherActivity) {
        for (App.Type type : Platform.getSupportedAppTypes(launcherActivity))
            launcherActivity.sharedPreferenceEditor
                    .remove(Settings.KEY_INCLUDED_SET + type)
                    .remove(Settings.KEY_EXCLUDED_SET + type);

        launcherActivity.sharedPreferenceEditor.apply();
    }
    // Opens the app info settings pane
    public static void openInfo(Context context, String packageName) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        context.startActivity(intent);
    }
    // Requests to uninstall the app
    public static void uninstall(LauncherActivity launcher, String packageName) {
        if (App.isWebsite(packageName)) {
            Set<String> webApps = launcher.sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
            webApps = new HashSet<>(webApps); // Copy since we're not supposed to modify directly
            if (launcher.browserService != null) launcher.browserService.killWebView(packageName); // Kill web view if running
            webApps.remove(packageName);
            launcher.sharedPreferenceEditor
                    .putString(packageName, null) // set display name
                    .putStringSet(Settings.KEY_WEBSITE_LIST, webApps);
            launcher.refreshAppDisplayListsAll();
        } else {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse("package:" + packageName));
            launcher.startActivity(intent);
        }
    }

    public static App.Type getType(LauncherActivity launcherActivity, ApplicationInfo app) {
        for (Type type : Platform.getSupportedAppTypes(launcherActivity)) {
            if (isAppOfType(app, type, launcherActivity)) return type;
        }
        return Type.TYPE_UNSUPPORTED;
    }

    public static String getTypeString(Activity a, Type type) {
        switch (type) {
            case TYPE_PHONE:
                return a.getString(R.string.apps_phone);
            case TYPE_VR:
                return a.getString(R.string.apps_vr);
            case TYPE_TV:
                return a.getString(R.string.apps_tv);
            case TYPE_WEB:
                return a.getString(R.string.apps_web);
            case TYPE_PANEL:
                return a.getString(R.string.apps_panel);
            default:
                return "Invalid type";
        }
    }
    public static String getDefaultGroupFor(App.Type type) {
        return SettingsManager.getDefaultGroupFor(type);
    }
    public static boolean typeIsBanner(App.Type type) {
        return SettingsManager.isTypeBanner(type);
    }

}
