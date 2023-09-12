package com.threethan.launcher.browser;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Objects;

class BrowserWebChromeClient extends WebChromeClient {
    private PermissionRequest webkitPermissionRequest = null;
    public Activity activity = null;

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        super.onPermissionRequest(request);
        webkitPermissionRequest = request;

        final String[] resources = request.getResources();
        for (String resource : resources) {
            if (Objects.equals(resource, PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                askForWebkitPermission(resource, Manifest.permission.RECORD_AUDIO);
            }
            if (Objects.equals(resource, PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                askForWebkitPermission(resource, Manifest.permission.CAMERA);
            }
        }
    }


    private void askForWebkitPermission(String webkitPermission, String androidPermission) {
        if (activity == null) {
            Log.i("WebChromeClient", "Activity was null!");
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, androidPermission) == PackageManager.PERMISSION_GRANTED)
            webkitPermissionRequest.grant(new String[]{webkitPermission});
        else
            ActivityCompat.requestPermissions(activity, new String[]{androidPermission}, 1); // requestCode is arbitrary int
        }
    }
}