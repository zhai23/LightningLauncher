package com.threethan.launcher.web;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.threethan.launcher.R;

import java.util.Objects;

public class WebViewActivity extends Activity {
    CustomWebView w;
    TextView urlPre;
    TextView urlMid;
    TextView urlEnd;
    String baseUrl = null;
    View back;
    View forward;

    // User agent of oculus browser v23. May cause issues with openXR apps which won't work
    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { finish();}
    };
    public static final String FINISH_ACTION = "com.threethan.launcher.FINISH_WEB";
    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter(FINISH_ACTION);
        registerReceiver(finishReceiver, filter);

        Log.v("WebsiteLauncher", "Starting WebView Activity");

        setContentView(R.layout.activity_webview);
        getWindow().setStatusBarColor(Color.parseColor("#11181f"));

        urlPre = findViewById(R.id.urlPre);
        urlMid = findViewById(R.id.urlMid);
        urlEnd = findViewById(R.id.urlEnd);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        baseUrl = Objects.requireNonNull(extras.getString("url"));

        back = findViewById(R.id.back);
        forward = findViewById(R.id.forward);

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
    }
    private void updateButtons() {
        back   .setVisibility(w.history.isEmpty() ? View.GONE : View.VISIBLE);
        forward.setVisibility(w.future .isEmpty() ? View.GONE : View.VISIBLE);
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
    private void updateUrl(String url) {
        url = url.replace("https://","");
        String pre = url.split("\\.")[0]+".";
        String mid = url.split("\\.")[1];
        urlPre.setText(pre);
        urlMid.setText(mid);
        urlEnd.setText(url.replace(pre+mid,""));
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
                    findViewById(R.id.back).setVisibility(View.VISIBLE);
                }
                w.current = newUrl;
                return false;
            }
        });
        w.setBackgroundColor(Color.parseColor("#70000000"));
        updateUrl(w.getUrl());
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


}
