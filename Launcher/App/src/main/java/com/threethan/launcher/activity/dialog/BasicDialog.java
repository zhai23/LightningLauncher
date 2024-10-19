package com.threethan.launcher.activity.dialog;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.threethan.launcher.R;
import com.threethan.launchercore.util.Platform;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Consumer;

/*
    Dialog

    This provides a wrapper for AlertDialog.Builder that makes it even easier to create an alert
    dialog from a layout resource
 */
public class BasicDialog<T extends Context> extends AbstractDialog<T> {
    private static WeakReference<Activity> activityContextWeakReference;

    @Nullable
    public static Activity getActivityContext() {
        return activityContextWeakReference.get();
    }
    public static void setActivityContext(Activity activityContext) {
        activityContextWeakReference = new WeakReference<>(activityContext);
    }


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
        AlertDialog dialog = new AlertDialog.Builder(a, R.style.dialog).setView(resource).create();

        if (dialog.getWindow() == null) return null;
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
        dialog.getWindow().setDimAmount(0.3f);
        final View rootView = dialog.getWindow().getDecorView().findViewById(android.R.id.content).getRootView();
        rootView.setLayerType(View.LAYER_TYPE_HARDWARE, new Paint());

        ObjectAnimator animator = ObjectAnimator.ofFloat(rootView, "TranslationY", 100, 0);
        animator.setDuration(300);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.start();

        dialog.show();
        return dialog;
    }

    public static void toast(String string) {
        toast(string, "", false);
    }

    public static void toast(String stringMain, String stringBold, boolean isLong) {
        if (getActivityContext() == null) return;

        // Real toast doesn't block dpad input
        if (!Platform.isVr()) {
            Toast.makeText(getActivityContext() , stringMain + " " + stringBold,
                    (isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)).show();
            return;
        }

        // Fake toast for the Quest
        AlertDialog dialog = new AlertDialog.Builder(getActivityContext(), R.style.dialogToast).setView(R.layout.dialog_toast).create();
        try {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog_transparent);
                dialog.getWindow().setDimAmount(0.0f);
                dialog.show();
                ((TextView) dialog.findViewById(R.id.toastTextMain)).setText(stringMain);
                ((TextView) dialog.findViewById(R.id.toastTextBold)).setText(stringBold);

                // Dismiss if not done automatically
                dialog.findViewById(R.id.toastTextMain).postDelayed(dialog::dismiss,
                        isLong ? 5000 : 1750);
            }

        } catch (Exception ignored) {}
    }

    public static void initSpinner(Spinner spinner, int array_res,
                                   Consumer<Integer> onPositionSelected, int initialSelection) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                Objects.requireNonNull(getActivityContext()),
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
