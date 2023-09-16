package com.threethan.launcher.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.support.Updater;

import java.lang.ref.WeakReference;
import java.util.Objects;

public abstract class AddonDialog {
    private static WeakReference<Updater> updaterRef;
    private static WeakReference<LauncherActivity> activityRef;
    private static final String EXPLORE_PACKAGE = "com.oculus.explore";
    public static void showAddons(LauncherActivity a) {
        AlertDialog dialog = Dialog.build(a, R.layout.dialog_addons);
        activityRef = new WeakReference<>(a);

        View addonMessenger = dialog.findViewById(R.id.addonMessenger);
        updateAddonButton(a, addonMessenger, Updater.TAG_MESSENGER_SHORTCUT);

        View addonExplore = dialog.findViewById(R.id.addonExplore);
        updateAddonButton(a, addonExplore, Updater.TAG_EXPLORE_SHORTCUT);

        View addonLibrary = dialog.findViewById(R.id.addonLibrary);
        updateAddonButton(a, addonLibrary, Updater.TAG_LIBRARY_SHORTCUT);

        dialog.findViewById(R.id.exitButton).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.disableExplore).setOnClickListener(v -> {
            App.openInfo(a, EXPLORE_PACKAGE);
        });
    }
    public static void updateAddonButton(final Activity a, final View outerView, final String tag) {
        final View uninstallButton = outerView.findViewById(R.id.addonUninstall);
        final View installButton = outerView.findViewById(R.id.addonInstall);
        final View updateButton = outerView.findViewById(R.id.addonUpdate);
        final View activateButton = outerView.findViewById(R.id.addonActivate);

        View icon = outerView.findViewById(R.id.icon);
        icon.setClipToOutline(true);
        Runnable updateButtonRunnable = new Runnable() {
            @Override
            public void run() {
                uninstallButton.setVisibility(View.GONE);
                installButton.setVisibility(View.GONE);
                updateButton.setVisibility(View.GONE);
                activateButton.setVisibility(View.GONE);

                Updater updater = getUpdater();
                if (updater == null) return;

                switch (updater.getAddonState(tag)) {
                    case Updater.STATE_ACTIVE:
                        uninstallButton.setVisibility(View.VISIBLE);
                        break;
                    case Updater.STATE_NOT_INSTALLED:
                        installButton.setVisibility(View.VISIBLE);
                        break;
                    case Updater.STATE_HAS_UPDATE:
                        updateButton.setVisibility(View.VISIBLE);
                        break;
                    case Updater.STATE_INACTIVE:
                        activateButton.setVisibility(View.VISIBLE);
                        break;
                }
                try {
                    outerView.postDelayed(this, 100);
                } catch (Exception ignored) {}
            }
        };
        outerView.post(updateButtonRunnable);

        uninstallButton.setOnClickListener((v -> Objects.requireNonNull(getUpdater()).uninstallAddon(a, tag)));
        installButton.setOnClickListener((v -> Objects.requireNonNull(getUpdater()).installAddon(tag)));
        updateButton.setOnClickListener((v -> Objects.requireNonNull(getUpdater()).installAddon(tag)));
        activateButton.setOnClickListener((v -> showAccessibilityDialog()));
    }
    @Nullable
    protected static Updater getUpdater() {
        Updater updater = null;
        if (updaterRef != null) updater = updaterRef.get();
        if (updater != null) return updater;
        LauncherActivity activity = activityRef.get();
        if (activity != null) {
            updater = new Updater(activity);
            updaterRef = new WeakReference<>(updater);
            return updater;
        }
        return null;
    }

    protected static void showAccessibilityDialog() {
        Activity a = activityRef.get();
        if (a==null) return;
        AlertDialog subDialog = Dialog.build(a, R.layout.dialog_service_info);
        subDialog.findViewById(R.id.confirm).setOnClickListener(view1 -> {
            // Navigate to accessibility settings
            Intent localIntent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
            localIntent.setPackage("com.android.settings");
            a.startActivity(localIntent);
        });
    }
}
