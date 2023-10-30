package com.threethan.launcher.browser.GeckoView.Delegate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threethan.launcher.browser.BrowserActivity;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

public class CustomHistoryDelgate implements GeckoSession.HistoryDelegate {
    public HistoryList historyList = null;

    public CustomHistoryDelgate(BrowserActivity activity) {
        super();
    }
    @Override
    public void onHistoryStateChange(@NonNull GeckoSession session, @NonNull HistoryList historyList) {
        GeckoSession.HistoryDelegate.super.onHistoryStateChange(session, historyList);
        this.historyList = historyList;
    }
}