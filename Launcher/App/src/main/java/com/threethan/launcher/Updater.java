package com.threethan.launcher;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

// Credit to Basti for update checking code
public class Updater {
    private static final String UPDATE_URL = "https://api.github.com/repos/threethan/LightningLauncher/releases/latest";
    private static final String TEMPLATE_URL = "https://github.com/threethan/LightningLauncher/releases/download/%s/LightningLauncher.apk";
    private static final String TAG = "LightningLauncher Updater";
    private final RequestQueue requestQueue;
    private final PackageManager packageManager;
    private final Context appContext;

    public Updater(Context appContext) {
        this.appContext = appContext;
        this.requestQueue = Volley.newRequestQueue(appContext);
        this.packageManager = appContext.getPackageManager();
    }

    public void checkForUpdate() {
        StringRequest updateRequest = new StringRequest(
                Request.Method.GET, UPDATE_URL,
                this::handleUpdateResponse, this::handleUpdateError);

        requestQueue.add(updateRequest);
    }

    private void handleUpdateResponse(String response) {
        try {
            JSONObject latestReleaseJson = new JSONObject(response);
            String tagName = latestReleaseJson.getString("tag_name");
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    appContext.getPackageName(), PackageManager.GET_ACTIVITIES);

            if (!("v" + packageInfo.versionName).contains(tagName)) {
                Log.v(TAG, "New version available!");
                showUpdateDialog(packageInfo.versionName, tagName);
            } else {
                Log.i(TAG, "App is up to date :)");
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
        AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(appContext);
        updateDialogBuilder.setTitle(R.string.update_title);
        updateDialogBuilder.setMessage(appContext.getString(R.string.update_content, curName, newName));
        updateDialogBuilder.setPositiveButton(R.string.update_button, (dialog, which) -> downloadUpdate(newName));
        AlertDialog updateAlertDialog = updateDialogBuilder.create();
        updateAlertDialog.show();
    }

    public void downloadUpdate(String versionTag) {
        DownloadManager.Request request1 = new DownloadManager.Request(Uri.parse(String.format(TEMPLATE_URL, versionTag)));
        request1.setDescription("Downloading Update");   //appears the same in Notification bar while downloading
        request1.setTitle("LightningLauncher Auto-Updater");

        request1.setDestinationInExternalFilesDir(appContext, "/Content", "update"+versionTag+".apk");

        DownloadManager manager1 = (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
        Objects.requireNonNull(manager1).enqueue(request1);


        installUpdate(versionTag);
    }

    public void installUpdate(String versionTag) {
        File p = appContext.getApplicationContext().getExternalFilesDir("/Content");
        File f = new File(p, "update"+versionTag+".apk");
        if (!f.exists()) {
            Log.w(TAG, "Failed to download APK! Will keep trying...");
            downloadUpdate(versionTag);
            return;
        }
        // provider is already included in the imagepicker lib
        Uri apkURI = FileProvider.getUriForFile(appContext, appContext.getApplicationContext().getPackageName() + ".imagepicker.provider", f);
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setDataAndType(apkURI, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

        appContext.startActivity(intent);

        AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(appContext);
        updateDialogBuilder.setTitle(R.string.update_failed_title);
        updateDialogBuilder.setMessage(R.string.update_failed_content);
        updateDialogBuilder.setPositiveButton(R.string.update_failed_button, (dialog, which) -> dialog.cancel());
        AlertDialog updateAlertDialog = updateDialogBuilder.create();
        updateAlertDialog.show();
    }
}