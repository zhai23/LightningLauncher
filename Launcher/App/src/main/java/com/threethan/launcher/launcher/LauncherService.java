package com.threethan.launcher.launcher;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;

import java.util.Collections;
import java.util.HashSet;
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
    private static final Set<Activity> activities = Collections.synchronizedSet(new HashSet<>());
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public View getNewView(LauncherActivity activity) {
        View view = View.inflate(activity, R.layout.activity_main, null);
        viewByIndex.put(0, view);
        activities.add(activity);
        return view;
    }
    public View getExistingView(LauncherActivity activity) {
        View view = viewByIndex.get(0);

        assert view != null;
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) parent.removeView(view);

        activities.add(activity);
        return view;
    }

    public boolean hasView(String ignoredId) {
        return viewByIndex.containsKey(activities.size()-1);
    }
    public void destroyed(Activity activity) {
        activities.remove(activity);
    }

    public void finishAllActivities() {
        for (Activity activity:activities) activity.finishAndRemoveTask();
        Intent finishIntent = new Intent(LauncherActivity.FINISH_ACTION);
        sendBroadcast(finishIntent);
    }

    //TODO Make multitasking work properly
}
