package com.threethan.launcher.helper;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Used to show or hide the soft keyboard, and nothing else
 */
public abstract class Keyboard {
    /** Show the soft keyboard */
    public static void show(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
    /** Hide the soft keyboard */
    public static void hide(Activity context, View anyView) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(anyView.getWindowToken(),0);
    }
}
