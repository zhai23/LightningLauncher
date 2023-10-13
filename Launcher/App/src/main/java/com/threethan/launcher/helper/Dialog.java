package com.threethan.launcher.helper;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.threethan.launcher.R;

import java.lang.ref.WeakReference;

/*
    Dialog

    This provides a wrapper for AlertDialog.Builder that makes it even easier to create an alert
    dialog from a layout resource
 */
public abstract class Dialog {
    private static WeakReference<Activity> activityContextWeakReference;

    @Nullable
    public static Activity getActivityContext() {
        return activityContextWeakReference.get();
    }
    public static void setActivityContext(Activity activityContext) {
        activityContextWeakReference = new WeakReference<>(activityContext);
    }
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
    public static AlertDialog toast(String string) {
        return toast(string, "");
    }
    @Nullable
    public static AlertDialog toast(String stringMain, String stringBold) {
        if (getActivityContext() == null) return null;
        AlertDialog dialog = new AlertDialog.Builder(getActivityContext(), R.style.dialogToast).setView(R.layout.dialog_toast).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog_transparent);
            dialog.getWindow().setDimAmount(0.0f);
        }
        dialog.show();

        ((TextView) dialog.findViewById(R.id.toastTextMain)).setText(stringMain);
        ((TextView) dialog.findViewById(R.id.toastTextBold)).setText(stringBold);

        // Dismiss if not done automatically
        dialog.findViewById(R.id.toastTextMain).postDelayed(dialog::dismiss, 5000);
        return dialog;
    }
}
