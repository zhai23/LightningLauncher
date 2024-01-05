package com.threethan.launcher.service.people;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class ShortcutAccessibilityService extends AccessibilityService {
    private final String[] alternateNames = new String[] {"[People]"};
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String eventTextLc = event.getText().toString().toLowerCase();
            String exploreAccessibilityEventName  = getResources().getString(R.string.accessibility_event_name);

            if (exploreAccessibilityEventName.toLowerCase().compareTo(eventTextLc) == 0 ||
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                            && Arrays.stream(alternateNames).anyMatch(s -> s.toLowerCase().compareTo(eventTextLc)==0)) {

                Intent launchIntent = new Intent(this, MainActivity.class);
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

                Log.i("LightningLauncherService", "Opening launcher activity from accessibility event");
                startActivity(launchIntent);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        startActivity(launchIntent);
                    }
                }, 650);
            }
        }
    }

    public void onInterrupt() {}
}