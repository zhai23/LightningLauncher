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

import java.util.HashMap;
import java.util.HashSet;

public class LauncherService extends Service {
    private final IBinder binder = new LocalBinder();
    private final static HashMap<String, View> viewById = new HashMap<>();
    private final static HashSet<Activity> activities = new HashSet<>(); // Will include dead activities!

    public class LocalBinder extends Binder {
        public LauncherService getService() {
            return LauncherService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public View getView(LauncherActivity activity) {
        View view;
        final String id = activity.getId();
        if (hasView(id)) {
            view = viewById.get(id);

            assert view != null;
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) parent.removeView(view);
        } else {
            view = View.inflate(activity, R.layout.activity_main, null);
            viewById.put(id, view);
        }
        activities.add(activity);
        return view;
    }
    public boolean hasView(String id) {
        return viewById.containsKey(id);
    }

    public void finishAllActivities() {
        for (Activity activity : activities) {
            try {
                activity.finishAndRemoveTask();
            } catch (Exception ignored) {}
        }
        activities.clear();
        Intent finishIntent = new Intent(LauncherActivity.FINISH_ACTION);
        sendBroadcast(finishIntent);
    }
}
