package com.threethan.launcher.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

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
        }
        dialog.show();
        return dialog;
    }
}
