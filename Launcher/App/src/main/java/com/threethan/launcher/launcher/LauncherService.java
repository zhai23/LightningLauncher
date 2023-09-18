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
import com.threethan.launcher.lib.DataLib;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LauncherService extends Service {
    private final IBinder binder = new LocalBinder();
    private final static ConcurrentHashMap<Integer, View> viewByIndex = new ConcurrentHashMap<>();
    private static final String TAG = "LauncherService";
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
        clearViewsWithoutActiveActivities();
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
    }

    private final Set<View> needsRefresh = Collections.synchronizedSet(new HashSet<>());
    public void refreshInterfaceAll() {
        needsRefresh.addAll(viewByIndex.values());
        for (LauncherActivity activity: activityByIndex.keySet()) activity.refreshInterface();
        clearViewsWithoutActiveActivities();
    }
    public void refreshAllBackground() {
        needsRefresh.addAll(viewByIndex.values());
        for (LauncherActivity activity: activityByIndex.keySet()) activity.refreshBackground();
        clearViewsWithoutActiveActivities();
    }
    private void clearViewsWithoutActiveActivities() {
        for (int index: viewByIndex.keySet())
            if (!activityByIndex.containsValue(index)) {
                viewByIndex.remove(index);
                Log.v("LauncherService", "Removed inactive view at "+index);
            } else {
                LauncherActivity activity = DataLib.keyByValue(activityByIndex, index);
                if (activity == null) return;
                if (activity.isKillable) activity.finish();
            }
    }
}
