package com.threethan.launcher.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.transition.Explode;
import android.transition.Slide;

import com.threethan.launcher.R;

import java.util.Objects;

public class DialogHelper {
    public static AlertDialog build(Context context, int resource) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.dialog).setView(resource).create();
//        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.bkg_dialog);
        dialog.getWindow().setDimAmount(0.1f);
        dialog.getWindow().setEnterTransition(new Explode());
        dialog.getWindow().setExitTransition(new Explode());

        dialog.show();
        return dialog;
    }
}
