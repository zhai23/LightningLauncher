package com.threethan.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Timer;
import java.util.TimerTask;

public class LauncherAccessibilityService extends AccessibilityService
{
    public void onAccessibilityEvent(AccessibilityEvent event)
    {
        String packageName = event.getPackageName().toString();

        Intent intent = new Intent(packageName.equals(getPackageName()) ? MainActivity.DONT_FINISH_ACTION: MainActivity.FINISH_ACTION);
        sendBroadcast(intent);

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String eventText = event.getText().toString();
            String exploreAccessibilityEventName = getResources().getString(R.string.accessibility_event_name);
            if (exploreAccessibilityEventName.compareTo(eventText) == 0) {

                Intent launchIntent = new Intent(this, MainActivity.class);

                launchIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                );
                Log.i("LightningLauncherService", "Opening launcher activity from accessibility event");
                startActivity(launchIntent);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.i("LightningLauncherService", "Opening launcher activity from accessibility event (delayed)");
                        startActivity(launchIntent);
                    }
                }, 800);}
        }
    }
    public void onInterrupt() {}
}