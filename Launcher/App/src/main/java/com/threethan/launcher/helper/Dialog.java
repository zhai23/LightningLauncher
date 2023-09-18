package com.threethan.launcher.helper;

import android.app.AlertDialog;
import android.content.Context;

import com.threethan.launcher.R;
public abstract class Dialog {
    public static AlertDialog build(Context context, int resource) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.dialog).setView(resource).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
            dialog.getWindow().setDimAmount(0.2f);
        }
        dialog.show();
        return dialog;
    }
}
