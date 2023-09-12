package com.threethan.launcher.browser;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BrowserWebView extends WebView {
    public List<String> history = new ArrayList<>();
    public int historyIndex = 0;
    protected BrowserWebChromeClient myClient;
    public BrowserWebView(@NonNull Context context) {
        super(context);
        setClient();
    }

    public BrowserWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClient();
    }

    public BrowserWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClient();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE) super.onWindowVisibilityChanged(View.VISIBLE);
    }

    protected void setClient() {
        myClient = new BrowserWebChromeClient();
        setWebChromeClient(myClient);
    }

    public void setActivity(Activity activity) {
        myClient.setActivity(activity);
    }
}