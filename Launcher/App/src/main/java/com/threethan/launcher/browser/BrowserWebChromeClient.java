package com.threethan.launcher.browser;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.Objects;

/*
    BrowserWebChromeClient

    A customized version of WebChromeClient which enables audio capture (recording) and DRM audio
    and video playback.

    It's worth noting that a camera permission can also be granted in a similar manner, but does not
    work on oculus.
 */
class BrowserWebChromeClient extends WebChromeClient {
    private PermissionRequest webkitPermissionRequest = null;
    private BrowserActivity activity = null;

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
    public void setActivity(BrowserActivity newActivity) {
        activity = newActivity;
    }

    @Override
    public void onHideCustomView() {
        activity.removeFullscreenView(customView);
        callback.onCustomViewHidden();
        this.customView = null;
        this.callback = null;
    }

    private View customView;
    private WebChromeClient.CustomViewCallback callback;
    @Override
    public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback) {
        if (this.customView != null) onHideCustomView();
        this.callback = paramCustomViewCallback;
        activity.addFullscreenView(paramView);
    }
    public boolean hasCustomView() {
        return (customView != null);
    }

    @Nullable
    @Override
    public Bitmap getDefaultVideoPoster() {
        return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    }
}