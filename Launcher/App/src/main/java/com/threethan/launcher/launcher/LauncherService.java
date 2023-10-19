package com.threethan.launcher.launcher;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;
import com.threethan.launcher.browser.BrowserActivitySeparate;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/*
    LauncherService

    This class runs as a service, but does not do anything on its own.

    It is used by LauncherActivity to store the view containing the main interface in memory,
    meaning if the activity is finished and reopened, it may re-use the same preloaded view.

    It also provides a number of helper functions that enable this & help with multitasking
 */

public class LauncherService extends Service {
    private final IBinder binder = new LocalBinder();
    private final static ConcurrentHashMap<Integer, View> viewByIndex = new ConcurrentHashMap<>();
    @Nullable public static WeakReference<BrowserActivitySeparate> browserActivitySeparateRef = null;
    public class LocalBinder extends Binder {
        public LauncherService getService() {
            return LauncherService.this;
        }
    }
    private static final Map<LauncherActivity, Integer> activityByIndex = Collections.synchronizedMap(new HashMap<>());
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public View getNewView(LauncherActivity activity) {
        final int index = getNewActivityIndex();

        View view = View.inflate(activity, R.layout.activity_main, null);
        viewByIndex.put(index, view);
        activityByIndex.put(activity, index);

        return view;
    }
    public View getExistingView(LauncherActivity activity) {
        final int index = getNewActivityIndex();
        View view = viewByIndex.get(index);

        assert view != null;
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) parent.removeView(view);
        activityByIndex.put(activity, index);
        return view;
    }
    public boolean checkForExistingView() {
        return viewByIndex.containsKey(getNewActivityIndex());
    }
    protected int getNewActivityIndex() {
        int i = 0;
        while(activityByIndex.containsValue(i)) i++;
        return i;
    }
    public void destroyed(LauncherActivity activity) {
        activityByIndex.remove(activity);
    }

    public void finishAllActivities() {
        for (Activity activity: activityByIndex.keySet()) activity.finishAndRemoveTask();
        if (browserActivitySeparateRef != null && browserActivitySeparateRef.get() != null)
            browserActivitySeparateRef.get().finishAndRemoveTask();
    }

    // ______All functions
    // calls the specified function on all launcher activities
    // if a launcher activity is inactive and unable to respond (rare), it will lose it's stored view
    public void refreshInterfaceAll() {
        for (LauncherActivity activity: activityByIndex.keySet()) activity.refreshInterface();
        clearViewsWithoutActiveActivities();
    }
    public void refreshAppDisplayListsAll() {
        for (LauncherActivity activity: activityByIndex.keySet()) activity.refreshAppDisplayLists();
        clearViewsWithoutActiveActivities();
    }
    public void refreshBackgroundAll() {
        for (LauncherActivity activity: activityByIndex.keySet()) activity.refreshBackground();
        clearViewsWithoutActiveActivities();
    }
    public void clearAdapterCachesAll() {
        for (LauncherActivity activity: activityByIndex.keySet()) activity.clearAdapterCaches();
        clearViewsWithoutActiveActivities();
    }
    // Clear the views & finish the activities of any activities which are currently inactive
    // This will force any applicable activities to be restarted with brand new views
    private void clearViewsWithoutActiveActivities() {
        for (int index: viewByIndex.keySet())
            if (!activityByIndex.containsValue(index)) {
                viewByIndex.remove(index);
                Log.v("LauncherService", "Removed inactive view with index: "+index);
            } else {
                LauncherActivity activity = keyByValue(activityByIndex, index);
                if (activity == null) continue;
                if (activity.isKillable) activity.finish();
            }
    }
    /** @noinspection SameParameterValue*/
    static <T, E> T keyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet())
            if (Objects.equals(value, entry.getValue()))
                return entry.getKey();
        return null;
    }
}
