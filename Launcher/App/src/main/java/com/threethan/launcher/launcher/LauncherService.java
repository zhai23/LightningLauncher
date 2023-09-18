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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LauncherService extends Service {
    private final IBinder binder = new LocalBinder();
    private final static ConcurrentHashMap<Integer, View> viewByIndex = new ConcurrentHashMap<>();
    public class LocalBinder extends Binder {
        public LauncherService getService() {
            return LauncherService.this;
        }
    }
    private static final Map<LauncherActivity, Integer> indexByActivity = Collections.synchronizedMap(new HashMap<>());
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public View getNewView(LauncherActivity activity) {
        final int index = getNewActivityIndex();

        View view = View.inflate(activity, R.layout.activity_main, null);
        viewByIndex.put(index, view);
        indexByActivity.put(activity, index);

        return view;
    }
    public View getExistingView(LauncherActivity activity) {
        final int index = getNewActivityIndex();
        View view = viewByIndex.get(index);

        assert view != null;
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) parent.removeView(view);

        indexByActivity.put(activity, index);

        return view;
    }
    public boolean checkForExistingView() {
        Log.v("LauncherService", indexByActivity.toString());

        for (LauncherActivity activity: indexByActivity.keySet()) {
            if (activity.canBeKilled) {
                activity.finishAndRemoveTask();
                Log.i("LauncherService", "Killed backgrounded activity "+activity);
            }
        }
        return viewByIndex.containsKey(getNewActivityIndex());
    }
    protected int getNewActivityIndex() {
        int i = 0;
        while(indexByActivity.containsValue(i)) i++;
        Log.v("LauncherService", "Queried new index, got "+String.valueOf(i));
        return i;
    }
    public void destroyed(LauncherActivity activity) {
        indexByActivity.remove(activity);
    }

    public void finishAllActivities() {
        for (Activity activity: indexByActivity.keySet()) activity.finishAndRemoveTask();
        Intent finishIntent = new Intent(LauncherActivity.FINISH_ACTION);
        sendBroadcast(finishIntent);
    }

    private final Set<View> needsRefresh = Collections.synchronizedSet(new HashSet<>());
    public void refreshInterfaceAll() {
        needsRefresh.addAll(viewByIndex.values());
        for (LauncherActivity activity: indexByActivity.keySet()) activity.refreshInterface();
        clearViewsWithoutActivities();
    }
    public void refreshAllBackground() {
        needsRefresh.addAll(viewByIndex.values());
        for (LauncherActivity activity: indexByActivity.keySet()) activity.refreshBackground();
        clearViewsWithoutActivities();
    }
    private void clearViewsWithoutActivities() {
        for (int index: viewByIndex.keySet())
            if (!indexByActivity.containsValue(index)) {
                viewByIndex.remove(index);
                Log.v("LauncherService", "Removed inactive view at "+index);
            }
    }

    // If any shortcut task is open when this package is updated/reinstalled,
    // those shortcut activities will retain their attached views -
    // but all the service's variables will be reset...
    // which means that the service may give that view to something else, not realizing its used
    // This function checks if that's the case.
    // If it is, that activity should start from scratch with a new view.
    public boolean hasRegisteredView(LauncherActivity activity) {
        return indexByActivity.containsKey(activity);
    }
}
