package com.threethan.launcher.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Objects;

public class BrowserService extends Service {
    private final IBinder binder = new LocalBinder();
    private final HashMap<String, BrowserWebView> webViewByBaseUrl = new HashMap<>();
    private final HashMap<String, Activity> activityByBaseUrl = new HashMap<>();

    // Spoof chrome 116 without an OS. May flag bot detection
    String UA = "Mozilla/5.0 (x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public class LocalBinder extends Binder {
        public BrowserService getService() {
            return BrowserService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @SuppressLint("SetJavaScriptEnabled")
    public BrowserWebView getWebView(BrowserActivity activity) {
        BrowserWebView webView;
        final String url = activity.baseUrl;
        if (hasWebView(url)) {
            webView = webViewByBaseUrl.get(url);

            assert webView != null;
            LinearLayout parent = (LinearLayout) webView.getParent();
            if (parent != null) parent.removeView(webView);

            Activity owner = activityByBaseUrl.get(url);
            if (owner != null && owner != activity) {
                owner.finish();
                activityByBaseUrl.remove(url);
            }
        } else {
            webView = new BrowserWebView(getApplicationContext());
            webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            webViewByBaseUrl.put(url, webView);
            activityByBaseUrl.put(url, activity);

            webView.setInitialScale(activity.sharedPreferences
                    .getInt(BrowserActivity.KEY_WEBSITE_ZOOM+activity.baseUrl, 75));

            webView.loadUrl(url);

            final WebSettings ws = webView.getSettings();
            ws.setLoadWithOverviewMode(true);
            ws.setJavaScriptEnabled(true);
            ws.setAllowFileAccess(true);
            ws.setUserAgentString(UA);
            if (android.os.Build.VERSION.SDK_INT >= 29)
                ws.setForceDark(activity.sharedPreferences
                .getBoolean(BrowserActivity.KEY_WEBSITE_DARK+activity.baseUrl, true)
                ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
        }
        return webView;
    }

    public boolean hasWebView(String url) {
        return webViewByBaseUrl.containsKey(url);
    }
    public void killWebView(String url) {
        if (!hasWebView(url)) return;
        BrowserWebView webView = webViewByBaseUrl.get(url);
        if (webView == null) return;
        webView.destroy();
        webViewByBaseUrl.remove(url);
        try {
            Objects.requireNonNull(activityByBaseUrl.get(url)).finish();
            activityByBaseUrl.remove(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.gc();
    }
    public void killActivities() {
        for (String key : activityByBaseUrl.keySet()) {
            try {
                Objects.requireNonNull(activityByBaseUrl.get(key)).finish();
                activityByBaseUrl.remove(key);
            } catch (Exception ignored) {}
        }
    }
}