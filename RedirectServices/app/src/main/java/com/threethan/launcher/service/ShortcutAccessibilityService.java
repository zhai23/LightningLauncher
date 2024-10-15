package com.threethan.launcher.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Timer;
import java.util.TimerTask;

public class ShortcutAccessibilityService extends AccessibilityService {
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (!event.getPackageName().equals("com.oculus.systemux")) return;

        String eventText = event.getText().toString();

        String targetName = getResources().getString(R.string.target_name);

        if (eventText.compareTo("[]") == 0) {
            if (event.getContentDescription() == null) return;
            if (targetName.compareToIgnoreCase(event.getContentDescription().toString()) != 0) return;
        } else {
            String targetEventName = "[" + targetName + "]";
            if (targetEventName.toLowerCase().compareToIgnoreCase(eventText) != 0) return;
        }

        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        Log.i("LightningLauncherService", "Opening launcher activity from accessibility event");
        startActivity(launchIntent);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startActivity(launchIntent);
            }
        }, 650);
    }

    public void onInterrupt() {}
}