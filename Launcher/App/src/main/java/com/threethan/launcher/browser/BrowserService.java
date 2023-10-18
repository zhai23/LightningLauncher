package com.threethan.launcher.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.FileLib;
import com.threethan.launcher.support.Updater;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/*
    BrowserService

    Similar to the LauncherService, this service provides views for websites
    Specifically, it provides the WebView itself. The browser interface (buttons, url bar) is
    handled by the browser activity.

    WebViews aren't just stored into memory - this service runs as a foreground service,
    so WebViews it owns are kept running with foreground privileges at all times. This lets them
    work in the background.

    This lets websites play audio/record in the background through this app.
    com.oculus.permission.PLAY_AUDIO_BACKGROUND & com.oculus.permission.RECORD_AUDIO_BACKGROUND
    let the app itself play audio in the background on oculus devices without system tweaks.
 */
public class BrowserService extends Service {
    private final IBinder binder = new LocalBinder();
    private final static HashMap<String, BrowserWebView> webViewByBaseUrl = new HashMap<>();
    private final static HashMap<String, Activity> activityByBaseUrl = new HashMap<>();

    // Arbitrary ID for the persistent notification
    private final static int NOTIFICATION_ID = 42;

    // User agent string; this is taken from the same version of chromium running as Google Chrome on desktop linux.
    // Android chrome may be more accurate, but would cause sites to serve mobile pages,
    // which look too large and often redirect to the play store instead of working in-browser.
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
    @SuppressLint({"SetJavaScriptEnabled", "UnspecifiedRegisterReceiverFlag"})
    // Javascript is important
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
                activityByBaseUrl.put(url, activity);
            }
        } else {
            webView = new BrowserWebView(getApplicationContext());
            webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            webViewByBaseUrl.put(url, webView);
            activityByBaseUrl.put(url, activity);

            webView.setInitialScale(Platform.isTv(activity) ? 160 : 120);

            // Change a number of settings to behave more like a normal browser
            final WebSettings ws = webView.getSettings();
            ws.setLoadWithOverviewMode(true);
            ws.setJavaScriptEnabled(true);
            ws.setAllowFileAccess(true);
            ws.setUserAgentString(UA);
            ws.setDomStorageEnabled(true);
            ws.setLoadWithOverviewMode(true);
            ws.setBuiltInZoomControls(true);
            ws.setDisplayZoomControls(false);
            ws.setSupportZoom(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setDefaultTextEncodingName("utf-8");
            ws.setJavaScriptCanOpenWindowsAutomatically(true);
            ws.setRenderPriority(WebSettings.RenderPriority.HIGH); // May improve performance
            // Enable Cookies
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

            // Set website's to use chrome's internal dark mode
            if (Build.VERSION.SDK_INT >= 29)
                ws.setForceDark(activity.sharedPreferences
                .getBoolean(BrowserActivity.KEY_WEBSITE_DARK+activity.baseUrl, false)
                ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);

            activity.findViewById(R.id.loading).setVisibility(View.VISIBLE);
            webView.loadUrl(url);
        }
        webView.setActivity(activity);
        updateStatus();

        webView.setDownloadListener((url1, userAgent, contentDisposition, mimetype, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(url1));
            final String filename= URLUtil.guessFileName(url1, contentDisposition, mimetype);

            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!

            if (filename.endsWith(".apk")) request.setDestinationInExternalFilesDir(activity, Updater.APK_DIR, filename);
            else try {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            } catch (IllegalStateException ignored) {
                // If we can't access downloads dir
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, filename);
            }

            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            final long id = manager.enqueue(request);
            downloadFilenameById.put(id, filename);
            downloadActivityById.put(id, activity);

            Dialog.toast(getString(R.string.web_download_started), filename, true);
        });
        return webView;
    }

    // Downloads
    Map<Long, String> downloadFilenameById = new ConcurrentHashMap<>();
    Map<Long, Activity> downloadActivityById = new ConcurrentHashMap<>();
    BroadcastReceiver onDownloadComplete=new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            String filename = downloadFilenameById.get(id);
            downloadFilenameById.remove(id);

            if (filename == null) return;

            if (filename.endsWith(".apk")) {
                final File path = getApplicationContext().getExternalFilesDir(Updater.APK_DIR);
                final File file = new File(path, filename);

                if (Dialog.getActivityContext() == null) {
                    // If we can't show an alert, copy AND prompt install
                    copyToDownloads(file);
                    promptInstall(file);
                }
                AlertDialog dialog = Dialog.build(Dialog.getActivityContext(), R.layout.dialog_downloaded_apk);
                dialog.findViewById(R.id.install).setOnClickListener(v -> {
                    promptInstall(file);
                    dialog.dismiss();
                });
                dialog.findViewById(R.id.save).setOnClickListener(v -> {
                    copyToDownloads(file);
                    dialog.dismiss();
                });
                dialog.findViewById(R.id.delete).setOnClickListener(v -> {
                    final boolean ignored = file.delete();
                    dialog.dismiss();
                });
                ((TextView) dialog.findViewById(R.id.downloadMessage)).setText(getString(R.string.web_apk_prompt_message_pre, filename));
                AlertDialog.Builder builder = new AlertDialog.Builder(Dialog.getActivityContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert);
            } else {
                final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                final File file = new File(path, filename);

                // provider is already included in the imagepicker lib
                Uri fileURI = FileProvider.getUriForFile(getBaseContext(), getApplicationContext().getPackageName() + ".imagepicker.provider", file);

                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setDataAndType(fileURI, getContentResolver().getType(fileURI));
                openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    startActivity(openIntent);
                } catch (ActivityNotFoundException ignored) {
                    Dialog.toast(getString(R.string.web_download_finished), filename, true);
                }
            }
        }
    };
    private void copyToDownloads(File file) {
        // Copy to downloads
        // Original file will be deleted next time the updater is called
        File dlPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File dlFile = new File(dlPath, file.getName());
        FileLib.copy(file, dlFile);
    }
    private void promptInstall(File file) {
        if(!file.exists()) return;

        // provider is already included in the imagepicker lib
        Uri apkURI = FileProvider.getUriForFile(getBaseContext(), getApplicationContext().getPackageName() + ".imagepicker.provider", file);

        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(apkURI, "application/vnd.android.package-archive");
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

        startActivity(openIntent);
    }
    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(onDownloadComplete);
        } catch (Exception ignored) {}
        super.onDestroy();
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
                Objects.requireNonNull(activityByBaseUrl.get(key)).finish();
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

    public static void bind(Activity activity, ServiceConnection connection, boolean needed){
        Intent intent = new Intent(activity, BrowserService.class);
        if (amRunning(activity)) {
            activity.bindService(intent, connection, 0);
        } else if (needed) {
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

        if (webViewByBaseUrl.size() < 1) stopSelf();
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