package com.threethan.launcher.support;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.AppData;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.updater.AddonUpdater;

import java.lang.ref.WeakReference;
import java.util.Objects;

/*
    AddonDialog

    This class handles the addon manager, AKA shortcut settings
    It handles related popups, and updates buttons according to the state returned by Addon
 */
public abstract class AddonDialog {
    private static WeakReference<AddonUpdater> updaterRef;
    private static WeakReference<LauncherActivity> activityRef;
    public static void showShortcutAddons(LauncherActivity a) {
        AlertDialog dialog = Dialog.build(a, Platform.isVr(a) ? R.layout.dialog_addons_vr : R.layout.dialog_addons_tv);
        if (dialog == null) return;
        activityRef = new WeakReference<>(a);

        View addonFacebook = dialog.findViewById(R.id.addonFacebook);
        if (addonFacebook!=null) updateAddonButton(a, addonFacebook, AddonUpdater.TAG_FACEBOOK);

        View addonMonday = dialog.findViewById(R.id.addonMonday);
        if (addonMonday!=null) updateAddonButton(a, addonMonday, AddonUpdater.TAG_MONDAY);

        View addonExplore = dialog.findViewById(R.id.addonExplore);
        if (addonExplore!=null) {
            updateAddonButton(a, addonExplore, AddonUpdater.TAG_FEED);
            dialog.findViewById(R.id.disableExplore).setOnClickListener(v -> App.openInfo(a, AppData.EXPLORE_PACKAGE));
            ((TextView) dialog.findViewById(R.id.disableExploreWhy)).setText(
                    App.isPackageEnabled(a, AppData.EXPLORE_PACKAGE) ?
                            R.string.addons_explore_disable_why : R.string.addons_explore_enable_why);
            ((TextView) dialog.findViewById(R.id.disableExploreText)).setText(
                    App.isPackageEnabled(a, AppData.EXPLORE_PACKAGE) ?
                    R.string.addons_explore_disable : R.string.addons_explore_enable);
        }

        View addonLibrary = dialog.findViewById(R.id.addonLibrary);
        if (addonLibrary!=null) updateAddonButton(a, addonLibrary, AddonUpdater.TAG_LIBRARY);

        View addonPeople = dialog.findViewById(R.id.addonPeople);
        if (addonLibrary!=null) updateAddonButton(a, addonPeople, AddonUpdater.TAG_PEOPLE);

        View addonAndroidTv = dialog.findViewById(R.id.addonAndroidTv);
        if (addonAndroidTv!=null) updateAddonButton(a, addonAndroidTv, AddonUpdater.TAG_ATV_LM);

        dialog.findViewById(R.id.exitButton).setOnClickListener(v -> dialog.dismiss());
    }
    public static void updateAddonButton(final Activity a, final View outerView, final String tag) {
        final View uninstallButton = outerView.findViewById(R.id.addonUninstall);
        final View installButton = outerView.findViewById(R.id.addonInstall);
        final View updateButton = outerView.findViewById(R.id.addonUpdate);
        final View activateButton = outerView.findViewById(R.id.addonActivate);

        View icon = outerView.findViewById(R.id.icon);
        if (icon != null) icon.setClipToOutline(true);
        Runnable updateButtonRunnable = new Runnable() {
            @Override
            public void run() {
                uninstallButton.setVisibility(View.GONE);
                installButton.setVisibility(View.GONE);
                updateButton.setVisibility(View.GONE);
                activateButton.setVisibility(View.GONE);

                AddonUpdater updater = getUpdater();
                if (updater == null) return;

                switch (updater.getAddonState(updater.getAddon(tag))) {
                    case INSTALLED_SERVICE_INACTIVE -> activateButton.setVisibility(View.VISIBLE);
                    case INSTALLED_ACTIVE -> uninstallButton.setVisibility(View.VISIBLE);
                    case INSTALLED_HAS_UPDATE       -> updateButton.setVisibility(View.VISIBLE);
                    case NOT_INSTALLED              -> installButton.setVisibility(View.VISIBLE);

                }
                outerView.postDelayed(this, 100);
            }
        };
        outerView.post(updateButtonRunnable);

        uninstallButton.setOnClickListener((v -> Objects.requireNonNull(getUpdater()).uninstallAddon(a, tag)));
        installButton.setOnClickListener((v -> Objects.requireNonNull(getUpdater()).installAddon(tag)));
        updateButton.setOnClickListener((v -> Objects.requireNonNull(getUpdater()).installAddon(tag)));
        activateButton.setOnClickListener((v -> showAccessibilityDialog()));
    }
    @Nullable
    protected static AddonUpdater getUpdater() {
        AddonUpdater updater = null;
        if (updaterRef != null) updater = updaterRef.get();
        if (updater != null) return updater;
        LauncherActivity activity = activityRef.get();
        if (activity != null) {
            updater = new AddonUpdater(activity);
            updaterRef = new WeakReference<>(updater);
            return updater;
        }
        return null;
    }

    protected static void showAccessibilityDialog() {
        Activity a = activityRef.get();
        if (a==null) return;
        AlertDialog subDialog = Dialog.build(a, R.layout.dialog_info_service);
        if (subDialog == null) return;
        subDialog.findViewById(R.id.confirm).setOnClickListener(view1 -> {
            // Navigate to accessibility settings
            Intent localIntent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
            localIntent.setPackage("com.android.settings");
            a.startActivity(localIntent);
        });
    }
}
