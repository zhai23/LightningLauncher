package com.threethan.launcher.launcher;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.Compat;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.support.SettingsManager;
import com.threethan.launcher.support.Updater;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class SettingsDialog {
    private static boolean clearedLabel;
    private static boolean clearedIcon;
    private static boolean clearedSort;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    public static void showSettings(LauncherActivity a) {
        a.settingsVisible = true;
        AlertDialog dialog = Dialog.build(a, R.layout.dialog_settings);
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
        if (a.canEdit()) {
            editSwitch.setChecked(a.isEditing());
            editSwitch.setOnClickListener(view1 -> {
                a.setEditMode(!a.isEditing());
                ArrayList<String> selectedGroups = a.settingsManager.getAppGroupsSorted(true);
                if (a.isEditing() && (selectedGroups.size() > 1)) {
                    Set<String> selectFirst = new HashSet<>();
                    selectFirst.add(selectedGroups.get(0));
                    a.settingsManager.setSelectedGroups(selectFirst);
                }
            });
        } else editSwitch.setVisibility(View.GONE);
        TextView editModeText = dialog.findViewById(R.id.editModeText);
        editModeText.setText(a.canEdit() ? R.string.edit_mode : R.string.edit_mode_disabled);
        // Update button
        if (Updater.isUpdateAvailable(a)) {
            View skippedUpdateButton = dialog.findViewById(R.id.removeButton);
            skippedUpdateButton.setVisibility(View.VISIBLE);
            skippedUpdateButton.setOnClickListener((view) -> new Updater(a).updateAppEvenIfSkipped());
        }

        // Wallpaper and style
        Switch dark = dialog.findViewById(R.id.darkModeSwitch);
        dark.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_DARK_MODE, Settings.DEFAULT_DARK_MODE));
        dark.setOnCheckedChangeListener((compoundButton, value) -> {
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_DARK_MODE, value);
            a.refresh();
        });
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
        int background = a.sharedPreferences.getInt(Settings.KEY_BACKGROUND, Settings.DEFAULT_BACKGROUND);
        if (background < 0) background = views.length-1;

        for (ImageView image : views) {
            image.setClipToOutline(true);
        }
        final int wallpaperWidth = 32;
        final int selectedWallpaperWidthPx = a.dp(455+20-(wallpaperWidth+4)*(views.length-1)-wallpaperWidth);
        views[background].getLayoutParams().width = selectedWallpaperWidthPx;
        views[background].requestLayout();
        for (int i = 0; i < views.length; i++) {
            int index = i;
            views[i].setOnClickListener(view -> {
                int lastIndex = a.sharedPreferences.getInt(Settings.KEY_BACKGROUND, Settings.DEFAULT_BACKGROUND);
                if (lastIndex >= SettingsManager.BACKGROUND_DRAWABLES.length || lastIndex < 0) lastIndex = SettingsManager.BACKGROUND_DRAWABLES.length;
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
                }
            });
        }


        // Icons & Layout
        SeekBar scale = dialog.findViewById(R.id.scaleSeekBar);

        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                a.sharedPreferenceEditor.putInt(Settings.KEY_SCALE, value);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { a.refresh(); }
        });
        scale.setMax(200);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) scale.setMin(80);
        scale.setProgress(a.sharedPreferences.getInt(Settings.KEY_SCALE, Settings.DEFAULT_SCALE));

        SeekBar margin = dialog.findViewById(R.id.marginSeekBar);
        margin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                a.sharedPreferenceEditor.putInt(Settings.KEY_MARGIN, value);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { a.refresh(); }
        });
        margin.setProgress(a.sharedPreferences.getInt(Settings.KEY_MARGIN, Settings.DEFAULT_MARGIN));
        margin.setMax(59);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) margin.setMin(5);

        Switch groups = dialog.findViewById(R.id.groupSwitch);
        groups.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_GROUPS_ENABLED, Settings.DEFAULT_GROUPS_ENABLED));
        groups.setOnCheckedChangeListener((sw, value) -> {
            if (!a.sharedPreferences.getBoolean(Settings.KEY_SEEN_HIDDEN_GROUPS_POPUP, false) && value != Settings.DEFAULT_GROUPS_ENABLED) {
                groups.setChecked(Settings.DEFAULT_GROUPS_ENABLED); // Revert switch
                AlertDialog subDialog = Dialog.build(a, R.layout.dialog_hide_groups_info);
                subDialog.findViewById(R.id.confirm).setOnClickListener(view -> {
                    final boolean newValue = !Settings.DEFAULT_GROUPS_ENABLED;
                    a.sharedPreferenceEditor
                            .putBoolean(Settings.KEY_SEEN_HIDDEN_GROUPS_POPUP, true)
                            .putBoolean(Settings.KEY_GROUPS_ENABLED, newValue)
                            .apply();
                    groups.setChecked(!Settings.DEFAULT_GROUPS_ENABLED);
                    a.refresh();
                    subDialog.dismiss();
                });
                subDialog.findViewById(R.id.cancel).setOnClickListener(view -> subDialog.dismiss());
            } else {
                a.sharedPreferenceEditor.putBoolean(Settings.KEY_GROUPS_ENABLED, value);
                a.refresh();
            }
        });

        // Clear buttons (limited to one use to prevent bugs due to spamming)
        clearedLabel= false;
        clearedSort = false;
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
        View clearSort = dialog.findViewById(R.id.clearSortButton);
        clearSort.setOnClickListener(view -> {
            if (!clearedSort) {
                Compat.clearSort(a);
                clearSort.setAlpha(0.5f);
                clearedSort = true;
            }
        });

        // Wide display
        Switch bannerVr = dialog.findViewById(R.id.bannerVrSwitch);
        bannerVr.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_WIDE_VR, Settings.DEFAULT_WIDE_VR));
        bannerVr.setOnCheckedChangeListener((compoundButton, value) -> {
            Compat.clearIconCache(a);
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_WIDE_VR, value);
            a.refreshAppDisplayLists();
            a.refresh();
        });
        Switch banner2d = dialog.findViewById(R.id.banner2dSwitch);
        banner2d.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_WIDE_2D, Settings.DEFAULT_WIDE_2D));
        banner2d.setOnCheckedChangeListener((compoundButton, value) -> {
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_WIDE_2D, value);
            a.refreshAppDisplayLists();
            a.refresh();
        });
        Switch bannerWeb = dialog.findViewById(R.id.bannerWebSwitch);
        bannerWeb.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_WIDE_WEB, Settings.DEFAULT_WIDE_WEB));
        bannerWeb.setOnCheckedChangeListener((compoundButton, value) -> {
            Compat.clearIconCache(a);
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_WIDE_WEB, value);
            a.refreshAppDisplayLists();
            a.refresh();
        });

        // Names
        Switch names = dialog.findViewById(R.id.nameSquareSwitch);
        names.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_SHOW_NAMES_SQUARE, Settings.DEFAULT_SHOW_NAMES_SQUARE));
        names.setOnCheckedChangeListener((compoundButton, value) -> {
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_SHOW_NAMES_SQUARE, value);
            a.refresh();
        });
        Switch wideNames = dialog.findViewById(R.id.nameBannerSwitch);
        wideNames.setChecked(a.sharedPreferences.getBoolean(Settings.KEY_SHOW_NAMES_BANNER, Settings.DEFAULT_SHOW_NAMES_BANNER));
        wideNames.setOnCheckedChangeListener((compoundButton, value) -> {
            a.sharedPreferenceEditor.putBoolean(Settings.KEY_SHOW_NAMES_BANNER, value);
            a.refresh();
        });
    }
}
