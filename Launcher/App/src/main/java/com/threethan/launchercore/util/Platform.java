package com.threethan.launchercore.util;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;

import com.threethan.launchercore.Core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @noinspection unused*/
public abstract class Platform {
    static {
        // Get package info on startup, so we can get info with only the package name
        Core.whenReady(() -> new Thread(() -> {
            for (ApplicationInfo app : Platform.listInstalledApps()) App.getType(app);
        }).start());
    }
    /**
     * @return True if running on a Meta Quest device
     */
    public static boolean isQuest() {
        return App.packageExists("com.oculus.vrshell");
    }
    /**
     * @return True if running on an Android TV device
     */
    public static boolean isTv() {
        return (((UiModeManager) Core.context().getSystemService(Context.UI_MODE_SERVICE))
                .getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION);
    }
    /**
     * @return True if running on a VR headset (*currently only Meta Quest)
     */
    public static boolean isVr() {
        return isQuest();
    }
    /**
     * Gets a list of all installed apps
     * @return ApplicationInfo for each installed app (*sans exclusions)
     */
    public static List<ApplicationInfo> listInstalledApps() {
        PackageManager pm = Core.context().getPackageManager();
        @SuppressLint("QueryPermissionsNeeded")
        List<ApplicationInfo> installedApps
                = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        if (Platform.isQuest()) for (String systemUxPanelApp : systemUxPanelApps) {
            ApplicationInfo panelAppInfo = new ApplicationInfo();
            panelAppInfo.packageName = systemUxPanelApp;
            installedApps.add(panelAppInfo);
        }

        return installedApps;
    }

    private static final Set<String> systemUxPanelApps = Set.of(
            "systemux://settings",
            "systemux://aui-social-v2",
            "systemux://events",
            "systemux://file-manager");
    public static final Map<String, String> labelOverrides = new HashMap<>();
    static {
        labelOverrides.put("systemux://settings", "Quest Settings");
        labelOverrides.put("systemux://aui-social-v2", "People");
        labelOverrides.put("systemux://events", "Events");
        labelOverrides.put("systemux://file-manager", "File Manager");
        labelOverrides.put("com.oculus.gamingactivity", "Meta Quest Scoreboards");
        labelOverrides.put("com.oculus.helpcenter", "Meta Quest Guide");
        labelOverrides.put("com.android.settings", "Android Settings");
    }
    static final Set<String> excludedPackageNames = Set.of(
            "com.oculus.vrshell",
            "com.oculus.shellenv",
            "com.oculus.integrity",
            "com.oculus.metacam",
            "com.oculus.tv",
            "com.oculus.socialplatform",
            "com.oculus.systemactivities",
            "com.oculus.systempermissions",
            "com.oculus.systemutilities",
            "com.oculus.systemresource",
            "com.oculus.extrapermissions",
            "com.oculus.mobile_mrc_setup",
            "com.oculus.os.chargecontrol",
            "com.oculus.os.clearactivity",
            "com.oculus.os.voidactivity",
            "com.oculus.os.qrcodereader",
            "com.oculus.AccountsCenter.pwa",
            "com.oculus.identitymanage",
            "com.oculus.voidactivity",
            "com.oculus.xrstreamingclient",
            "com.oculus.vrprivacycheckup",
            "com.oculus.panelapp.settings",
            "com.oculus.panelapp.kiosk",
            "com.meta.handseducationmodule",
            "com.oculus.avatareditor",
            "com.oculus.accountscenter",
            "com.oculus.identitymanagement.service",
            "com.meta.AccountsCenter.pwa",
            "com.oculus.firsttimenux",
            "com.oculus.guidebook",
            "com.oculus.vrshell.desktop",
            "com.oculus.systemux"
    );

    /**
     * Opens the application info settings page for a given package
     * @param packageName Package name of the package
     */
    public static void openPackageInfo(String packageName) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" +
                packageName.replaceFirst("systemux://", "")));
        Core.context().startActivity(intent);
    }

    /**
     * Request to uninstall a package
     * @param packageName Package name of the package
     */
    public static void uninstall(String packageName) {
        if (App.isWebsite(packageName) || App.isShortcut(packageName)) {
            throw new RuntimeException("Uninstalling websites/shortcuts is not implemented!");
        } else {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse("package:" + packageName));
            Core.context().startActivity(intent);
        }
    }

    /**
     * Gets a system property using reflection
     * @param key System property key
     * @param def Default value
     * @return Def if property doesn't exist or reflection failed, else the value of the property
     */
    public static String getSystemProperty(String key, String def) {
        try {
            @SuppressLint("PrivateApi") Class<?> systemProperties
                    = Class.forName("android.os.SystemProperties");
            Method getMethod = systemProperties.getMethod("get",
                    String.class, String.class);
            return (String) getMethod.invoke(null, key, def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Gets the version of Meta's VrOs
     * @return -1 if failed, else VrOs version
     */
    public static int getVrOsVersion() {
        try {
            return Integer.parseInt(getSystemProperty("ro.vros.build.version", "-1"));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Returns true if the device supports the new meta quest multi-window
     * @return True if VrOs >= 69
     */
    public static boolean supportsNewVrOsMultiWindow() {
        return getVrOsVersion() >= 69;
    }
}
