package com.threethan.launcher.browser;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    public void backFull() {
        int i = 0;
        while (canGoBackOrForward(i-1)) i--;
        goBackOrForward(i);
    }
    public void forwardFull() {
        int i = 0;
        while (canGoBackOrForward(i+1)) i++;
        goBackOrForward(i);
    }
}