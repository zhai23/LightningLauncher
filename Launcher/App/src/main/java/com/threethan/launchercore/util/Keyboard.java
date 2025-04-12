package com.threethan.launchercore.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Used to show or hide the soft keyboard, and nothing else
 */
public abstract class Keyboard {
    /** Show the soft keyboard */
    public static void show(View anyView) {
        InputMethodManager imm = (InputMethodManager) anyView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(anyView, InputMethodManager.SHOW_FORCED);
    }
    /** Hide the soft keyboard */
    public static void hide(View anyView) {
        InputMethodManager imm = (InputMethodManager) anyView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(anyView.getWindowToken(),0);
    }
}
