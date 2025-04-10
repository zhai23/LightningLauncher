package com.threethan.launcher.activity.adapter;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Extends GridLayoutManager to prefetch more aggressively,
 * and to scroll to a target position even as data changes
 */
public class LauncherGridLayoutManager extends GridLayoutManager {
    /**
     * @noinspection unused
     */
    public LauncherGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setItemPrefetchEnabled(true);
    }

    public LauncherGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
        setItemPrefetchEnabled(true);
    }

    @Override
    public void collectAdjacentPrefetchPositions(int dx, int dy, RecyclerView.State state,
                                                 LayoutPrefetchRegistry layoutPrefetchRegistry) {
        super.collectAdjacentPrefetchPositions(dx, dy, state, layoutPrefetchRegistry);
        int itemCount = getItemCount();
        if (itemCount > 0) {
            int lastVisiblePosition = findLastVisibleItemPosition();
            while (lastVisiblePosition != RecyclerView.NO_POSITION && lastVisiblePosition + 1 < itemCount) {
                layoutPrefetchRegistry.addPosition(++lastVisiblePosition, lastVisiblePosition * 50 + 5000);
            }
        }
    }
}