package com.threethan.launcher.launcher;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;

import java.util.HashMap;

public class LauncherService extends Service {
    private final IBinder binder = new LocalBinder();
    private final HashMap<String, View> viewById = new HashMap<>();
    private final HashMap<String, Activity> activityById = new HashMap<>();
    @Override
    public void onCreate() {
        super.onCreate();
    }

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
        final String id = activity.id;
        if (hasView(id)) {
            view = viewById.get(id);

            assert view != null;
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) parent.removeView(view);

            Activity owner = activityById.get(id);
            if (owner != null && owner != activity) {
                owner.finish();
                activityById.remove(id);
            }
        } else {
            view = View.inflate(activity, R.layout.activity_main, null);
            viewById.put(id, view);
            activityById.put(id, activity);
        }
        return view;
    }
    public boolean hasView(String id) {
        return viewById.containsKey(id);
    }
}
