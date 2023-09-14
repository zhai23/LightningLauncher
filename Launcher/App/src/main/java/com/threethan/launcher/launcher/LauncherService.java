package com.threethan.launcher.launcher;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;

import java.util.concurrent.ConcurrentHashMap;

public class LauncherService extends Service {
    private final IBinder binder = new LocalBinder();
    private final static ConcurrentHashMap<String, View> viewById = new ConcurrentHashMap<>();
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
    public View getNewView(LauncherActivity activity) {
        View view = View.inflate(activity, R.layout.activity_main, null);
        viewById.put(activity.getId(), view);
        return view;
    }
    public View getExistingView(LauncherActivity activity) {
        View view = viewById.get(activity.getId());

        assert view != null;
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) parent.removeView(view);

        return view;
    }

    public boolean hasView(String id) {
        return viewById.containsKey(id);
    }

    public void finishAllActivities() {
        Intent finishIntent = new Intent(LauncherActivity.FINISH_ACTION);
        sendBroadcast(finishIntent);
    }
}
