package com.threethan.launcher.web;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Objects;

public class WebViewService extends Service {
    private final IBinder binder = new LocalBinder();
    private final HashMap<String, CustomWebView> webViewsByBaseUrl = new HashMap<>();
    private final HashMap<String, Activity> activityByBaseUrl = new HashMap<>();

    // Spoof chrome 116 sans OS
    String UA = "Mozilla/5.0 (x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public class LocalBinder extends Binder {
        public WebViewService getService() {
            // Return this instance of LocalService so clients can call public methods.
            return WebViewService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @SuppressLint("SetJavaScriptEnabled")
    public CustomWebView getWebView(WebViewActivity activity) {
        CustomWebView webView;
        final String url = activity.baseUrl;
        if (hasWebView(url)) {
            webView = webViewsByBaseUrl.get(url);

            assert webView != null;
            LinearLayout parent = (LinearLayout) webView.getParent();
            if (parent != null) parent.removeView(webView);

            try {
                Objects.requireNonNull(activityByBaseUrl.get(url)).finish();
                activityByBaseUrl.remove(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            webView = new CustomWebView(getApplicationContext());
            webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            webViewsByBaseUrl.put(url, webView);
            activityByBaseUrl.put(url, activity);

            webView.setInitialScale(activity.sharedPreferences
                    .getInt(WebViewActivity.KEY_WEBSITE_ZOOM+activity.baseUrl, 75));

            webView.loadUrl(url);

            final WebSettings ws = webView.getSettings();
            ws.setLoadWithOverviewMode(true);
            ws.setJavaScriptEnabled(true);
            ws.setAllowFileAccess(true);
            ws.setUserAgentString(UA);
            if (android.os.Build.VERSION.SDK_INT >= 29)
                ws.setForceDark(activity.sharedPreferences
                .getBoolean(WebViewActivity.KEY_WEBSITE_DARK+activity.baseUrl, true)
                ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
        }
        return webView;

    }

    public boolean hasWebView(String url) {
        return webViewsByBaseUrl.containsKey(url);
    }
    public void killWebView(String url) {
        if (!hasWebView(url)) return;
        CustomWebView webView = webViewsByBaseUrl.get(url);
        if (webView == null) return;
        webView.destroy();
        webViewsByBaseUrl.remove(url);
        try {
            Objects.requireNonNull(activityByBaseUrl.get(url)).finish();
            activityByBaseUrl.remove(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.gc();
    }
}