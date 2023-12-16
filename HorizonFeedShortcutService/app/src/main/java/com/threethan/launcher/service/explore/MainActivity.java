package com.threethan.launcher.service.explore;

import android.app.Activity;
import android.content.Intent;

public class MainActivity extends Activity {
    private boolean launch() {
        finish();
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.threethan.launcher");
        if (launchIntent != null) return launchIt(launchIntent);
        return false;
    }
    private boolean launchIt(Intent launchIntent) {
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        finish();
        startActivity(launchIntent);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        launch();
    }
}
