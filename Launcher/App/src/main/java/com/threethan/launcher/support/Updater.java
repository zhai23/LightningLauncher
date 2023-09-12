package com.threethan.launcher.support;

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
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.threethan.launcher.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;

// Credit to Basti for update checking code
public class Updater {
    private static final String UPDATE_URL = "https://api.github.com/repos/threethan/LightningLauncher/releases/latest";
    private static final String TEMPLATE_URL = "https://github.com/threethan/LightningLauncher/releases/download/%s/LightningLauncher.apk";
    private static final String TAG = "LightningLauncher Updater";
    private final RequestQueue requestQueue;
    private final PackageManager packageManager;
    private final Activity activity;
    private static final String KEY_IGNORED_UPDATE_VERSION = "UPDATER_IGNORED_UPDATE_VERSION";
    private static final String KEY_UPDATE_AVAILABLE = "UPDATER_UPDATE_AVAILABLE";
    public Updater(Activity activity) {
        this.activity = activity;
        this.requestQueue = Volley.newRequestQueue(activity);
        this.packageManager = activity.getPackageManager();
    }

    public void checkForUpdate() {
        StringRequest updateRequest = new StringRequest(
                Request.Method.GET, UPDATE_URL,
                this::handleUpdateResponse, this::handleUpdateError);
        requestQueue.add(updateRequest);
    }
    public void updateEvenIfSkipped() {
        getSharedPreferences().edit().remove(KEY_IGNORED_UPDATE_VERSION).apply();
        checkForUpdate();
    }

    private void handleUpdateResponse(String response) {
        try {
            JSONObject latestReleaseJson = new JSONObject(response);
            String tagName = latestReleaseJson.getString("tag_name");
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    activity.getPackageName(), PackageManager.GET_ACTIVITIES);

            if (!("v" + packageInfo.versionName).contains(tagName)) {
                Log.v(TAG, "New version available!");
                getSharedPreferences().edit().putBoolean(KEY_UPDATE_AVAILABLE, true).apply();
                if (tagName.equals(getSharedPreferences().getString(KEY_IGNORED_UPDATE_VERSION, null))) return;
                showUpdateDialog(packageInfo.versionName, tagName);
            } else {
                Log.i(TAG, "App is up to date :)");
                getSharedPreferences().edit().putBoolean(KEY_UPDATE_AVAILABLE, false).apply();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Received invalid JSON", e);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
        }
    }
    private void handleUpdateError(VolleyError error) {
        Log.w(TAG, "Couldn't get update info", error);
    }

    private void showUpdateDialog(String curName, String newName) {
        try {
            AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(activity);
            updateDialogBuilder.setTitle(R.string.update_title);
            updateDialogBuilder.setMessage(activity.getString(R.string.update_content, curName, newName));
            updateDialogBuilder.setPositiveButton(R.string.update_button, (dialog, which) -> downloadUpdate(newName));
            updateDialogBuilder.setNegativeButton(R.string.update_skip_button, (dialog, which) -> skipUpdate(newName));
            AlertDialog updateAlertDialog = updateDialogBuilder.create();
            updateAlertDialog.show();
        } catch (Exception ignored) {}
    }
    private SharedPreferences getSharedPreferences() {
        //noinspection deprecation
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
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void downloadUpdate(String versionTag) {
        DownloadManager.Request request1 = new DownloadManager.Request(Uri.parse(String.format(TEMPLATE_URL, versionTag)));
        request1.setDescription("Downloading Update");   //appears the same in Notification bar while downloading
        request1.setTitle("LightningLauncher Auto-Updater");

        request1.setDestinationInExternalFilesDir(activity, "/Content", "update"+versionTag+".apk");

        DownloadManager manager1 = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        Objects.requireNonNull(manager1).enqueue(request1);

        AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(activity);
        updateDialogBuilder.setTitle(R.string.update_downloading_title);
        updateDialogBuilder.setMessage(R.string.update_downloading_content);
        updateDialogBuilder.setNegativeButton(R.string.update_hide_button, (dialog, which) -> dialog.cancel());
        updateAlertDialog = updateDialogBuilder.create();
        updateAlertDialog.show();

        latestTag = versionTag;
        activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
    String latestTag;
    AlertDialog updateAlertDialog;
    BroadcastReceiver onComplete=new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                updateAlertDialog.dismiss();
            } catch (Exception ignored) {}
            installUpdate(latestTag);
        }
    };

    public void installUpdate(String versionTag) {
        File p = activity.getApplicationContext().getExternalFilesDir("/Content");
        File f = new File(p, "update"+versionTag+".apk");
        if (!f.exists()) {
            Log.w(TAG, "Failed to download APK! Will keep trying...");
            downloadUpdate(versionTag);
            return;
        }
        // provider is already included in the imagepicker lib
        Uri apkURI = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".imagepicker.provider", f);
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setDataAndType(apkURI, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

        activity.startActivity(intent);
    }
}