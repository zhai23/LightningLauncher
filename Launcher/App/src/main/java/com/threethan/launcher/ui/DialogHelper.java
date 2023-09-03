package com.threethan.launcher.ui;

import android.app.AlertDialog;
import android.content.Context;

import com.threethan.launcher.R;

import java.util.Objects;

public class DialogHelper {
    public static AlertDialog build(Context context, int resource) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.dialog).setView(resource).create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.bkg_dialog);
        dialog.getWindow().setDimAmount(0.15f);

        dialog.show();
        return dialog;
    }
}
