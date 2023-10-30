package com.threethan.launcher.browser.GeckoView.Delegate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threethan.launcher.browser.BrowserActivity;
import com.threethan.launcher.browser.GeckoView.MobileForcedWebsites;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;

import java.util.List;

public class CustomNavigationDelegate implements GeckoSession.NavigationDelegate {
    public boolean canGoBack = false;
    public boolean canGoForward = false;
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
    }

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
        this.currentUrl = request.uri;
        session.getSettings().setUserAgentMode(MobileForcedWebsites.check(currentUrl)
                ? GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                : GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        );
        this.mActivity.updateButtonsAndUrl(currentUrl);
        return GeckoSession.NavigationDelegate.super.onLoadRequest(session, request);
    }
}