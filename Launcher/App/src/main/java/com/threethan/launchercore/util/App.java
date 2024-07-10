package com.threethan.launchercore.util;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.threethan.launcher.helper.AppExt;
import com.threethan.launchercore.Core;
import com.threethan.launchercore.adapter.UtilityApplicationInfo;
import com.threethan.launchercore.lib.StringLib;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** @noinspection unused*/
public abstract class App {
    private static final Map<String, Type> packageTypeCache = new ConcurrentHashMap<>();

    public enum Type {PHONE, VR, TV, PANEL, SHORTCUT, WEB, UNSUPPORTED, UTILITY}

    /**
     * Check if a given package exists and is installed on the device
     * @param packageName Name of the package
     * @return True if the given package exists and could be queried
     */
    public static boolean packageExists(String packageName) {
        try {
            Core.context().getPackageManager().getApplicationInfo(packageName,0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    /**
     * Gets the type of an app
     * @param app ApplicationInfo object for the app
     * @return Type of the app
     */
    public static Type getType(ApplicationInfo app) {
        if (packageTypeCache.containsKey(app.packageName))
            return packageTypeCache.get(app.packageName);
        Type type = getTypeInternal(app);
        packageTypeCache.put(app.packageName, type);
        if (Platform.excludedPackageNames.contains(app.packageName)
                || app.packageName.equals(Core.context().getPackageName())
                || (type == Type.VR || type == Type.PHONE) && Launch.getLaunchIntent(app) == null) {
            packageTypeCache.put(app.packageName, Type.UNSUPPORTED);
            return Type.UNSUPPORTED;
        }
        return type;
    }
    /**
     * Gets the type of an app (which was previously queried)
     * @param packageName Package name of the app
     * @return Type of the app
     */
    public static Type getType(String packageName) {
        if (packageTypeCache.containsKey(packageName))
            return packageTypeCache.get(packageName);
        Log.w(Core.TAG, "Queried package "+packageName+" whose type is not yet known");
        return Type.PHONE;
    }
    private static Type getTypeInternal(ApplicationInfo app) {
        if (app instanceof UtilityApplicationInfo) return Type.UTILITY;
        if (isWebsite(app.packageName)) return Type.WEB;
        if (isShortcut(app.packageName)) return Type.SHORTCUT;
        if (isVrApp(app)) return Type.VR;
        if (isPanelApp(app)) return Type.PANEL;
        if (isTvApp(app)) return Type.TV;
        return Type.PHONE;
    }
    private static boolean isPanelApp(ApplicationInfo app) {
        if (app.packageName.startsWith("systemux://")) return true;
        PackageManager pm = Core.context().getPackageManager();
        Intent panelIntent = new Intent("com.oculus.vrshell.SHELL_MAIN");
        panelIntent.setPackage(app.packageName);
        // Detect panel apps
        if (pm.resolveService(panelIntent, 0) != null) return true;
        // Detect Oculus PWAs
        return app.metaData != null && app.metaData.containsKey("com.oculus.pwa.START_URL");
    }
    private static boolean isVrApp(ApplicationInfo app) {
        if (app.metaData != null)
            if ( app.metaData.containsKey("com.oculus.ossplash")
                || app.metaData.containsKey("com.samsung.android.vr.application.mode")
                || app.metaData.containsKey("com.oculus.intent.category.VR"))
                return true;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory("com.oculus.intent.category.VR");
        intent.setPackage(app.packageName);
        PackageManager pm = Core.context().getPackageManager();
        return pm.resolveActivity(intent, 0) != null;
    }
    private static boolean isTvApp(ApplicationInfo app) {
        PackageManager pm = Core.context().getPackageManager();
        // First check for banner, then check for intent
        if (app.banner != 0) return true;
        Intent tvIntent = new Intent();
        tvIntent.setAction(Intent.CATEGORY_LEANBACK_LAUNCHER);
        tvIntent.setPackage(app.packageName);
        return (tvIntent.resolveActivity(pm) != null);
    }

    public static boolean isWebsite(String packageName) {
        return packageName.contains("//")
                && !packageName.startsWith("systemux:")
                && !isShortcut(packageName);
    }
    public static boolean isShortcut(String packageName) {
        return packageName.startsWith("{\"mActivity\"")
                || packageName.startsWith("json://");
    }

    /** @return The string label for an app */
    public static String getLabel(ApplicationInfo app) {
        if (app instanceof UtilityApplicationInfo uApp) return uApp.getString();
        if (Platform.labelOverrides.containsKey(app.packageName))
            return Platform.labelOverrides.get(app.packageName);
        PackageManager pm = Core.context().getPackageManager();
        return (String) pm.getApplicationLabel(app);
    }
    /** @return The string label for an app, processed for sorting */
    public static String getSortableAppLabel(ApplicationInfo app) {
        return StringLib.forSort(getLabel(app));
    }
    /** @noinspection unused*/
    public static boolean isBanner(ApplicationInfo app) {
        return AppExt.typeIsBanner(getType(app));
    }
    public static Boolean showsName(ApplicationInfo app) {
        return AppExt.showsName(app);
    }

    /** @return Application info for the given packageName (*without metadata) */
    public static ApplicationInfo infoFor(String packageName) {
        try {
            return Core.context().getPackageManager().getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(Core.TAG, "Could not find application info for "+packageName);
            ApplicationInfo info = new ApplicationInfo();
            info.packageName = packageName;
            return info;
        }
    }
}
