package com.threethan.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class LauncherAccessibilityService extends AccessibilityService
{
    public void onAccessibilityEvent(AccessibilityEvent event)
    {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String eventText = event.getText().toString();
            String exploreAccessibilityEventName = getResources().getString(R.string.accessibility_event_name);

            if (exploreAccessibilityEventName.compareTo(eventText) == 0) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.threethan.launcher");

                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            }
        }
    }
    public void onInterrupt() {}
}