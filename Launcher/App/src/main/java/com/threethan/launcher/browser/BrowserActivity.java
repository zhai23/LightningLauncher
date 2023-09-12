package com.threethan.launcher.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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

import com.threethan.launcher.R;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.StringLib;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

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

        back = findViewById(R.id.back);
        forward = findViewById(R.id.forward);

        zoomIn = findViewById(R.id.zoomIn);
        zoomOut = findViewById(R.id.zoomOut);

        dark  = findViewById(R.id.darkMode);
        light = findViewById(R.id.lightMode);

        back.setOnClickListener((view) -> {
            try {
                w.historyIndex--;
                loadUrl(w.history.get(w.historyIndex - 1));
                updateButtons();
                Log.v("Browser History", w.history.toString());
                Log.v("Browser History Index", String.valueOf(w.historyIndex));
            } catch (Exception ignored) {}
        });
        forward.setOnClickListener((view) -> {
            try {
                w.historyIndex ++;
                loadUrl(w.history.get(w.historyIndex-1));
                updateButtons();
                Log.v("Browser History" ,w.history.toString());

                Log.v("Browser History Index" , String.valueOf(w.historyIndex));
            } catch (Exception ignored) {}
        });

        findViewById(R.id.back).setOnLongClickListener((view -> {
            try {
                w.historyIndex = 1;
                loadUrl(w.history.get(0));
                updateButtons();
            } catch (Exception ignored) {}
            return true;
        }));
        findViewById(R.id.forward).setOnLongClickListener((view -> {
            try {
                w.historyIndex = w.history.size();
                loadUrl(w.history.get(w.historyIndex-1));
                updateButtons();
            } catch (Exception ignored) {}
            return true;
        }));


        View refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener((view) -> reload());
        refresh.setOnLongClickListener((view) -> {
            w.history.clear();
            w.historyIndex = 0;
            loadUrl(baseUrl);
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
            urlEdit.requestFocus(View.LAYOUT_DIRECTION_RTL);
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
            loadUrl(urlEdit.getText().toString());
            topBar.setVisibility(View.VISIBLE);
            topBarEdit.setVisibility(View.GONE);
        });
        topBarEdit.findViewById(R.id.cancel).setOnClickListener((view) -> {
            topBar.setVisibility(View.VISIBLE);
            topBarEdit.setVisibility(View.GONE);
        });

        addHome = findViewById(R.id.addHome);
        addHome.setOnClickListener(view -> {
            Platform.addWebsite(sharedPreferences, currentUrl);
            addHome.setVisibility(View.GONE);
        });
    }
    private void updateButtons() {
        try {
            back.setVisibility(w.historyIndex > 1 ? View.VISIBLE : View.GONE);
            forward.setVisibility(w.historyIndex < w.history.size() ? View.VISIBLE : View.GONE);
        } catch (Exception ignored) {}
    }
    private void updateZoom(float scale) {
        try {
            zoomIn .setVisibility(scale < 2.00 ? View.VISIBLE : View.GONE);
            zoomOut.setVisibility(scale > 1.01 ? View.VISIBLE : View.GONE);
            Log.i("SCALE", String.valueOf(scale));
        } catch (Exception ignored) {}
    }
    private void updateDark(boolean newDark) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dark .setVisibility(newDark  ? View.GONE : View.VISIBLE);
                light.setVisibility(!newDark ? View.GONE : View.VISIBLE);
                sharedPreferences.edit().putBoolean(KEY_WEBSITE_DARK+baseUrl, newDark).apply();

                if (w != null) w.getSettings().setForceDark(newDark ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);

                background.setBackgroundResource(newDark ? R.drawable.bg_meta_dark : R.drawable.bg_meta_light);
            }
        } catch (Exception ignored) {}
    }
    private void loadUrl(String url) {
        Log.v("Browser url", url);
        w.loadUrl(url);
        updateUrl(url);
    }
    private void reload() {
        w.reload();
    }
    private String currentUrl = "";
    @SuppressLint("SetTextI18n")
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
        else if (StringLib.compareUrl(baseUrl, url)) addHome.setVisibility(View.GONE);
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
    boolean wBound = false;

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService.
        Intent intent = new Intent(this, BrowserService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        LinearLayout container = findViewById(R.id.container);
        container.post(new Runnable() {
            @Override
            public void run() {
                if (wBound) justBound();
                else container.post(this);
            }
        });
    }

    @Override
    public void onBackPressed() {
        back.callOnClick();
    }

    @Override
    protected void onDestroy() {
        unbindService(connection);
        super.onDestroy();
    }

    private void justBound() {
        w = wService.getWebView(this);
        LinearLayout container = findViewById(R.id.container);
        container.addView(w);
        w.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                final String newUrl = String.valueOf(request.getUrl());
                updateUrl(newUrl);
                w.history = w.history.subList(0, w.historyIndex);
                w.history.add(newUrl);
                w.historyIndex++;
                Log.v("Browser History" ,w.history.toString());
                updateButtons();
                return false;
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                super.onScaleChanged(view, oldScale, newScale);
                updateZoom(newScale);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Remove blue highlights via basic javascript injection
                // this makes things feel a lot more native
                view.loadUrl("javascript:(function() { " +
                        "document.body.style.webkitTapHighlightColor = 'rgba(0,0,0,0)'; " +
                        "})()");
            }
        });
        w.setBackgroundColor(Color.parseColor("#10000000"));
        updateUrl(w.getUrl());

        updateButtons();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            boolean isDark = w.getSettings().getForceDark() == WebSettings.FORCE_DARK_ON;
            (isDark ? light : dark).setVisibility(View.VISIBLE);
        }
    }


    /** Defines callbacks for service binding, passed to bindService(). */
    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            BrowserService.LocalBinder binder = (BrowserService.LocalBinder) service;
            wService = binder.getService();
            wBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            wBound = false;
        }
    };
}
