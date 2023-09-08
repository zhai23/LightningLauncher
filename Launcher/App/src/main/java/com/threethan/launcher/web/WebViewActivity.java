package com.threethan.launcher.web;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.threethan.launcher.MainActivity;
import com.threethan.launcher.R;
import com.threethan.launcher.helpers.SettingsManager;
import com.threethan.launcher.platforms.AbstractPlatform;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class WebViewActivity extends Activity {
    CustomWebView w;
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
    public static final String KEY_WEBSITE_ZOOM = "KEY_WEBSITE_ZOOM-";
    public static final String KEY_WEBSITE_DARK = "KEY_WEBSITE_DARK-";

    // finishReceiver
    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { finish();}
    };
    public static final String FINISH_ACTION = "com.threethan.launcher.FINISH_WEB";
    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter(FINISH_ACTION);
        IntentFilter filter2= new IntentFilter(MainActivity.FINISH_ACTION);
        registerReceiver(finishReceiver, filter);
        registerReceiver(finishReceiver, filter2);

        Log.v("WebsiteLauncher", "Starting WebView Activity");

        setContentView(R.layout.activity_webview);
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
            if (w.history.isEmpty()) { updateButtons(); return; }

            w.future.add(w.current);

            final int i = w.history.size()-1;
            loadUrl(w.history.get(i));

            updateButtons();
        });
        forward.setOnClickListener((view) -> {
            if (w.future.isEmpty()) { updateButtons(); return; }

            w.history.add(w.current);

            final int i = w.future.size()-1;
            loadUrl(w.future.get(i));

            updateButtons();
        });

        findViewById(R.id.back).setOnLongClickListener((view -> {
            w.future.addAll(w.history);
            w.future.add(w.current);

            loadUrl(w.history.get(0));
            w.history.clear();

            updateButtons();
            return true;
        }));
        findViewById(R.id.forward).setOnLongClickListener((view -> {
            w.history.add(w.current);
            w.history.addAll(w.future);

            loadUrl(w.future.get(w.future.size()-1));
            w.future.clear();

            updateButtons();
            return true;
        }));


        View refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener((view) -> reload());

        View exit = findViewById(R.id.exit);
        exit.setOnClickListener((View) -> finish());

        zoomIn.setOnClickListener(view -> w.zoomIn());
        zoomOut.setOnClickListener(view -> w.zoomOut());

        dark .setOnClickListener(view -> updateDark(true ));
        light.setOnClickListener(view -> updateDark(false));

        boolean isDark = sharedPreferences.getBoolean(WebViewActivity.KEY_WEBSITE_DARK+baseUrl, true);
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
            AbstractPlatform.addWebApp(sharedPreferences, currentUrl);
            addHome.setVisibility(View.GONE);
        });
    }
    private void updateButtons() {
        try {
            back.setVisibility(w.history.isEmpty() ? View.GONE : View.VISIBLE);
            forward.setVisibility(w.future.isEmpty() ? View.GONE : View.VISIBLE);
        } catch (Exception ignored) {}
    }
    private void updateZoom(int scale) {
        try {
            zoomIn .setVisibility(scale > 150 ? View.GONE : View.VISIBLE);
            zoomOut.setVisibility(scale < 50 ? View.GONE : View.VISIBLE);
            sharedPreferences.edit().putInt(KEY_WEBSITE_ZOOM+baseUrl, scale).apply();
            w.setInitialScale(scale);
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
        if (url == null) return;
        w.current = null;
        w.history.remove(url);
        w.future .remove(url);
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
        boolean isDefault = url.replace("/","").equals(
        baseUrl.replace("https://","").replace("/",""));
        if (isDefault) addHome.setVisibility(View.GONE);
        else {
            Set<String> webList = sharedPreferences.getStringSet(SettingsManager.KEY_WEBSITE_LIST, new HashSet<>());
            for (String webUrl : webList) {
                if (url.replace("/", "")
                        .equals(webUrl.replace("https://", "").replace("/", ""))) {
                    addHome.setVisibility(View.GONE);
                    return;
                }
            }
            if (webList.contains(url) || webList.contains((url+" ").replace("/ ",""))) addHome.setVisibility(View.GONE);
            else addHome.setVisibility(View.VISIBLE);
        }
    }

    WebViewService wService;
    boolean wBound = false;

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService.
        Intent intent = new Intent(this, WebViewService.class);
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
        unregisterReceiver(finishReceiver);
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
                if (w.current != null) {
                    w.history.add(w.current);
                    w.future.clear();
                    updateButtons();
                }
                w.current = newUrl;
                return false;
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                super.onScaleChanged(view, oldScale, newScale);
                updateZoom((int) newScale * 100);
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
            WebViewService.LocalBinder binder = (WebViewService.LocalBinder) service;
            wService = binder.getService();
            wBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            wBound = false;
        }
    };


    public static void killInstances(Context context) {
        Intent finishIntent = new Intent(WebViewActivity.FINISH_ACTION);
        context.sendBroadcast(finishIntent);
    }

}
