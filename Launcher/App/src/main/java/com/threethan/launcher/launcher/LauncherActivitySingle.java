package com.threethan.launcher.launcher;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.threethan.launcher.R;
import com.threethan.launcher.adapter.AppsAdapter;
import com.threethan.launcher.helper.Launch;
import com.threethan.launcher.view.EditTextWatched;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

/*
    LauncherActivitySingle

    This is a dummy class to allow for a more standard launcher-style intent on Android TV devices,
    and on normal Android devices if you want to use this as your normal launcher for some reason.
 */

public class LauncherActivitySingle extends LauncherActivityEditable { }
