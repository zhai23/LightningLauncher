package com.threethan.launcher.support;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Build;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Compat;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.ImageLib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/*
    SettingsDialog

    This class handles the main settings page, including setting/getting preferences, button states,
    and making sure settings are correctly applied to launcher activities
 */

public abstract class SettingsDialog {
    private static boolean clearedLabel;
    private static boolean clearedIcon;
    private static boolean clearedSort;
    private static boolean clearedGroups;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    public static void showSettings(LauncherActivity a) {
        AlertDialog dialog = Dialog.build(a, R.layout.dialog_settings);

        if (dialog == null) return;
        a.settingsVisible = true;

        dialog.setOnDismissListener(dialogInterface -> a.settingsVisible = false);

        // Addons
        View addonsButton = dialog.findViewById(R.id.addonsButton);
        addonsButton.setOnClickListener(view -> AddonDialog.showAddons(a));
        if (!a.sharedPreferences.getBoolean(Settings.KEY_SEEN_ADDONS, false)) {
            View addonsButtonAttract = dialog.findViewById(R.id.addonsButtonAttract);
            addonsButtonAttract.setVisibility(View.VISIBLE);
            addonsButton.setVisibility(View.GONE);
            addonsButtonAttract.setOnClickListener(view -> {
                a.sharedPreferenceEditor.putBoolean(Settings.KEY_SEEN_ADDONS, true).apply();
                AddonDialog.showAddons(a);
            });
        }

        // Edit
        Switch editSwitch = dialog.findViewById(R.id.editModeSwitch);
        editSwitch.setOnClickListener(view1 -> {
            a.setEditMode(!a.isEditing());
            ArrayList<String> selectedGroups = a.settingsManager.getAppGroupsSorted(true);
            if (a.isEditing() && (selectedGroups.size() > 1)) {
                Set<String> selectFirst = new HashSet<>();
                selectFirst.add(selectedGroups.get(0));
                a.settingsManager.setSelectedGroups(selectFirst);
            }
        });

        View addWebsite = dialog.findViewById(R.id.addWebsiteButton);
        addWebsite.setOnClickListener(v -> a.addWebsite(a));

        // Group enabled state
        if (a.canEdit()) {
            // Can edit, show switch
            editSwitch.setChecked(a.isEditing());
            dialog.findViewById(R.id.editModeContainer).setVisibility(View.VISIBLE);
            dialog.findViewById(R.id.editRequiredContaier).setVisibility(View.VISIBLE);
            addWebsite.setVisibility(View.GONE);
        } else {
            dialog.findViewById(R.id.editModeContainer).setVisibility(View.GONE);
            dialog.findViewById(R.id.editRequiredContaier).setVisibility(View.GONE);
            addWebsite.setVisibility(View.VISIBLE);
        }

        // Update button
        if (Updater.isUpdateAvailable(a)) {
            View skippedUpdateButton = dialog.findViewById(R.id.updateButton);
            skippedUpdateButton.setVisibility(View.VISIBLE);
            skippedUpdateButton.setOnClickListener((view) -> new Updater(a).updateAppEvenIfSkipped());
        }

        // Wallpaper and style
        Switch dark = dialog.findViewById(R.id.darkModeSwitch);
        dark.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_DARK_MODE, Settings.DEFAULT_DARK_MODE));
        dark.setOnCheckedChangeListener((compoundButton, value) -> {
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_DARK_MODE, value);
            a.darkMode = value;
            a.clearAdapterCaches();
            a.refreshInterfaceAll();
        });
        Switch hueShift = dialog.findViewById(R.id.hueShiftSwitch);
        hueShift.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_BACKGROUND_OVERLAY,
                Platform.isTv(a) ? Settings.DEFAULT_BACKGROUND_OVERLAY_TV : Settings.DEFAULT_BACKGROUND_OVERLAY_VR));
        hueShift.setOnCheckedChangeListener((compoundButton, value) -> a.setBackgroundOverlay(value));
        ImageView[] views = {
                dialog.findViewById(R.id.background0),
                dialog.findViewById(R.id.background1),
                dialog.findViewById(R.id.background2),
                dialog.findViewById(R.id.background3),
                dialog.findViewById(R.id.background4),
                dialog.findViewById(R.id.background5),
                dialog.findViewById(R.id.background6),
                dialog.findViewById(R.id.background7),
                dialog.findViewById(R.id.background8),
                dialog.findViewById(R.id.background9),
                dialog.findViewById(R.id.background_custom)
        };
        int background = a.sharedPreferences.getInt(Settings.KEY_BACKGROUND,
                Platform.isTv(a)
                        ? Settings.DEFAULT_BACKGROUND_TV
                        : Settings.DEFAULT_BACKGROUND_VR);
        if (background < 0) background = views.length-1;

        for (ImageView image : views) {
            image.setClipToOutline(true);
        }
        final int wallpaperWidth = 32;
        final int selectedWallpaperWidthPx = a.dp(445+20-(wallpaperWidth+4)*(views.length-1)-wallpaperWidth);
        views[background].getLayoutParams().width = selectedWallpaperWidthPx;
        views[background].requestLayout();
        for (int i = 0; i < views.length; i++) {
            int index = i;
            views[i].setOnClickListener(view -> {
                int lastIndex = a.sharedPreferences.getInt(Settings.KEY_BACKGROUND,
                        Platform.isTv(a)
                                ? Settings.DEFAULT_BACKGROUND_TV
                                : Settings.DEFAULT_BACKGROUND_VR);
                if (lastIndex >= SettingsManager.BACKGROUND_DRAWABLES.length || lastIndex < 0)
                    lastIndex = SettingsManager.BACKGROUND_DRAWABLES.length;
                ImageView last = views[lastIndex];
                if (last == view) return;

                ValueAnimator viewAnimator = ValueAnimator.ofInt(view.getWidth(), selectedWallpaperWidthPx);
                viewAnimator.setDuration(250);
                viewAnimator.setInterpolator(new DecelerateInterpolator());
                viewAnimator.addUpdateListener(animation -> {
                    view.getLayoutParams().width = (int) animation.getAnimatedValue();
                    view.requestLayout();
                });
                viewAnimator.start();

                ValueAnimator lastAnimator = ValueAnimator.ofInt(last.getWidth(), a.dp(wallpaperWidth));
                lastAnimator.setDuration(250);
                lastAnimator.setInterpolator(new DecelerateInterpolator());
                lastAnimator.addUpdateListener(animation -> {
                    last.getLayoutParams().width = (int) animation.getAnimatedValue();
                    last.requestLayout();
                });
                lastAnimator.start();

                if (index == SettingsManager.BACKGROUND_DRAWABLES.length) {
                    ImageLib.showImagePicker(a, Settings.PICK_THEME_CODE);
                } else {
                    a.setBackground(index);
                    dark.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_DARK_MODE, Settings.DEFAULT_DARK_MODE));
                    hueShift.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_BACKGROUND_OVERLAY,
                            Platform.isTv(a) ? Settings.DEFAULT_BACKGROUND_OVERLAY_TV : Settings.DEFAULT_BACKGROUND_OVERLAY_VR));
                }
            });
        }

        // Icons & Layout
        SeekBar scale = dialog.findViewById(R.id.scaleSeekBar);
        scale.setProgress(a.sharedPreferences.getInt(Settings.KEY_SCALE, Settings.DEFAULT_SCALE));

        scale.post(() -> scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                a.sharedPreferenceEditor.putInt(Settings.KEY_SCALE, value);
                a.refreshInterface();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { a.refreshInterfaceAll(); }
        }));
        scale.setMax(150);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) scale.setMin(80);

        SeekBar margin = dialog.findViewById(R.id.marginSeekBar);
        margin.setProgress(a.sharedPreferences.getInt(Settings.KEY_MARGIN, Settings.DEFAULT_MARGIN));
        margin.post(() -> margin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                a.sharedPreferenceEditor.putInt(Settings.KEY_MARGIN, value);
                a.refreshInterface();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { a.refreshInterfaceAll(); }
        }));
        margin.setMax(40);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) margin.setMin(0);

        Switch groups = dialog.findViewById(R.id.groupSwitch);
        groups.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_GROUPS_ENABLED, Settings.DEFAULT_GROUPS_ENABLED));
        groups.setOnCheckedChangeListener((sw, value) -> {
            if (!a.sharedPreferences.getBoolean(Settings.KEY_SEEN_HIDDEN_GROUPS_POPUP, false) && value != Settings.DEFAULT_GROUPS_ENABLED) {
                groups.setChecked(Settings.DEFAULT_GROUPS_ENABLED); // Revert switch
                AlertDialog subDialog = Dialog.build(a, R.layout.dialog_hide_groups_info_tv);
                if (subDialog == null) return;
                subDialog.findViewById(R.id.confirm).setOnClickListener(view -> {
                    final boolean newValue = !Settings.DEFAULT_GROUPS_ENABLED;
                    a.sharedPreferenceEditor
                            .putBoolean(Settings.KEY_SEEN_HIDDEN_GROUPS_POPUP, true)
                            .putBoolean(Settings.KEY_GROUPS_ENABLED, newValue)
                            .apply();
                    groups.setChecked(!Settings.DEFAULT_GROUPS_ENABLED);
                    a.refreshInterfaceAll();
                    subDialog.dismiss();
                    // Group enabled state
                    {
                        dialog.findViewById(R.id.editModeContainer).setVisibility(View.GONE);
                        dialog.findViewById(R.id.editRequiredContaier).setVisibility(View.GONE);
                        addWebsite.setVisibility(View.VISIBLE);
                    }
                });
                subDialog.findViewById(R.id.cancel).setOnClickListener(view -> subDialog.dismiss());
            } else {
                a.sharedPreferenceEditor.putBoolean(Settings.KEY_GROUPS_ENABLED, value);
                // Group enabled state
                if (value) {
                    // Can edit, show switch
                    editSwitch.setChecked(a.isEditing());
                    dialog.findViewById(R.id.editModeContainer).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editRequiredContaier).setVisibility(View.VISIBLE);
                    addWebsite.setVisibility(View.GONE);
                } else {
                    dialog.findViewById(R.id.editModeContainer).setVisibility(View.GONE);
                    dialog.findViewById(R.id.editRequiredContaier).setVisibility(View.GONE);
                    addWebsite.setVisibility(View.VISIBLE);
                }
                a.refreshInterfaceAll();
            }
        });

        // Clear buttons (limited to one use to prevent bugs due to spamming)
        clearedLabel= false;
        clearedIcon = false;

        View clearLabel = dialog.findViewById(R.id.clearLabelButton);
        clearLabel.setOnClickListener(view -> {
            if (!clearedLabel) {
                Compat.clearLabels(a);
                clearLabel.setAlpha(0.5f);
                clearedLabel = true;
            }
        });
        View clearIcon = dialog.findViewById(R.id.clearIconButton);
        clearIcon.setOnClickListener(view -> {
            if (!clearedIcon) {
                Compat.clearIcons(a);
                clearIcon.setAlpha(0.5f);
                clearedIcon = true;
            }
        });
        View groupSettings = dialog.findViewById(R.id.groupDefaultsInfoButton);
        groupSettings.setOnClickListener(view -> showGroupSettings(a));

        // Banner mode
        final Map<App.Type, Switch> switchByType = new HashMap<>();
        switchByType.put(App.Type.TYPE_PHONE, dialog.findViewById(R.id.banner2dSwitch));
        switchByType.put(App.Type.TYPE_VR, dialog.findViewById(R.id.bannerVrSwitch));
        switchByType.put(App.Type.TYPE_TV, dialog.findViewById(R.id.bannerTvSwitch));
        switchByType.put(App.Type.TYPE_PANEL, dialog.findViewById(R.id.bannerPanelSwitch));
        switchByType.put(App.Type.TYPE_WEB, dialog.findViewById(R.id.bannerWebSwitch));
        final Map<App.Type, View> switchContainerByType = new HashMap<>();
        switchContainerByType.put(App.Type.TYPE_PHONE, dialog.findViewById(R.id.bannerPhoneContainer));
        switchContainerByType.put(App.Type.TYPE_VR, dialog.findViewById(R.id.bannerVrContainer));
        switchContainerByType.put(App.Type.TYPE_TV, dialog.findViewById(R.id.bannerTvContainer));
        switchContainerByType.put(App.Type.TYPE_PANEL, dialog.findViewById(R.id.bannerPanelContainer));
        switchContainerByType.put(App.Type.TYPE_WEB, dialog.findViewById(R.id.bannerWebsiteContainer));

        for (App.Type type : switchByType.keySet()) {
            if (Platform.getSupportedAppTypes(a).contains(type)) {
                Objects.requireNonNull(switchContainerByType.get(type)).setVisibility(View.VISIBLE);

                final Switch bSwitch = switchByType.get(type);
                if (bSwitch == null) continue;
                bSwitch.setChecked(App.typeIsBanner(type));
                bSwitch.setOnCheckedChangeListener((switchView, value) -> {
                            Compat.clearIconCache(a);
                            a.sharedPreferenceEditor.putBoolean(Settings.KEY_BANNER + type, value)
                                    .apply();
                            a.refreshAppDisplayListsAll();
                });

            } else {
                Objects.requireNonNull(switchContainerByType.get(type)).setVisibility(View.GONE);
            }
        }

        // Names
        Switch names = dialog.findViewById(R.id.nameSquareSwitch);
        names.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_SHOW_NAMES_SQUARE, Settings.DEFAULT_SHOW_NAMES_SQUARE));
        names.setOnCheckedChangeListener((compoundButton, value) -> {
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_SHOW_NAMES_SQUARE, value);
            a.refreshInterfaceAll();
            if(a.getAdapterSquare() != null) a.getAdapterSquare().setShowNames(value);
        });
        Switch wideNames = dialog.findViewById(R.id.nameBannerSwitch);
        wideNames.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_SHOW_NAMES_BANNER, Settings.DEFAULT_SHOW_NAMES_BANNER));
        wideNames.setOnCheckedChangeListener((compoundButton, value) -> {
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_SHOW_NAMES_BANNER, value);
            a.refreshInterfaceAll();
            if(a.getAdapterBanner() != null) a.getAdapterBanner().setShowNames(value);
            if(a.getAdapterBanner() != null) a.getAdapterBanner().setShowNames(value);
        });

        // Advanced
        Switch longPressEdit = dialog.findViewById(R.id.longPressEditSwitch);
        longPressEdit.setChecked(!a.sharedPreferences.getBoolean(Settings.KEY_DETAILS_LONG_PRESS, Settings.DEFAULT_DETAILS_LONG_PRESS));
        longPressEdit.setOnCheckedChangeListener((compoundButton, value) -> {
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_DETAILS_LONG_PRESS, !value);
            a.refreshInterfaceAll();
        });
        Switch hideEmpty = dialog.findViewById(R.id.hideEmptySwitch);
        hideEmpty.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_AUTO_HIDE_EMPTY, Settings.DEFAULT_AUTO_HIDE_EMPTY));
        hideEmpty.setOnCheckedChangeListener((compoundButton, value) -> {
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_AUTO_HIDE_EMPTY, value);
            a.refreshInterfaceAll();
        });
        Switch defaultLaunchOut = dialog.findViewById(R.id.defaultLaunchOutSwitch);
        defaultLaunchOut.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_DEFAULT_LAUNCH_OUT, Settings.DEFAULT_DEFAULT_LAUNCH_OUT));
        defaultLaunchOut.setOnCheckedChangeListener((compoundButton, value) -> {
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_DEFAULT_LAUNCH_OUT, value);
            a.refreshInterfaceAll();
        });
        View loadSettings = dialog.findViewById(R.id.loadSettingsButton);
        loadSettings.setAlpha(SettingsSaver.canLoad(a) ? 1F : 0.5F);
        loadSettings.setOnClickListener((view) -> {
            if (SettingsSaver.canLoad(a)) {
                dialog.dismiss();
                SettingsSaver.load(a);
            }
        });

        View saveSettings = dialog.findViewById(R.id.saveSettingsButton);
        saveSettings.setOnClickListener((view) -> SettingsSaver.save(a));
        saveSettings.setOnClickListener((view) -> {
            SettingsSaver.save(a);
            loadSettings.setAlpha(1F);
        });

    }
    public static void showGroupSettings(LauncherActivity a) {
        clearedSort = false;
        clearedGroups = false;

        AlertDialog dialog = Dialog.build(a, R.layout.dialog_setting_reset_groups);
        if (dialog == null) return;

        View clearSort = dialog.findViewById(R.id.resortOnly);
        clearSort.setOnClickListener(view -> {
            if (!clearedSort) {
                Compat.clearSort(a);
                clearSort.setAlpha(0.5f);
                clearedSort = true;

                Dialog.toast(a.getString(R.string.default_groups_resort_only_toast_main),
                        a.getString(R.string.default_groups_resort_only_toast_bold),
                        false);
            }
        });

        StringBuilder builder = new StringBuilder();
        for (App.Type type : Platform.getSupportedAppTypes(a)) {
            builder.append(App.getTypeString(a, type))
                    .append(" : ")
                    .append(App.getDefaultGroupFor(type))
                    .append("\n");
        }

        TextView info = dialog.findViewById(R.id.infoText);
        info.setText(a.getString(R.string.default_groups_info, builder.toString()));


        View clearDefaults = dialog.findViewById(R.id.resetGroups);
        clearDefaults.setOnClickListener(view -> {
            if (!clearedGroups) {
                Compat.resetDefaultGroups(a);
                clearDefaults.setAlpha(0.5f);
                clearedGroups = true;
                clearSort.setAlpha(0.5f);
                clearedSort = true;

                Dialog.toast(a.getString(R.string.default_groups_reset_groups_toast_main),
                        a.getString(R.string.default_groups_reset_groups_toast_bold),
                        false);

                StringBuilder builder2 = new StringBuilder();
                for (App.Type type : Platform.getSupportedAppTypes(a)) {
                    builder2.append(App.getTypeString(a, type))
                            .append(" : ")
                            .append(App.getDefaultGroupFor(type))
                            .append("\n");
                }

                info.setText(a.getString(R.string.default_groups_info, builder2.toString()));
            }
        });

        dialog.findViewById(R.id.cancel).setOnClickListener(v -> dialog.dismiss());
    }

    }
