package com.threethan.launcher.browser;

import android.os.Bundle;
import android.view.View;

import com.threethan.launcher.R;

/*
    BrowserActivitySeparate

    This activity is the same as BrowserActivity, but it need to be technically different to open
    in a separate window.

    The only change is that the close button is hidden.
 */
public class BrowserActivitySeparate extends BrowserActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View exit = findViewById(R.id.exit);
        exit.setVisibility(View.GONE);
    }
}
