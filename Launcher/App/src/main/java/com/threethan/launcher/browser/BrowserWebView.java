package com.threethan.launcher.browser;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/*
    BrowserWebView

    A customized version of WebView which keeps media playing in the background.
    onWindowVisibilityChanged usually pauses media playback when a view is not visible, but this
    class prevents a super call in that case.

    It also automatically uses the BrowserWebChromeClient instead of the default WebChromeClient.
 */
public class BrowserWebView extends WebView {
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
        if (visibility == View.VISIBLE) super.onWindowVisibilityChanged(View.VISIBLE);
    }

    protected void setClient() {
        myClient = new BrowserWebChromeClient();
        setWebChromeClient(myClient);
    }
    public void setActivity(BrowserActivity activity) {
        myClient.setActivity(activity);
    }


    // History
    protected List<String> history = Collections.synchronizedList(new ArrayList<>());
    public int historyIndex = 0;
    public void forward() {
        historyIndex ++;
        loadUrl(history.get(historyIndex-1));
    }
    public boolean back() {
        if (historyIndex <= 1) return false;
        historyIndex--;
        loadUrl(history.get(historyIndex - 1));
        return true;
    }
    public void backFull() {
        historyIndex = history.size();
        loadUrl(history.get(historyIndex-1));
    }
    public void forwardFull() {
        historyIndex = 1;
        loadUrl(history.get(0));
    }

    public void addHistory(String url) {
        if (historyIndex == 0 || !Objects.equals(url, history.get(historyIndex - 1))) {
            history = history.subList(0, historyIndex);
            history.add(url);
            historyIndex++;
        }
    }
}