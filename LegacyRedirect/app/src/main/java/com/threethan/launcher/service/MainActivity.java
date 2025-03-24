package com.threethan.launcher.service;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
    private void launch() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.threethan.launcher");
        if (launchIntent != null) launchIt(launchIntent);
    }
    private void launchIt(Intent launchIntent) {
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityFromChild(this, launchIntent, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launch();
    }
}
