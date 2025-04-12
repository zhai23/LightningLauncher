package com.threethan.launcher.activity.dialog;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.threethan.launcher.R;
import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launchercore.Core;
import com.threethan.launchercore.util.CustomDialog;
import com.threethan.launchercore.util.Platform;

import java.util.Objects;
import java.util.function.Consumer;

/*
    Dialog

    This provides a wrapper for AlertDialog.Builder that makes it even easier to create an alert
    dialog from a layout resource
 */
public class BasicDialog<T extends Context> extends AbstractDialog<T> {
    final int resource;

    /**
     * Constructs a new dialog from a context and resource.
     * Use .show() to show and return an AlertDialog.
     * @param context Context to show the dialog
     * @param resource Resource of the dialog's layout
     */
    public BasicDialog(T context, int resource) {
        super(context);
        this.resource = resource;
    }
    @Nullable
    public AlertDialog show() {
        AlertDialog dialog = new CustomDialog.Builder(a).setView(resource).create();

        if (dialog.getWindow() == null) return null;
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
        final View rootView = dialog.getWindow().getDecorView().findViewById(android.R.id.content).getRootView();
        rootView.setLayerType(View.LAYER_TYPE_HARDWARE, new Paint());

        ObjectAnimator animator = ObjectAnimator.ofFloat(rootView, "TranslationY", 100, 0);
        animator.setDuration(300);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.start();

        dialog.show();
        return dialog;
    }

    public static void toast(CharSequence string) {
        toast(string, "", false);
    }

    public static void toast(CharSequence stringMain, CharSequence stringBold, boolean isLong) {
        if (Core.context() == null) return;
        Log.d("Toast", stringMain + " " + stringBold);

        // Real toast doesn't block dpad input
        if (!Platform.isVr()) {
            Toast.makeText(Core.context() , stringMain + " " + stringBold,
                    (isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)).show();
            return;
        }

        try {
            LayoutInflater inflater =
                    Objects.requireNonNull(LauncherActivity.getForegroundInstance())
                            .getLayoutInflater();
            @SuppressLint("InflateParams")
            View layout = inflater.inflate(R.layout.dialog_toast, null);

            TextView textMain = layout.findViewById(R.id.toastTextMain);
            TextView textBold = layout.findViewById(R.id.toastTextBold);
            textMain.setText(stringMain);
            textBold.setText(stringBold);

            Toast toast = new Toast(LauncherActivity.getForegroundInstance());
            toast.setDuration(isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            Log.w("Toast", "Failed to show toast", e);
        }
    }

    public static void initSpinner(Spinner spinner, int array_res,
                                   Consumer<Integer> onPositionSelected, int initialSelection) {
        Context foregroundContext = LauncherActivity.getForegroundInstance();
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                foregroundContext != null ? foregroundContext : Core.context(),
                array_res, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onPositionSelected.accept(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (initialSelection < 0 || initialSelection >= adapter.getCount())
            Log.e("SettingsArray", "Invalid position "+initialSelection);
        else spinner.setSelection(initialSelection);
    }
}
