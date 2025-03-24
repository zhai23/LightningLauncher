package com.threethan.launcher.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Timer;
import java.util.TimerTask;

/** Listens for specific accessibility events and opens the launcher appropriately */
public class ShortcutAccessibilityService extends AccessibilityService {
    private static final int HOVER_DELAY_MS = 500;
    private static final int LAUNCH_COOLDOWN_MS = 250;

    /**
     * Times how long you continue to hover an icon.
     * If you hold for at least HOVER_DELAY_MS, then it will launch.
     * When you exit the hover, the timer is cleared.
     */
    private static Timer hoverHoldTimer = null;

    /**
     * Times how much time passes between launch requests.
     * At least LAUNCH_COOLDOWN_MS must pass for a new launch to occur,
     * and no new hover events will be recognized until it completes
     */
    private static Timer launchCooldownTimer = null;

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!event.getPackageName().equals("com.oculus.systemux")) return;

        String eventText = event.getText().toString();
        String targetName = getResources().getString(R.string.target_name);

        // Return if event does not contain expected text (in text or contentDescription)
        if (eventText.compareTo("[]") == 0) {
            if (event.getContentDescription() == null) return;
            if (targetName.compareToIgnoreCase(event.getContentDescription().toString()) != 0) return;
        } else {
            String targetEventName = "[" + targetName + "]";
            if (targetEventName.toLowerCase().compareToIgnoreCase(eventText) != 0) return;
        }

        // Now we know the event is valid, so handle it:
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER) {
            // Handle Hover Start
            if (hoverHoldTimer == null && launchCooldownTimer == null) {
                hoverHoldTimer = new Timer();
                hoverHoldTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        doLaunch();
                        try {launchCooldownTimer.cancel();} catch (Exception ignored) {}
                        launchCooldownTimer = new Timer();
                    }
                }, HOVER_DELAY_MS);
            }
        } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_HOVER_EXIT) {
            // Handle Hover End
            try {
                hoverHoldTimer.cancel();} catch (Exception ignored) {}
            hoverHoldTimer = null;
            if (launchCooldownTimer != null) setLaunchCooldownTimer();
        } else {
            // Handle click or window open (indicates a definitely intentional interaction)
            try {
                hoverHoldTimer.cancel();} catch (Exception ignored) {}
            if (launchCooldownTimer == null) {
                launchCooldownTimer = new Timer();
                setLaunchCooldownTimer();
                doLaunch();
            }
        }
    }

    /** Set a timer that, while active, prevents a new launch */
    private static void setLaunchCooldownTimer() {
        launchCooldownTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                launchCooldownTimer = null;
            }
        }, LAUNCH_COOLDOWN_MS);
    }

    /** Launches a wrapped instance of Lightning Launcher using the appropriate intent */
    private void doLaunch() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        Log.i("LightningLauncherService", "Opening launcher activity from accessibility event");
        startActivity(launchIntent);
    }

    public void onInterrupt() {}
}