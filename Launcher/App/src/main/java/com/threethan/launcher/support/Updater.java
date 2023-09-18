package com.threethan.launcher.support;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.threethan.launcher.R;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.DataLib;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

// Credit to Basti for update checking code
public class Updater {
    private static final String UPDATE_URL = "https://api.github.com/repos/threethan/LightningLauncher/releases/latest";
    private static final String TEMPLATE_URL = "https://github.com/threethan/LightningLauncher/releases/download/%s/%s.apk";
    private static final String NAME_MAIN = "LightningLauncher";
    private static final String KEY_SAVED_VERSION_CODE = "KEY_SAVED_VERSION_CODE";
    public static final String TAG_MESSENGER_SHORTCUT = "TAG_MESSENGER_SHORTCUT";
    public static final String TAG_LIBRARY_SHORTCUT = "TAG_LIBRARY_SHORTCUT";
    public static final String TAG_EXPLORE_SHORTCUT = "TAG_EXPLORE_SHORTCUT";
    public static final String ADDON_RELEASE_TAG = "addons";
    public static final String UPDATE_DIR = "/Content/Updates/";
    public static final Addon[] addons = {
            new Addon(TAG_MESSENGER_SHORTCUT, "MessengerRedirect", "com.facebook.orca", "5.1.0", false),
            new Addon(TAG_LIBRARY_SHORTCUT, "LibraryShortcutService", "com.threethan.launcher.service.library", "5.1.0", true),
            new Addon(TAG_EXPLORE_SHORTCUT, "ExploreShortcutService", "com.threethan.launcher.service.explore", "5.1.0", true),
    };
    private static final String TAG = "LightningLauncher Updater";
    private final RequestQueue requestQueue;
    private final PackageManager packageManager;
    private final Activity activity;
    private static final String KEY_IGNORED_UPDATE_VERSION = "UPDATER_IGNORED_UPDATE_VERSION";
    private static final String KEY_UPDATE_AVAILABLE = "UPDATER_UPDATE_AVAILABLE";
    public static final int STATE_NOT_INSTALLED = 0;
    public static final int STATE_ACTIVE = 1;
    public static final int STATE_HAS_UPDATE = 2;
    public static final int STATE_INACTIVE = 3;
    String latestVersionTag;

    public Updater(Activity activity) {
        this.activity = activity;
        this.requestQueue = Volley.newRequestQueue(activity);
        this.packageManager = activity.getPackageManager();
    }
    public Updater(LauncherActivity activity) {
        this.activity = activity;
        this.requestQueue = Volley.newRequestQueue(activity);
        this.packageManager = activity.getPackageManager();

        // Clear update files if just updated
        try {
            String savedVersion = activity.sharedPreferences.getString(KEY_SAVED_VERSION_CODE, null);
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    activity.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (!packageInfo.versionName.equals(savedVersion)) {
                Log.i(TAG, String.format("Update was detected! Old update files will be cleared..." +
                        " (%s)->(%s)", packageInfo.versionName, savedVersion));
                DataLib.delete(UPDATE_DIR);
                activity.sharedPreferenceEditor.putString(KEY_SAVED_VERSION_CODE, packageInfo.versionName).apply();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void checkForAppUpdate() {
        checkLatestVersion(this::storeLatestVersionAndPrompt);
    }
    @Nullable
    private Addon getAddon(String tag) {
        for (Addon a:addons)
            if (a.match(tag))
                return a;
        return null;
    }
    public int getAddonState(String tag) {
        Addon addon = getAddon(tag);
        if (addon == null) return STATE_NOT_INSTALLED;
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(
                    addon.packageName, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            return STATE_NOT_INSTALLED;
        }
        if (!packageInfo.versionName.equals(addon.latestVersion)) return STATE_HAS_UPDATE;
        if (addon.accessibilityService) {
            AccessibilityManager am = (AccessibilityManager) activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
            List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

            for (AccessibilityServiceInfo enabledService : enabledServices) {
                ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
                if (enabledServiceInfo.packageName.equals(addon.packageName))
                    return STATE_ACTIVE;
            }
            return STATE_INACTIVE;
        } else return STATE_ACTIVE;
    }
    public void uninstallAddon(Activity activity, String tag) {
        Addon addon = getAddon(tag);
        if (addon == null) return;
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + addon.packageName));
        activity.startActivity(intent);
    }
    public void installAddon(String tag) {
        Addon addon = getAddon(tag);
        if (addon == null) return;
        Log.v(TAG, "Attempting to install addon "+tag);
        attempts = 3;
        downloadUpdate(addon.downloadName, ADDON_RELEASE_TAG);
    }

    public void updateAppEvenIfSkipped() {
        getSharedPreferences().edit().remove(KEY_IGNORED_UPDATE_VERSION).apply();
        attempts = 6;
        downloadUpdate(NAME_MAIN);
    }
    protected void storeLatestVersionAndPrompt(String tagName) {
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(
                    activity.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }

        boolean appHasUpdate = !(packageInfo.versionName).equals(tagName);

        if (appHasUpdate) {
            Log.v(TAG, "New version available!");
            getSharedPreferences().edit().putBoolean(KEY_UPDATE_AVAILABLE, true).apply();
            if (tagName.equals(getSharedPreferences().getString(KEY_IGNORED_UPDATE_VERSION, null))) return;
            showAppUpdateDialog(packageInfo.versionName, tagName);
        } else {
            Log.i(TAG, "App is up to date :)");
            getSharedPreferences().edit().putBoolean(KEY_UPDATE_AVAILABLE, false).apply();
        }
    }

    public void checkLatestVersion(Response.Listener<String> callback) {
        StringRequest updateRequest = new StringRequest(
                Request.Method.GET, UPDATE_URL,
                (response -> handleUpdateResponse(response, callback)),
                (this::handleUpdateError));
        requestQueue.add(updateRequest);
        requestQueue.start();
    }

    private void handleUpdateResponse(String response, @Nullable Response.Listener<String> callback) {
        try {
            JSONObject latestReleaseJson = new JSONObject(response);
            String tagName = latestReleaseJson.getString("tag_name");
            if (callback != null) callback.onResponse(tagName);
            latestVersionTag = tagName;
        } catch (JSONException e) {
            Log.w(TAG, "Received invalid JSON", e);
        }
    }
    private void handleUpdateError(VolleyError error) {
        Log.w(TAG, "Couldn't get update info", error);
    }

    private void showAppUpdateDialog(String curName, String newName) {
        try {
            attempts = 4;
            AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(activity);
            updateDialogBuilder.setTitle(R.string.update_title);
            updateDialogBuilder.setMessage(activity.getString(R.string.update_content, curName, newName));
            updateDialogBuilder.setPositiveButton(R.string.update_button, (dialog, which) -> downloadUpdate(NAME_MAIN));
            updateDialogBuilder.setNegativeButton(R.string.update_skip_button, (dialog, which) -> skipUpdate(newName));
            AlertDialog updateAlertDialog = updateDialogBuilder.create();
            updateAlertDialog.show();
        } catch (Exception ignored) {}
    }
    private SharedPreferences getSharedPreferences() {
        // noinspection deprecation
        return PreferenceManager.getDefaultSharedPreferences(activity);
    }
    public static boolean isUpdateAvailable(Context context) {
        //noinspection deprecation
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_UPDATE_AVAILABLE, false);
    }
    public void skipUpdate(String versionTag) {
        AlertDialog.Builder skipDialogBuilder = new AlertDialog.Builder(activity);
        skipDialogBuilder.setTitle(activity.getString(R.string.update_skip_title, versionTag));
        skipDialogBuilder.setMessage(R.string.update_skip_content);
        skipDialogBuilder.setPositiveButton(R.string.update_skip_confirm_button, (dialog, i) -> {
            getSharedPreferences().edit().putString(KEY_IGNORED_UPDATE_VERSION, versionTag).apply();
            dialog.dismiss();
        });
        skipDialogBuilder.setNegativeButton(R.string.update_skip_cancel_button, ((dialog, i) -> dialog.dismiss()));
        AlertDialog skipAlertDialog = skipDialogBuilder.create();
        skipAlertDialog.show();
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag") // Can't be fixed on this android API
    public void downloadUpdate(String apkName) {
        downloadSucceeded = false;
        if (latestVersionTag == null) {
            Log.w(TAG, "Latest version tag was null!" +
                    "ill try to get it again, but this download will probably fail");
            checkLatestVersion(response -> handleUpdateResponse(response, null));
        }
        downloadUpdate(apkName, latestVersionTag);
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag") // Can't be fixed on this API version
    private void downloadUpdate(String apkName, String tagName) {
        downloadSucceeded = false;
        downloadingName = apkName;
        downloadingTag = tagName;


        String url = String.format(TEMPLATE_URL, downloadingTag, apkName);
        Log.v(TAG, "Downloading from url "+url);
        DownloadManager.Request request1 = new DownloadManager.Request(Uri.parse(url));
        request1.setDescription("Downloading Update");   //appears the same in Notification bar while downloading
        request1.setTitle("LightningLauncher Auto-Updater");

        request1.setDestinationInExternalFilesDir(activity, UPDATE_DIR, apkName+latestVersionTag+".apk");

        DownloadManager manager1 = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);

        AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(activity);
        updateDialogBuilder.setTitle(R.string.update_downloading_title);
        updateDialogBuilder.setMessage(R.string.update_downloading_content);
        updateDialogBuilder.setNegativeButton(R.string.update_hide_button, (dialog, which) -> dialog.cancel());
        updateAlertDialog = updateDialogBuilder.create();
        updateAlertDialog.show();

        activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        manager1.enqueue(request1);
    }
    String downloadingTag;
    String downloadingName;
    boolean downloadSucceeded;
    AlertDialog updateAlertDialog;
    BroadcastReceiver onComplete=new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (updateAlertDialog != null) updateAlertDialog.dismiss();
            installUpdate(downloadingName);
        }
    };

    static int attempts = 8;
    public void installUpdate(String apkName) {
        File p = activity.getApplicationContext().getExternalFilesDir(UPDATE_DIR);
        File f = new File(p, apkName+latestVersionTag+".apk");
        if (downloadSucceeded) return;
        if (f.exists()) {
            downloadSucceeded = true;
            // provider is already included in the imagepicker lib
            Uri apkURI = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".imagepicker.provider", f);
            Intent intent = new Intent(Intent.ACTION_VIEW);

            intent.setDataAndType(apkURI, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

            activity.startActivity(intent);
        } else {
            if (attempts > 0) {
                Log.w(TAG, "Failed to download APK! Will keep trying " + attempts + " more times");
                downloadUpdate(apkName);
                attempts --;
            } else {
                AlertDialog.Builder failedDownloadDialogBuilder = new AlertDialog.Builder(activity);
                failedDownloadDialogBuilder.setTitle(R.string.update_failed_title);
                failedDownloadDialogBuilder.setMessage(R.string.update_failed_content);
                failedDownloadDialogBuilder.setNegativeButton(R.string.update_hide_button, (dialog, which) -> dialog.cancel());
                failedDownloadDialogBuilder.show();
            }
        }

    }
}