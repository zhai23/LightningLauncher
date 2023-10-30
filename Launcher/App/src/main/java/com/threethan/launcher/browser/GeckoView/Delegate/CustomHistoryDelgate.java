package com.threethan.launcher.browser.GeckoView.Delegate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threethan.launcher.browser.BrowserActivity;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

public class CustomHistoryDelgate implements GeckoSession.HistoryDelegate {
    public HistoryList historyList = null;
    private final BrowserActivity mActivity;
    public CustomHistoryDelgate(BrowserActivity activity) {
        super();
        this.mActivity = activity;
    }
    @Nullable
    @Override
    public GeckoResult<Boolean> onVisited(@NonNull GeckoSession session, @NonNull String url, @Nullable String lastVisitedURL, int flags) {
        return GeckoSession.HistoryDelegate.super.onVisited(session, url, lastVisitedURL, flags);
    }
    @Override
    public void onHistoryStateChange(@NonNull GeckoSession session, @NonNull HistoryList historyList) {
        GeckoSession.HistoryDelegate.super.onHistoryStateChange(session, historyList);
        this.historyList = historyList;
    }
}