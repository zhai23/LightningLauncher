package com.threethan.launcher.support;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Compat;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Icon;
import com.threethan.launcher.helper.Launch;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.lib.StringLib;

import java.io.File;
import java.util.Objects;

public abstract class AppDetailsDialog {
    private static Drawable iconDrawable;
    private static File customIconFile;
    private static ApplicationInfo imageApp;

    @SuppressLint("SetTextI18n")
    public static void showAppDetails(ApplicationInfo currentApp, LauncherActivity launcherActivity) {
        // Set View
        AlertDialog dialog = Dialog.build(launcherActivity, R.layout.dialog_app_details);
        if (dialog == null) return;
        // Package Name
        ((TextView) dialog.findViewById(R.id.packageName)).setText(currentApp.packageName);

        PackageInfo packageInfo;
        try {
            packageInfo = launcherActivity.getPackageManager().getPackageInfo(currentApp.packageName, 0);
            ((TextView) dialog.findViewById(R.id.packageVersion)).setText("v"+packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException ignored) {}
        // Info Action
        dialog.findViewById(R.id.info).setOnClickListener(view -> App.openInfo(launcherActivity, currentApp.packageName));
        dialog.findViewById(R.id.uninstall).setOnClickListener(view -> {
            App.uninstall(launcherActivity, currentApp.packageName); dialog.dismiss();});


        dialog.findViewById(R.id.kill).setVisibility(
                SettingsManager.getRunning(currentApp.packageName) ? View.VISIBLE : View.GONE);
        dialog.findViewById(R.id.kill).setOnClickListener((view) -> {
            SettingsManager.stopRunning(currentApp.packageName);
            view.setVisibility(View.GONE);
        });


        // Launch Mode Toggle
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        final Switch launchModeSwitch = dialog.findViewById(R.id.launchModeSwitch);
        final View launchOutButton = dialog.findViewById(R.id.launchOut);
        final View launchModeSection = dialog.findViewById(R.id.launchModeSection);
        final View refreshIconButton = dialog.findViewById(R.id.refreshIconButton);

        // Launch Mode Selection
        final View launchSizeSpinner = dialog.findViewById(R.id.launchSizeSpinner);
        final TextView launchSizeSpinnerText = dialog.findViewById(R.id.launchSizeSpinnerText);
        // Launch Browser Selection
        final View launchBrowserSpinner = dialog.findViewById(R.id.launchBrowserSpinner);
        final TextView launchBrowserSpinnerText = dialog.findViewById(R.id.launchBrowserSpinnerText);

        // Load Icon
        PackageManager packageManager = launcherActivity.getPackageManager();
        ImageView iconImageView = dialog.findViewById(R.id.appIcon);
        iconImageView.setImageDrawable(Icon.loadIcon(launcherActivity, currentApp, null));

        iconImageView.setClipToOutline(true);
        if (App.isBanner(currentApp)) iconImageView.getLayoutParams().width = launcherActivity.dp(150);

        iconImageView.setOnClickListener(iconPickerView -> {
            iconDrawable = currentApp.loadIcon(packageManager);

            customIconFile = Icon.iconCustomFileForApp(launcherActivity, currentApp);
            if (customIconFile.exists()) //noinspection ResultOfMethodCallIgnored
                customIconFile.delete();
            launcherActivity.setSelectedIconImage(iconImageView, currentApp.packageName);

            imageApp = currentApp;
            ImageLib.showImagePicker(launcherActivity, Settings.PICK_ICON_CODE);
        });

        App.Type appType = App.getType(launcherActivity, currentApp);
        dialog.findViewById(R.id.info).setVisibility(currentApp.packageName.contains("://")
                ? View.GONE : View.VISIBLE);
        if (appType == App.Type.TYPE_VR || appType == App.Type.TYPE_PANEL
                || Platform.isTv(launcherActivity)) {
            // VR apps MUST launch out, so just hide the option and replace it with another
            // Also hide it on TV where it is useless
            launchModeSection.setVisibility(View.GONE);
            refreshIconButton.setVisibility(View.VISIBLE);
            launchOutButton.setVisibility(View.GONE);

            refreshIconButton.setOnClickListener(view -> Icon.reloadIcon(launcherActivity, currentApp, iconImageView));
        } else {
            launchModeSection.setVisibility(View.VISIBLE);
            refreshIconButton.setVisibility(View.GONE);

            launchOutButton.setVisibility(View.VISIBLE);
            launchOutButton.setOnClickListener((view) -> {
                final boolean prevLaunchOut = SettingsManager.getAppLaunchOut(currentApp.packageName);
                SettingsManager.setAppLaunchOut(currentApp.packageName, true);
                Launch.launchApp(launcherActivity, currentApp);
                SettingsManager.setAppLaunchOut(currentApp.packageName, prevLaunchOut);
            });

            // Normal size settings
            launchModeSwitch.setChecked(SettingsManager.getAppLaunchOut(currentApp.packageName));
            launchModeSwitch.setOnCheckedChangeListener((sw, value) -> {
                SettingsManager.setAppLaunchOut(currentApp.packageName, value);

                if (!launcherActivity.dataStoreEditor.getBoolean(Settings.KEY_SEEN_LAUNCH_OUT_POPUP, false)) {
                    AlertDialog subDialog = Dialog.build(launcherActivity, R.layout.dialog_launch_out_info);
                    if (subDialog == null) return;
                    subDialog.findViewById(R.id.confirm).setOnClickListener(view -> {
                        launcherActivity.dataStoreEditor
                                .putBoolean(Settings.KEY_SEEN_LAUNCH_OUT_POPUP, true);
                        subDialog.dismiss();
                    });
                }
            });

            // Browser settings
            if (appType == App.Type.TYPE_WEB && Platform.isQuest(launcherActivity)) {
                launchBrowserSpinner.setVisibility(View.VISIBLE);
                launchModeSection.setVisibility(View.GONE);

                final String launchBrowserKey = Settings.KEY_LAUNCH_BROWSER + currentApp.packageName;
                final int[] launchBrowserSelection = {launcherActivity.dataStoreEditor.getInt(
                        launchBrowserKey,
                        SettingsManager.getAppLaunchOut(currentApp.packageName) ? 0 : 1)};

                launchBrowserSpinnerText.setText(Settings.launchBrowserStrings[launchBrowserSelection[0]]);
                launchBrowserSpinner.setOnClickListener((view) -> {
                    launchBrowserSelection[0] = (launchBrowserSelection[0] + 1) % Settings.launchBrowserStrings.length;
                    launchBrowserSpinnerText.setText(Settings.launchBrowserStrings[launchBrowserSelection[0]]);
                    launcherActivity.dataStoreEditor.putInt(launchBrowserKey, launchBrowserSelection[0]);
                    SettingsManager.setAppLaunchOut(currentApp.packageName, launchBrowserSelection[0] != 0);
                });
            }
            // Advanced size settings
            else if (SettingsManager.getAdvancedLaunching(launcherActivity)) {
                launchSizeSpinner.setVisibility(View.VISIBLE);
                launchModeSection.setVisibility(View.GONE);
                final String launchSizeKey = Settings.KEY_LAUNCH_SIZE + currentApp.packageName;
                final int[] launchSizeSelection = {launcherActivity.dataStoreEditor.getInt(
                        launchSizeKey,
                        SettingsManager.getAppLaunchOut(currentApp.packageName) ? 0 : 1)};

                launchSizeSpinnerText.setText(Settings.launchSizeStrings[launchSizeSelection[0]]);
                launchSizeSpinner.setOnClickListener((view) -> {
                    launchSizeSelection[0] = (launchSizeSelection[0] + 1) % Settings.launchSizeStrings.length;
                    launchSizeSpinnerText.setText(Settings.launchSizeStrings[launchSizeSelection[0]]);
                    launcherActivity.dataStoreEditor.putInt(launchSizeKey, launchSizeSelection[0]);
                    SettingsManager.setAppLaunchOut(currentApp.packageName, launchSizeSelection[0] != 0);
                });
            }
        }

        // Show/hide button
        final View showButton = dialog.findViewById(R.id.show);
        final View hideButton = dialog.findViewById(R.id.hide);
        String unhideGroup = SettingsManager.getAppGroupMap().get(currentApp.packageName);
        if (Objects.equals(unhideGroup, Settings.HIDDEN_GROUP))
            unhideGroup = App.getDefaultGroupFor(App.getType(launcherActivity, currentApp));
        if (Objects.equals(unhideGroup, Settings.HIDDEN_GROUP))
            try {
                unhideGroup = (String) SettingsManager.getAppGroups().toArray()[0];
            } catch (AssertionError | IndexOutOfBoundsException ignored) {
                unhideGroup = Settings.HIDDEN_GROUP;
                Dialog.toast("Could not find a group to unhide app to!");
            }
        String finalUnhideGroup = unhideGroup;

        boolean amHidden = Objects.equals(SettingsManager.getAppGroupMap()
                .get(currentApp.packageName), Settings.HIDDEN_GROUP);
        showButton.setVisibility( amHidden ? View.VISIBLE : View.GONE);
        hideButton.setVisibility(!amHidden ? View.VISIBLE : View.GONE);
        showButton.setOnClickListener(v -> {
            launcherActivity.settingsManager.setAppGroup(currentApp.packageName,
                    finalUnhideGroup);
            boolean nowHidden = Objects.equals(SettingsManager.getAppGroupMap()
                    .get(currentApp.packageName), Settings.HIDDEN_GROUP);
            showButton.setVisibility( nowHidden ? View.VISIBLE : View.GONE);
            hideButton.setVisibility(!nowHidden ? View.VISIBLE : View.GONE);
            Dialog.toast(launcherActivity.getString(R.string.moved_shown), finalUnhideGroup, false);
        });
        hideButton.setOnClickListener(v -> {
            launcherActivity.settingsManager.setAppGroup(currentApp.packageName,
                    Settings.HIDDEN_GROUP);
            boolean nowHidden = Objects.equals(SettingsManager.getAppGroupMap()
                    .get(currentApp.packageName), Settings.HIDDEN_GROUP);
            showButton.setVisibility( nowHidden ? View.VISIBLE : View.GONE);
            hideButton.setVisibility(!nowHidden ? View.VISIBLE : View.GONE);
            Dialog.toast(launcherActivity.getString(R.string.moved_hidden),
                    launcherActivity.getString(R.string.moved_hidden_bold), false);

        });

        // Set Label (don't show star)
        String label = SettingsManager.getAppLabel(currentApp);
        final EditText appNameEditText = dialog.findViewById(R.id.appLabel);
        appNameEditText.setText(StringLib.withoutStar(label));
        // Star (actually changes label)
        final ImageView starButton = dialog.findViewById(R.id.star);
        final boolean[] isStarred = {StringLib.hasStar(label)};
        starButton.setImageResource(isStarred[0] ? R.drawable.ic_star_on : R.drawable.ic_star_off);
        starButton.setOnClickListener((view) -> {
            isStarred[0] = !isStarred[0];
            starButton.setImageResource(isStarred[0] ? R.drawable.ic_star_on : R.drawable.ic_star_off);
        });
        // Save Label & Reload on Confirm
        dialog.findViewById(R.id.confirm).setOnClickListener(view -> {
            SettingsManager.setAppLabel(currentApp, StringLib.setStarred(appNameEditText.getText().toString(), isStarred[0]));
            launcherActivity.getAppAdapter().notifyAppChanged(currentApp);
            dialog.dismiss();
        });
    }
    public static void onImageSelected(String path, ImageView selectedImageView, LauncherActivity launcherActivity) {
        Compat.clearIconCache(launcherActivity);
        if (path != null) {
            Bitmap bitmap = ImageLib.bitmapFromFile(launcherActivity, new File(path));
            if (bitmap == null) return;
            bitmap = ImageLib.getResizedBitmap(bitmap, 450);
            ImageLib.saveBitmap(bitmap, customIconFile);
            selectedImageView.setImageBitmap(bitmap);
        } else {
            selectedImageView.setImageDrawable(iconDrawable);
            Icon.updateIcon(customIconFile, imageApp, null);
            // No longer sets icon here but that should be fine
        }
    }
}
