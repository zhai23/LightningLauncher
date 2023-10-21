package com.threethan.launcher.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Keyboard;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.StringLib;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/*
    BrowserActivity

    This activity is the browser used for web apps.
    It's launched with a 'url' in the extras for it's intent.

    It loads a WebView from the BrowserService based on its url.
    If the website is running in the background, it will grab that same WebView.

    Other than that, it's just a basic browser interface without tabs.
 */
public class BrowserActivity extends Activity {
    BrowserWebView w;
    TextView urlPre;
    TextView urlMid;
    TextView urlEnd;
    String baseUrl = null;
    View back;
    View forward;
    View zoomIn;
    View zoomOut;
    View dark;
    View light;
    View background;
    View addHome;
    public SharedPreferences sharedPreferences;
    // Keys
    public static final String KEY_WEBSITE_DARK = "KEY_WEBSITE_DARK-";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            w.restoreState(savedInstanceState);
            return;
        }

        Log.v("LightningLauncher", "Starting Browser Activity");

        setContentView(R.layout.activity_browser);
        getWindow().setStatusBarColor(Color.parseColor("#11181f"));

        //noinspection deprecation
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        background = findViewById(R.id.container);

        urlPre = findViewById(R.id.urlPre);
        urlMid = findViewById(R.id.urlMid);
        urlEnd = findViewById(R.id.urlEnd);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        baseUrl = Objects.requireNonNull(extras.getString("url"));

        // Buttons
        back = findViewById(R.id.back);
        forward = findViewById(R.id.forward);

        zoomIn = findViewById(R.id.zoomIn);
        zoomOut = findViewById(R.id.zoomOut);

        dark  = findViewById(R.id.darkMode);
        light = findViewById(R.id.lightMode);

        back.setOnClickListener((view) -> {
            if (w == null) return;
            if (w.canGoBack()) w.goBack();
            updateButtonsAndUrl();
        });
        forward.setOnClickListener((view) -> {
            if (w == null) return;
            if (w.canGoForward()) w.goForward();
            updateButtonsAndUrl();
        });

        findViewById(R.id.back).setOnLongClickListener((view -> {
            if (w == null) return false;
            w.forwardFull();
            updateButtonsAndUrl();
            return true;
        }));
        findViewById(R.id.forward).setOnLongClickListener((view -> {
            if (w == null) return false;
            w.backFull();
            updateButtonsAndUrl();
            return true;
        }));

        View refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener((view) -> reload());
        refresh.setOnLongClickListener((view) -> {
            w.loadUrl(baseUrl);
            w.clearQueued = true;
            updateButtonsAndUrl(baseUrl);
            return true;
        });

        View exit = findViewById(R.id.exit);
        exit.setOnClickListener((View) -> finish());

        zoomIn.setOnClickListener(view -> w.zoomIn());
        zoomOut.setOnClickListener(view -> w.zoomOut());

        dark .setOnClickListener(view -> updateDark(true ));
        light.setOnClickListener(view -> updateDark(false));

        boolean isDark = sharedPreferences.getBoolean(BrowserActivity.KEY_WEBSITE_DARK+baseUrl, true);
        updateDark(isDark);

        // Edit URL
        View urlLayout = findViewById(R.id.urlLayout);
        EditText urlEdit = findViewById(R.id.urlEdit);
        View topBar = findViewById(R.id.topBar);
        View topBarEdit = findViewById(R.id.topBarEdit);
        urlLayout.setOnClickListener((view) -> {
            topBar    .setVisibility(View.GONE);
            topBarEdit.setVisibility(View.VISIBLE);
            urlEdit.setText(currentUrl);
//            urlEdit.post(urlEdit::requestFocus);
            urlEdit.postDelayed(() -> Keyboard.show(this), 100);
        });
        urlEdit.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Perform action on Enter key press
                topBarEdit.findViewById(R.id.confirm).callOnClick();
                return true;
            }
            return false;
        });
        topBarEdit.findViewById(R.id.confirm).setOnClickListener((view) -> {
            String url = urlEdit.getText().toString();
            if (StringLib.isInvalidUrl(url)) url = StringLib.googleSearchForUrl(url);
            w.loadUrl(url);
            updateButtonsAndUrl(url);
            topBar.setVisibility(View.VISIBLE);
            topBarEdit.setVisibility(View.GONE);
            Keyboard.hide(this, topBar);
        });
        topBarEdit.findViewById(R.id.cancel).setOnClickListener((view) -> {
            topBar.setVisibility(View.VISIBLE);
            topBarEdit.setVisibility(View.GONE);
            Keyboard.hide(this, topBar);
        });

        addHome = findViewById(R.id.addHome);
        addHome.setOnClickListener(view -> {
            Platform.addWebsite(sharedPreferences, currentUrl, w.getTitle());
            addHome.setVisibility(View.GONE);
        });
    }

    private void updateButtonsAndUrl() {
        updateButtonsAndUrl(w.getUrl());
    }
    private void updateButtonsAndUrl(String url) {
        if (w == null) return;
        updateUrl(url);
        back.setVisibility(w.canGoBack()       && !w.clearQueued ? View.VISIBLE : View.GONE);
        forward.setVisibility(w.canGoForward() && !w.clearQueued ? View.VISIBLE : View.GONE);
    }
    private void updateZoom(float scale) {
        zoomIn .setVisibility(scale < 2.00 ? View.VISIBLE : View.GONE);
        zoomOut.setVisibility(scale > (Platform.isTv(this) ? 1.61 : 1.21) ? View.VISIBLE : View.GONE);
    }
    private void updateDark(boolean newDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dark.setVisibility(newDark ? View.GONE : View.VISIBLE);
            light.setVisibility(!newDark ? View.GONE : View.VISIBLE);
            sharedPreferences.edit().putBoolean(KEY_WEBSITE_DARK + baseUrl, newDark).apply();

            if (w != null)
                w.getSettings().setForceDark(newDark ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
        }
    }
    private void reload() {
        w.reload();
    }
    private String currentUrl = "";

    // Splits the URL into parts and updates the URL display
    @SuppressLint("SetTextI18n") // It wants me to use a string resource to add a dot
    private void updateUrl(String url) {
        url = url.replace("https://","");
        String[] split = url.split("\\.");

        if (split.length <= 1) {
            urlPre.setText("");
            urlMid.setText(url);
            urlEnd.setText("");
        } else if (split.length == 2) {
            urlPre.setText("");
            urlMid.setText(split[0]);
            urlEnd.setText(url.replace(split[0], ""));
        } else {
            urlPre.setText(split[0] + ".");
            urlMid.setText(split[1]);
            urlEnd.setText(url.replace(split[0]+"."+split[1], ""));
        }
        currentUrl = url;

        addHome.setVisibility(View.VISIBLE);
        if (StringLib.isInvalidUrl(url)) addHome.setVisibility(View.GONE);
        else if (StringLib.compareUrl(baseUrl, url) && !isEphemeral()) addHome.setVisibility(View.GONE);
        else {
            Set<String> webList = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, new HashSet<>());
            for (String webUrl : webList) {
                if (StringLib.compareUrl(webUrl, url)) {
                    addHome.setVisibility(View.GONE);
                    return;
                }
            }
        }
    }

    BrowserService wService;
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to BrowserService, which will provide our WebView
        BrowserService.bind(this, connection, true);
    }
    // Defines callbacks for service binding
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to BrowserService, cast the IBinder and get the BrowserService instance.
            BrowserService.LocalBinder binder = (BrowserService.LocalBinder) service;
            wService = binder.getService();
            onBound();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };
    @Override
    public void onBackPressed() {
        if (findViewById(R.id.topBarEdit).getVisibility() == View.VISIBLE)
            findViewById(R.id.cancel).callOnClick();
        else {
            if (w == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @SuppressLint("WebViewApiAvailability") BrowserWebChromeClient client = (BrowserWebChromeClient) w.getWebChromeClient();
                assert client != null;
                if (client.hasCustomView()) {
                    client.onHideCustomView();
                    return;
                }
            }
            if (w.canGoBack()) {
                w.goBack();
                updateButtonsAndUrl();
            }
            else finish();
        }
    }

    @Override
    protected void onDestroy() {
        // Don't keep search views in background
        if (isEphemeral())
            wService.killWebView(baseUrl);
        // Unbind
        unbindService(connection);
        super.onDestroy();
    }

    protected boolean isEphemeral() {
        if (w != null && w.getUrl() != null &&
            StringLib.isSearchUrl(baseUrl)) {
            for (ApplicationInfo app : Platform.appListBanner)
                if (Objects.equals(app.packageName, baseUrl)) return true;
            for (ApplicationInfo app : Platform.appListSquare)
                if (Objects.equals(app.packageName, baseUrl)) return true;
        }

        return false;
    }

    // Sets the WebView when the service is bound
    private void onBound() {
        w = wService.getWebView(this);
        CursorLayout container = findViewById(R.id.container);
        container.addView(w);
        container.targetView = w;

        View loading = findViewById(R.id.loading);

        // The WebViewClient will be overridden to provide info, incl. when a new page is loaded
        // Note than WebViewClient != WebChromeClient
        w.setWebViewClient(new WebViewClient() {
            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                super.onScaleChanged(view, oldScale, newScale);
                updateZoom(newScale);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loading.setVisibility(View.GONE);
                super.onPageFinished(view, url);
                // Remove blue highlights via basic javascript injection
                // this makes things feel a lot more native
                view.loadUrl("javascript:(function() { " +
                        "document.body.style.webkitTapHighlightColor = 'rgba(0,0,0,0)'; " +
                        "})()");
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                loading.setVisibility(View.VISIBLE);
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                if (isReload) loading.setVisibility(View.VISIBLE);
                super.doUpdateVisitedHistory(view, url, isReload);
                updateButtonsAndUrl(url);

                if (w.clearQueued) {
                    w.clearHistory();
                    w.clearQueued = false;
                }
            }
        });
        updateButtonsAndUrl();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            boolean isDark = w.getSettings().getForceDark() == WebSettings.FORCE_DARK_ON;
            (isDark ? light : dark).setVisibility(View.VISIBLE);
        }
    }

    public void addFullscreenView(View view) {
        if (w != null) w.setVisibility(View.GONE);
        View topBar = findViewById(R.id.topBar);
        topBar.setVisibility(View.GONE);
        LinearLayout container = findViewById(R.id.container);
        container.addView(view);
    }
    public void removeFullscreenView(View view) {
        LinearLayout container = findViewById(R.id.container);
        container.removeView(view);
        if (w != null) w.setVisibility(View.VISIBLE);
        View topBar = findViewById(R.id.topBar);
        topBar.setVisibility(View.VISIBLE);
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (w != null) w.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (w != null) w.restoreState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        Dialog.setActivityContext(this);
        super.onResume();
    }
}
