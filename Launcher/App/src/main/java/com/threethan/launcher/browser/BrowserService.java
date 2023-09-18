package com.threethan.launcher.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.threethan.launcher.R;
import com.threethan.launcher.launcher.LauncherActivity;

import java.util.HashMap;
import java.util.Objects;

public class BrowserService extends Service {
    private final IBinder binder = new LocalBinder();
    private final static HashMap<String, BrowserWebView> webViewByBaseUrl = new HashMap<>();
    private final static HashMap<String, Activity> activityByBaseUrl = new HashMap<>();

    private final static int NOTIFICATION_ID = 42;

    // Spoof chrome 116 on linux
    String UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36";

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
    @SuppressLint("SetJavaScriptEnabled") // Javascript is important
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

            webView.setInitialScale(100);

            webView.loadUrl(url);

            final WebSettings ws = webView.getSettings();
            ws.setLoadWithOverviewMode(true);
            ws.setJavaScriptEnabled(true);
            ws.setAllowFileAccess(true);
            ws.setUserAgentString(UA);
            // Fail-safes (they do make a difference!)
            ws.setDomStorageEnabled(true);
            ws.setLoadWithOverviewMode(true);
            ws.setBuiltInZoomControls(true);
            ws.setDisplayZoomControls(false);
            ws.setSupportZoom(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setDefaultTextEncodingName("utf-8");
            // Cookies
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

            // Dark Theme
            if (Build.VERSION.SDK_INT >= 29)
                ws.setForceDark(activity.sharedPreferences
                .getBoolean(BrowserActivity.KEY_WEBSITE_DARK+activity.baseUrl, true)
                ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
        }
        webView.setActivity(activity);
        updateStatus();
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
        if (activityByBaseUrl.get(url) != null) {
            Objects.requireNonNull(activityByBaseUrl.get(url)).finish();
            activityByBaseUrl.remove(url);
        }
        System.gc();
        updateStatus();
    }
    public void killActivities() {
        for (String key : activityByBaseUrl.keySet()) {
            if (activityByBaseUrl.get(key) != null) {
                activityByBaseUrl.get(key).finish();
                activityByBaseUrl.remove(key);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, getNotification());
        return super.onStartCommand(intent, flags, startId);

    }

    public static void bind(Activity activity, ServiceConnection connection){
        Intent intent = new Intent(activity, BrowserService.class);
        if (amRunning(activity)) {
            activity.bindService(intent, connection, 0);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent);
                activity.bindService(intent, connection, 0);
            } else activity.bindService(intent, connection, BIND_AUTO_CREATE);
        }
    }

    private static boolean amRunning(Activity activity) {
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BrowserService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void updateStatus() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, getNotification());

        if (webViewByBaseUrl.isEmpty()) stopSelf();
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, LauncherActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE);
        final int n = webViewByBaseUrl.size();
        return new Notification.Builder(this)
                .setContentTitle( n == 0 ? getString(R.string.notification_title_n) :
                        (n == 1 ? getString(R.string.notification_title_s) :
                        getString(R.string.notification_title_p, n) ))
                .setContentText(getText(R.string.notification_content))
                .setSmallIcon(R.drawable.ic_shortcut)
                .setContentIntent(pendingIntent)
                .build();
    }
}