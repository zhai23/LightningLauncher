package com.threethan.launcher.helper;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.transition.Explode;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.threethan.launcher.R;

import java.util.List;

/*
    Dialog

    This provides a wrapper for AlertDialog.Builder that makes it even easier to create an alert
    dialog from a layout resource
 */
public abstract class Dialog {
    public static AlertDialog build(Context context, int resource) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.dialog).setView(resource).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
            dialog.getWindow().setDimAmount(0.2f);
            ObjectAnimator animator = ObjectAnimator.ofFloat(dialog.getWindow().getDecorView(), "TranslationY", 100, 0);
            animator.setDuration(300);
            animator.setInterpolator(new FastOutSlowInInterpolator());
            animator.start();
        }

        dialog.show();
        return dialog;
    }
}
