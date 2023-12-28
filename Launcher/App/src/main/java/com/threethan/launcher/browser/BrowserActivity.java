package com.threethan.launcher.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.threethan.launcher.R;
import com.threethan.launcher.browser.GeckoView.BrowserWebView;
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
    private BrowserWebView w;
    TextView urlPre;
    TextView urlMid;
    TextView urlEnd;
    String baseUrl = null;
    View back;
    View forward;
    View background;
    View loading;
    View addHome;
    public SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v("LightningLauncher", "Starting Browser Activity");

        setContentView(R.layout.activity_browser);
        getWindow().setStatusBarColor(Color.parseColor("#11181f"));

        //noinspection deprecation
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        background = findViewById(R.id.container);
        loading = findViewById(R.id.loading);

        urlPre = findViewById(R.id.urlPre);
        urlMid = findViewById(R.id.urlMid);
        urlEnd = findViewById(R.id.urlEnd);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        baseUrl = Objects.requireNonNull(extras.getString("url"));

        // Buttons
        back = findViewById(R.id.back);
        forward = findViewById(R.id.forward);

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

        // Edit URL
        View urlLayout = findViewById(R.id.urlLayout);
        EditText urlEdit = findViewById(R.id.urlEdit);
        View topBar = findViewById(R.id.topBar);
        View topBarEdit = findViewById(R.id.topBarEdit);
        urlLayout.setOnClickListener((view) -> {
            topBar    .setVisibility(View.GONE);
            topBarEdit.setVisibility(View.VISIBLE);
            urlEdit.setText(currentUrl);
            urlEdit.post(urlEdit::requestFocus);
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

            if (!StringLib.isInvalidUrl("https://" + url)) url = "https://" + url;
            else if (StringLib.isInvalidUrl(url)) url = StringLib.googleSearchForUrl(url);

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
            Platform.addWebsite(sharedPreferences, currentUrl);
            addHome.setVisibility(View.GONE);
        });

        findViewById(R.id.extensions).setOnClickListener(v -> BrowserService.ManageExtensions());
    }

    private void updateButtonsAndUrl() {
        updateButtonsAndUrl(w.getUrl());
    }
    public void updateButtonsAndUrl(String url) {
        if (w == null) return;
        updateUrl(url);
        back.setVisibility(w.canGoBack()       && !w.clearQueued ? View.VISIBLE : View.GONE);
        forward.setVisibility(w.canGoForward() && !w.clearQueued ? View.VISIBLE : View.GONE);
    }
    private void reload() {
        w.reload();
    }

    public void startLoading() {
        loading.setVisibility(View.VISIBLE);
    }
    public void stopLoading() {
        loading.setVisibility(View.GONE);
    }

    public void showTopBar() {
        findViewById(R.id.topBar).setVisibility(View.VISIBLE);
        findViewById(R.id.topBarEdit).setVisibility(View.GONE);
    }
    public void hideTopBar() {
        findViewById(R.id.topBar).setVisibility(View.GONE);
        findViewById(R.id.topBarEdit).setVisibility(View.GONE);
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
        if (!StringLib.isInvalidUrl("https://" + url)) url = "https://" + url;
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
    public BrowserService wService;
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
        public void onServiceDisconnected(ComponentName arg0) { wService = null; }
    };
    @Override
    public void onBackPressed() {
        if (findViewById(R.id.topBarEdit).getVisibility() == View.VISIBLE)
            findViewById(R.id.cancel).callOnClick();
        else {
            if (w.canGoBack()) {
                w.goBack();
                updateButtonsAndUrl();
            }
            else finish();
        }
    }

    @Override
    protected void
    onDestroy() {
        if (isFinishing()) {
            // Don't keep search views in background
            if (isEphemeral())
                wService.killWebView(baseUrl);
        }
        wService.removeActivity(this);
        // Unbind
        unbindService(connection);
        super.onDestroy();
    }

    protected boolean isEphemeral() {
        if (w != null && /*w.getUrl() != null &&*/
            StringLib.isSearchUrl(baseUrl)) {
            for (ApplicationInfo app : Platform.appListBanner)
                if (Objects.equals(app.packageName, baseUrl)) return true;
            for (ApplicationInfo app : Platform.appListSquare)
                if (Objects.equals(app.packageName, baseUrl)) return true;
        }

        return false;
    }
    public void loadUrl(String url) {
        w.loadUrl(url);
        updateButtonsAndUrl(url);
    }

    // Sets the WebView when the service is bound
    private void onBound() {
        w = wService.getWebView(this);
        CursorLayout container = findViewById(R.id.container);
        container.addView(w);
        container.targetView = w;

        updateButtonsAndUrl();
    }

    @Override
    protected void onResume() {
        Dialog.setActivityContext(this);
        super.onResume();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) onBackPressed();
        return true;
    }
}
