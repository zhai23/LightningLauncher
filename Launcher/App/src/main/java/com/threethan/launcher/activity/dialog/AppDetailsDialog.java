package com.threethan.launcher.activity.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.threethan.launcher.R;
import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launcher.activity.support.SettingsManager;
import com.threethan.launcher.data.Settings;
import com.threethan.launcher.helper.AppExt;
import com.threethan.launcher.helper.LaunchExt;
import com.threethan.launchercore.icon.IconLoader;
import com.threethan.launchercore.icon.IconUpdater;
import com.threethan.launchercore.lib.ImageLib;
import com.threethan.launchercore.lib.StringLib;
import com.threethan.launchercore.util.App;
import com.threethan.launchercore.util.Platform;

import java.io.File;
import java.util.Objects;

/**
 * Provides the dialog which appears when pressing the three-dots icon on an app,
 * or when long-pressing an app in edit mode
 */
public class AppDetailsDialog extends BasicDialog<LauncherActivity> {
    private static File customIconFile;
    private static ApplicationInfo imageApp;
    private final ApplicationInfo currentApp;

    /**
     * Constructs a new GroupDetailsDialog. Make sure to call .show()!
     * @param launcherActivity Parent activity
     * @param app App to show details of
     */
    public AppDetailsDialog(LauncherActivity launcherActivity, ApplicationInfo app) {
        super(launcherActivity, R.layout.dialog_details_app);
        this.currentApp = app;
    }

    @SuppressLint("SetTextI18n")
    public AlertDialog show() {
        // Set View
        AlertDialog dialog = super.show();
        if (dialog == null) return null;
        // Package Name
        ((TextView) dialog.findViewById(R.id.packageName)).setText(currentApp.packageName);

        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(currentApp.packageName, 0);
            ((TextView) dialog.findViewById(R.id.packageVersion)).setText("v"+packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException ignored) {}
        // Info Action
        dialog.findViewById(R.id.info).setOnClickListener(view -> AppExt.openInfo(context, currentApp.packageName));
        dialog.findViewById(R.id.uninstall).setOnClickListener(view -> {
            AppExt.uninstall(currentApp.packageName); dialog.dismiss();});

        // Launch Mode Toggle
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        final View launchButton = dialog.findViewById(R.id.launch);
        final View refreshIconButton = dialog.findViewById(R.id.refreshIcon);
        final View tuningButton = dialog.findViewById(R.id.tuningButton);

        final Spinner launchSizeSpinner = dialog.findViewById(R.id.launchSizeSpinner);
        final Spinner launchBrowserSpinner = dialog.findViewById(R.id.launchBrowserSpinner);

        // Load Icon
        ImageView iconImageView = dialog.findViewById(R.id.appIcon);
        IconLoader.loadIcon(currentApp, drawable -> {
            if (getActivityContext() == null) return;
            getActivityContext().runOnUiThread(() ->
                    iconImageView.setImageDrawable(drawable)
            );
        });

        iconImageView.setClipToOutline(true);
        if (App.isBanner(currentApp)) iconImageView.getLayoutParams().width = context.dp(150);

        iconImageView.setOnClickListener(iconPickerView -> {
            customIconFile = IconLoader.iconCustomFileForApp(currentApp);
            if (customIconFile.exists()) //noinspection ResultOfMethodCallIgnored
                customIconFile.delete();
            context.setSelectedIconImage(iconImageView);

            imageApp = currentApp;
            context.showImagePicker(LauncherActivity.ImagePickerTarget.ICON);
        });

        App.Type appType = AppExt.getType(currentApp);
        dialog.findViewById(R.id.info).setVisibility(currentApp.packageName.contains("://")
                ? View.GONE : View.VISIBLE);
        if (appType == App.Type.VR || appType == App.Type.PANEL
                || Platform.isTv()) {
            // VR apps MUST launch out, so just hide the option and replace it with another
            // Also hide it on TV where it is useless

            launchButton.setVisibility(View.GONE);
            refreshIconButton.setVisibility(View.VISIBLE);
            refreshIconButton.setOnClickListener(view -> {
                IconLoader.cachedIcons.remove(currentApp.packageName);
                IconUpdater.nextCheckByPackageMs.remove(currentApp.packageName);
                IconLoader.loadIcon(currentApp, d
                        -> context.runOnUiThread(() -> iconImageView.setImageDrawable(d)));
            });

            launchSizeSpinner.setVisibility(View.GONE);
        } else {
            tuningButton.setVisibility(View.GONE);

            launchButton.setVisibility(View.VISIBLE);
            launchButton.setOnClickListener((view) -> LaunchExt.launchApp(context, currentApp));

            launchSizeSpinner.setVisibility(Platform.isVr() ? View.VISIBLE : View.GONE);

            // Browser selection spinner
            if (appType == App.Type.WEB) {
                final String launchBrowserKey = Settings.KEY_LAUNCH_BROWSER + currentApp.packageName;
                final int launchBrowserSelection = context.dataStoreEditor.getInt(
                        launchBrowserKey,
                        SettingsManager.getAppLaunchOut(currentApp.packageName) ? 0 : 1);
                launchBrowserSpinner.setSelection(launchBrowserSelection);
                initSpinner(launchBrowserSpinner,
                        Platform.isQuest()
                                ? R.array.advanced_launch_browsers_quest
                                : R.array.advanced_launch_browsers,
                        p -> context.dataStoreEditor.putInt(launchBrowserKey, p));
                launchBrowserSpinner.setVisibility(View.VISIBLE);
                launchSizeSpinner.setVisibility(View.GONE);
            } else {
                final String launchSizeKey = Settings.KEY_LAUNCH_SIZE + currentApp.packageName;
                final int launchSizeSelection = context.dataStoreEditor.getInt(
                        launchSizeKey,
                        SettingsManager.getAppLaunchOut(currentApp.packageName) ? 0 : 1);
                initSpinner(launchSizeSpinner, R.array.advanced_launch_sizes, p ->
                        context.dataStoreEditor.putInt(launchSizeKey, p)
                );
                launchSizeSpinner.setSelection(launchSizeSelection);
                launchSizeSpinner.setVisibility(View.VISIBLE);
            }
        }

        // Show/hide button
        final View showButton = dialog.findViewById(R.id.show);
        final View hideButton = dialog.findViewById(R.id.hide);
        String unhideGroup = SettingsManager.getAppGroupMap().get(currentApp.packageName);
        if (Objects.equals(unhideGroup, Settings.HIDDEN_GROUP))
            unhideGroup = AppExt.getDefaultGroupFor(AppExt.getType(currentApp));
        if (Objects.equals(unhideGroup, Settings.HIDDEN_GROUP))
            try {
                unhideGroup = (String) SettingsManager.getAppGroups().toArray()[0];
            } catch (AssertionError | IndexOutOfBoundsException ignored) {
                unhideGroup = Settings.HIDDEN_GROUP;
                BasicDialog.toast("Could not find a group to unhide app to!");
            }
        String finalUnhideGroup = unhideGroup;

        boolean amHidden = Objects.equals(SettingsManager.getAppGroupMap()
                .get(currentApp.packageName), Settings.HIDDEN_GROUP);
        showButton.setVisibility( amHidden ? View.VISIBLE : View.GONE);
        hideButton.setVisibility(!amHidden ? View.VISIBLE : View.GONE);
        showButton.setOnClickListener(v -> {
            context.settingsManager.setAppGroup(currentApp.packageName,
                    finalUnhideGroup);
            boolean nowHidden = Objects.equals(SettingsManager.getAppGroupMap()
                    .get(currentApp.packageName), Settings.HIDDEN_GROUP);
            showButton.setVisibility( nowHidden ? View.VISIBLE : View.GONE);
            hideButton.setVisibility(!nowHidden ? View.VISIBLE : View.GONE);
            BasicDialog.toast(context.getString(R.string.moved_shown), finalUnhideGroup, false);
            context.launcherService.forEachActivity(LauncherActivity::refreshAppList);

        });
        hideButton.setOnClickListener(v -> {
            context.settingsManager.setAppGroup(currentApp.packageName,
                    Settings.HIDDEN_GROUP);
            boolean nowHidden = Objects.equals(SettingsManager.getAppGroupMap()
                    .get(currentApp.packageName), Settings.HIDDEN_GROUP);
            showButton.setVisibility( nowHidden ? View.VISIBLE : View.GONE);
            hideButton.setVisibility(!nowHidden ? View.VISIBLE : View.GONE);
            BasicDialog.toast(context.getString(R.string.moved_hidden),
                    context.getString(R.string.moved_hidden_bold), false);
            context.launcherService.forEachActivity(LauncherActivity::refreshAppList);
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
        dialog.findViewById(R.id.install).setOnClickListener(view -> {
            SettingsManager.setAppLabel(currentApp, StringLib.setStarred(appNameEditText.getText().toString(), isStarred[0]));
            context.launcherService.forEachActivity(a -> {
                if (a.getAppAdapter() != null) {
                    a.getAppAdapter().notifyItemChanged(currentApp);
                    a.refreshAppList();
                }
            });
            dialog.dismiss();
        });
        return dialog;
    }
    public static void onImageSelected(@NonNull Bitmap bitmap,
                                       ImageView selectedImageView, LauncherActivity launcherActivity) {
        bitmap = ImageLib.getResizedBitmap(bitmap, 450);
        ImageLib.saveBitmap(bitmap, customIconFile);
        selectedImageView.setImageBitmap(bitmap);

        IconLoader.cachedIcons.remove(IconLoader.cacheName(imageApp));
        launcherActivity.launcherService.forEachActivity(a -> {
            if (a.getAppAdapter() != null) a.getAppAdapter().notifyItemChanged(imageApp);
        });
    }
}
