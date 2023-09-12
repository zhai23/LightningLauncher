package com.threethan.launcher.browser;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class BrowserWebView extends WebView {

    public ArrayList<String> history = new ArrayList<>();
    public ArrayList<String> future = new ArrayList<>();
    String current = null;

    public BrowserWebView(@NonNull Context context) {
        super(context);
    }

    public BrowserWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BrowserWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE) super.onWindowVisibilityChanged(View.VISIBLE);
    }
}