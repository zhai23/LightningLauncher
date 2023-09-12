package com.threethan.launcher.helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.Uri;

import com.threethan.launcher.R;
import com.threethan.launcher.launcher.LauncherActivity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class App {
    static Set<String> setVr = new HashSet<>();
    static Set<String> set2d = new HashSet<>();
    public static boolean isVirtualReality(ApplicationInfo applicationInfo, LauncherActivity launcherActivity) {
        final SharedPreferences sharedPreferences = launcherActivity.sharedPreferences;
        final SharedPreferences.Editor sharedPreferenceEditor = launcherActivity.sharedPreferenceEditor;
        if (setVr.isEmpty()) {
            setVr = sharedPreferences.getStringSet(Settings.KEY_VR_SET, setVr);
            set2d = sharedPreferences.getStringSet(Settings.KEY_2D_SET, set2d);
        }
        if (setVr.contains(applicationInfo.packageName)) return true;
        if (set2d.contains(applicationInfo.packageName)) return false;

        if (
                checkVirtualReality(applicationInfo)) {
            setVr.add(applicationInfo.packageName);
            launcherActivity.post(() -> sharedPreferenceEditor.putStringSet(Settings.KEY_VR_SET, setVr));
            launcherActivity.postSharedPreferenceApply();
            return true;
        } else {
            set2d.add(applicationInfo.packageName);
            launcherActivity.post(() -> sharedPreferenceEditor.putStringSet(Settings.KEY_2D_SET, set2d));
            launcherActivity.postSharedPreferenceApply();
            return false;
        }
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

    static HashSet<String> setSupported = new HashSet<>();
    static HashSet<String> setUnsupported = new HashSet<>();
    public static boolean isSupported(ApplicationInfo app, LauncherActivity launcherActivity) {
        final SharedPreferences sharedPreferences = launcherActivity.sharedPreferences;
        final SharedPreferences.Editor sharedPreferenceEditor = launcherActivity.sharedPreferenceEditor;
        if (setSupported.isEmpty()) {
            sharedPreferences.getStringSet(Settings.KEY_SUPPORTED_SET, setSupported);
            sharedPreferences.getStringSet(Settings.KEY_UNSUPPORTED_SET, setUnsupported);
            setUnsupported.add(launcherActivity.getPackageName());
            setSupported.addAll(sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet()));
        }

        if (setSupported.contains(app.packageName)) return true;
        if (setUnsupported.contains(app.packageName)) return false;

        if (checkSupported(app, launcherActivity)) {
            setSupported.add(app.packageName);
            sharedPreferenceEditor.putStringSet(Settings.KEY_SUPPORTED_SET, setSupported);
            return true;
        } else {
            setUnsupported.add(app.packageName);
            sharedPreferenceEditor.putStringSet(Settings.KEY_UNSUPPORTED_SET, setUnsupported);
            return false;
        }
    }
    private static String[] unsupportedPrefixes;
    private static boolean checkSupported(ApplicationInfo applicationInfo, LauncherActivity launcherActivity) {
        if (unsupportedPrefixes == null) unsupportedPrefixes = launcherActivity.getResources().getStringArray(R.array.unsupported_app_prefixes);

        if (applicationInfo.metaData != null) {
            boolean isVr = isVirtualReality(applicationInfo, launcherActivity);
            if (!isVr && applicationInfo.metaData.keySet().contains("com.oculus.environmentVersion")) return false;
        }
        if (launcherActivity.getPackageManager().getLaunchIntentForPackage(applicationInfo.packageName) == null)
            return isWebsite(applicationInfo);

        for (String prefix : unsupportedPrefixes)
            if (applicationInfo.packageName.startsWith(prefix))
                return false;
        return true;
    }

    public static boolean isBanner(ApplicationInfo applicationInfo, LauncherActivity launcherActivity) {
        final boolean isVr = isVirtualReality(applicationInfo, launcherActivity);
        final SharedPreferences sharedPreferences = launcherActivity.sharedPreferences;
        if (isVr)  return sharedPreferences.getBoolean(Settings.KEY_WIDE_VR , Settings.DEFAULT_WIDE_VR );
        final boolean isWeb = isWebsite(applicationInfo);
        if (isWeb) return sharedPreferences.getBoolean(Settings.KEY_WIDE_WEB, Settings.DEFAULT_WIDE_WEB);
        else       return sharedPreferences.getBoolean(Settings.KEY_WIDE_2D , Settings.DEFAULT_WIDE_2D );
    }
    public static boolean isWebsite(ApplicationInfo applicationInfo) {
        return (isWebsite(applicationInfo.packageName));
    }
    public static boolean isWebsite(String packageName) {
        return (packageName.contains("//"));
    }

    public static void openInfo(Context context, String packageName) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        context.startActivity(intent);
    }
    public static void uninstall(LauncherActivity launcher, String packageName) {
        if (App.isWebsite(packageName)) {
            Set<String> webApps = launcher.sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
            webApps = new HashSet<>(webApps); // Copy since we're not supposed to modify directly
            launcher.wService.killWebView(packageName); // Kill web view if running
            webApps.remove(packageName);
            launcher.sharedPreferenceEditor
                    .putString(packageName, null) // set display name
                    .putStringSet(Settings.KEY_WEBSITE_LIST, webApps);
            launcher.refreshApps();
            launcher.refresh();
        } else {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse("package:" + packageName));
            launcher.startActivity(intent);
        }
    }
}
