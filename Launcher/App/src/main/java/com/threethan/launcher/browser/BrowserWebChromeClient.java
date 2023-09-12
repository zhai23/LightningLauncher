package com.threethan.launcher.browser;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;

import androidx.core.app.ActivityCompat;

import java.util.Objects;

class BrowserWebChromeClient extends WebChromeClient {
    private PermissionRequest webkitPermissionRequest = null;
    private Activity activity = null;

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        webkitPermissionRequest = request;

        final String[] resources = request.getResources();
        for (String resource : resources) {
            // Audio recording
            if (Objects.equals(resource, PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                askForAudioPermission(resource);
            }
            // Allow DRM (spotify, tidal)
            if (Objects.equals(resource, PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
                request.grant(new String[]{resource});
            }
        }
    }
    private void askForAudioPermission(String webkitPermission) {
        if (activity == null) {
            Log.i("WebChromeClient", "Activity was null!");
            return;
        }
        String androidPermission = Manifest.permission.RECORD_AUDIO;
        if (ActivityCompat.checkSelfPermission(activity, androidPermission) == PackageManager.PERMISSION_GRANTED)
            webkitPermissionRequest.grant(new String[]{webkitPermission});
        else
            ActivityCompat.requestPermissions(activity, new String[]{androidPermission}, 1); // requestCode is arbitrary int
    }
    public void setActivity(Activity newActivity) {
        activity = newActivity;
    }
}