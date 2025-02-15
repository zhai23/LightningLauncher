package com.threethan.launcher.helper;

import android.app.Activity;
import android.content.pm.ApplicationInfo;

import com.threethan.launcher.R;
import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launcher.activity.dialog.BasicDialog;
import com.threethan.launcher.activity.support.DataStoreEditor;
import com.threethan.launcher.activity.support.SettingsManager;
import com.threethan.launcher.data.Settings;
import com.threethan.launchercore.Core;
import com.threethan.launchercore.adapter.UtilityApplicationInfo;
import com.threethan.launchercore.util.App;
import com.threethan.launchercore.util.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *     This abstract class stores lists of apps.
 *     It also provides a few helper functions for adding websites, and decides if we're in VR
 */
public abstract class PlatformExt {
    public static List<ApplicationInfo> installedApps;
    public static Set<ApplicationInfo> apps = Collections.synchronizedSet(new HashSet<>());
    public static int changeIndex = 0; //Used to track changes, specifically adding websites

    public static final Map<String, String> infoOverrides = new HashMap<>();
    static {
        infoOverrides.put("systemux://settings", "com.oculus.panelapp.settings");
        infoOverrides.put("systemux://aui-social-v2", "com.oculus.socialplatform");
        infoOverrides.put("systemux://events", "com.oculus.explore");
    }
    /**
     * Finds an existing website on the launcher
     * @return Null if not found, else name of group website is in
     */
    public static String findWebsite(DataStoreEditor dataStoreEditor, String url) {
        if (!url.contains("://")) url = "https://" + url;

        Set<String> webApps = dataStoreEditor.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        if (!webApps.contains(url)) return null;
        else return SettingsManager.getAppGroupMap().get(url);
    }

    /**
     * Add a website to the launcher
     * @param dataStoreEditor DataStoreEditor instance to use
     * @param url Url to launch
     */
    public static void addWebsite(DataStoreEditor dataStoreEditor, String url) {
        addWebsite(dataStoreEditor, url, null);
    }

    /**
     * Add a website to the launcher
     * @param dataStoreEditor DataStoreEditor instance to use
     * @param url Url to launch
     * @param name Name for the website on the launcher
     */
    public static String addWebsite(DataStoreEditor dataStoreEditor, String url, String name) {
        if (!url.contains("://")) url = "https://" + url;

        Set<String> webApps = dataStoreEditor.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        webApps = new HashSet<>(webApps); // Copy since we're not supposed to modify directly
        webApps.add(url);

        dataStoreEditor.putStringSet(Settings.KEY_WEBSITE_LIST, webApps);
        if (name != null) dataStoreEditor.putString(url, name);

        changeIndex ++;
        return url;
    }

    public static final String BROWSER_PACKAGE = "com.threethan.browser";
    /** Returns true if Lightning Browser is installed */
    public static boolean hasBrowser(Activity activity) {
        return AppExt.doesPackageExist(activity, BROWSER_PACKAGE);
    }

    // Get a list of valid app types depending on platform
    private static List<App.Type> cachedSupportedAppTypes;

    /**
     * Returns a list of app types which are supported.
     * <p>
     * The Quest ignored TV apps, while TVs ignore VR apps.
     * <p>
     * Note that "unsupported" apps may end up being supported, but put into a different category.
     *
     * @return Types of apps which are supported on this device
     */
    public static List<App.Type> getSupportedAppTypes() {
        if (cachedSupportedAppTypes != null) return cachedSupportedAppTypes;

        final List<App.Type> validTypes = new ArrayList<>();


        // These must be in order of priority
        if (Platform.isQuest()) validTypes.add(App.Type.PANEL);

        if (Platform.isTv()) validTypes.add(App.Type.TV);
        if (Platform.isVr()) validTypes.add(App.Type.VR);

        validTypes.add(App.Type.WEB);
        validTypes.add(App.Type.PHONE);

        cachedSupportedAppTypes = validTypes;
        return validTypes;
    }

    public static Set<ApplicationInfo> listInstalledApps(LauncherActivity launcherActivity) {
        apps.clear();
        apps.addAll(Platform.listInstalledApps());
        // Add web apps
        Set<String> webApps = launcherActivity.dataStoreEditor
                .getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        // LL Specific
        for (String url:webApps) {
            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = url;
            apps.add(applicationInfo);
        }
        // Utility apps
        if (Platform.getVrOsVersion() >= 74) apps.add(ApkInstallerUtilityApplication.getInstance());
        return apps;
    }

    /** @return True, if we should use the new multitasking behaviour */
    public static boolean useNewVrOsMultiWindow() {
        if (!Platform.supportsNewVrOsMultiWindow()) return false;
        return Compat.getDataStore()
                .getBoolean(Settings.KEY_NEW_MULTITASK, Settings.DEFAULT_NEW_MULTITASK);
    }

    /** @return True, if we should use the new launching behaviour */
    public static boolean useVrOsChainLaunch() {
        if (!Platform.supportsVrOsChainLaunch()) return false;
        return Compat.getDataStore()
                .getBoolean(Settings.KEY_ALLOW_CHAIN_LAUNCH, Settings.DEFAULT_ALLOW_CHAIN_LAUNCH);
    }

    /**
     * An instance of UtilityApplicationInfo which provides an easy way to install APKs on v74+
     */
    public static class ApkInstallerUtilityApplication extends UtilityApplicationInfo {
        private ApkInstallerUtilityApplication() {
            super("builtin://apk-install", R.drawable.ic_installer);
        }
        private static ApkInstallerUtilityApplication instance;
        public static ApkInstallerUtilityApplication getInstance() {
            if (instance== null) instance = new ApkInstallerUtilityApplication();
            return instance;
        }

        public void launch() {
            BasicDialog.toast(Core.context().getString(R.string.apk_installer_tip));
            if (LauncherActivity.getForegroundInstance() != null)
                LauncherActivity.getForegroundInstance()
                        .showFilePicker(LauncherActivity.FilePickerTarget.APK);
        }
    }
}