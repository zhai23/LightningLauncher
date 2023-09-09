package com.threethan.launcher.browser;

import android.os.Bundle;
import android.view.View;

import com.threethan.launcher.R;

// This class is used by browser activities that run in a separate window
public class BrowserActivitySeparate extends BrowserActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View exit = findViewById(R.id.exit);
        exit.setVisibility(View.GONE);
    }
}
