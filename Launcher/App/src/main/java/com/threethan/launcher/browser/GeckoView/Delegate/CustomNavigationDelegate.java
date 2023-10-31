package com.threethan.launcher.browser.GeckoView.Delegate;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threethan.launcher.browser.BrowserActivity;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.WebRequestError;

import java.util.List;

public class CustomNavigationDelegate implements GeckoSession.NavigationDelegate {
    public boolean canGoBack = false;
    public boolean canGoForward = false;
    public boolean spotifixed = false;
    public String currentUrl = "";

    private final BrowserActivity mActivity;
    public CustomNavigationDelegate(BrowserActivity activity) {
        super();
        this.mActivity = activity;
    }
    @Override
    public void onCanGoBack(@NonNull GeckoSession session, boolean canGoBack) {
        this.canGoBack = canGoBack;
    }
    @Override
    public void onCanGoForward(@NonNull GeckoSession session, boolean canGoForward) {
        this.canGoForward = canGoForward;
    }
    @Override
    public void onLocationChange(@NonNull GeckoSession session, @Nullable String url,
                                 @NonNull List<GeckoSession.PermissionDelegate.ContentPermission> perms) {
        GeckoSession.NavigationDelegate.super.onLocationChange(session, url, perms);

        if (url != null && !url.isEmpty() && !url.equals("about:blank")) {
            currentUrl = url;
            mActivity.updateButtonsAndUrl(currentUrl);
        }
    }

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
        // Spotify-specific patch
        if (request.uri.contains("open.spotify.com"))
            session.getSettings().setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        else
            session.getSettings().setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);

        this.currentUrl = request.uri;
        this.mActivity.updateButtonsAndUrl(currentUrl);
        return GeckoSession.NavigationDelegate.super.onLoadRequest(session, request);
    }
}