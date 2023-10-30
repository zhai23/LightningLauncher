package com.facebook.facebookvr;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {
    private boolean openedWeb = false;
    private boolean launch() {
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
        if (!launch() && !openedWeb) {
            openedWeb = true;
            final String downloadUrl = "https://github.com/threethan/LightningLauncher/releases/";
            Intent browserIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(downloadUrl)
            );
            startActivity(browserIntent);
        }
    }
}
