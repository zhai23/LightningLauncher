package com.threethan.launcher.helper;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;

import java.util.Objects;

public abstract class Dialog {
    @Nullable
    public static AlertDialog build(Context context, int resource) {
        try {
            AlertDialog dialog = new AlertDialog.Builder(context, R.style.dialog).setView(resource).create();
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.bkg_dialog);
            dialog.getWindow().setDimAmount(0.15f);

            dialog.show();
            return dialog;
        } catch (Exception ignored) {}
        return null;
    }
}
