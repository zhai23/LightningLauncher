package com.threethan.launcher.launcher;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;
import com.threethan.launcher.adapter.AppsAdapter;
import com.threethan.launcher.adapter.GroupsAdapter;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.StringLib;
import com.threethan.launcher.support.SettingsManager;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;


public class LauncherActivityEditable extends LauncherActivity {
    @Nullable
    Boolean editMode = null;
    public Set<String> currentSelectedApps = new HashSet<>();
    @Override
    public void onBackPressed() {
        if (AppsAdapter.animateClose(this)) return;
        if (!settingsVisible) setEditMode(Boolean.FALSE.equals(editMode));
    }

    // Startup
    @Override
    protected void init() {
        super.init();
        View addWebsiteButton = rootView.findViewById(R.id.addWebsite);
        addWebsiteButton.setOnClickListener(view -> addWebsite(this));
        View stopEditingButton = rootView.findViewById(R.id.stopEditing);
        stopEditingButton.setOnClickListener(view -> setEditMode(false));
    }

    @Override
    protected void refreshInternal() {
        if (editMode == null) editMode = sharedPreferences.getBoolean(Settings.KEY_EDIT_MODE, false);

        super.refreshInternal();

        final View editFooter = rootView.findViewById(R.id.editFooter);
        if (editMode) {
            // Edit bar theming and actions
            editFooter.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#60000000" : "#70BeBeBe")));

            final View selectionHint = rootView.findViewById(R.id.selectionHint);
            final TextView selectionHintText = rootView.findViewById(R.id.selectionHintText);
            final View uninstallButton = rootView.findViewById(R.id.uninstallBulk);

            for (TextView textView: new TextView[]{selectionHintText, rootView.findViewById(R.id.addWebsite), rootView.findViewById(R.id.stopEditing)}) {
                textView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#80000000" : "#FFFFFF")));
                textView.setTextColor(Color.parseColor(darkMode ? "#FFFFFF" : "#000000"));
            }
            selectionHint  .setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#80000000" : "#FFFFFF")));
            uninstallButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#FFFFFF" : "#3a3a3c")));

            selectionHint.setOnClickListener((view) -> {
                if (currentSelectedApps.isEmpty()) {
                    final Adapter appAdapterIcon = ((GridView) rootView.findViewById(R.id.appsViewIcon)).getAdapter();
                    for (int i=0; i<appAdapterIcon.getCount(); i++) currentSelectedApps.add(((ApplicationInfo) appAdapterIcon.getItem(i)).packageName);
                    final Adapter appsAdapterWide = ((GridView) rootView.findViewById(R.id.appsViewWide)).getAdapter();
                    for (int i=0; i<appsAdapterWide.getCount(); i++) currentSelectedApps.add(((ApplicationInfo) appsAdapterWide.getItem(i)).packageName);
                    selectionHintText.setText(R.string.selection_hint_all);
                } else {
                    currentSelectedApps.clear();
                    selectionHintText.setText(R.string.selection_hint_cleared);
                }
                selectionHint.postDelayed(this::updateSelectionHint, 2000);
                rootView.findViewById(R.id.uninstallBulk).setVisibility(View.GONE);
            });
            selectionHintText.setOnClickListener((view) -> {
                if (currentSelectedApps.isEmpty()) {
                    final Adapter appAdapterIcon = ((GridView) rootView.findViewById(R.id.appsViewIcon)).getAdapter();
                    for (int i=0; i<appAdapterIcon.getCount(); i++) currentSelectedApps.add(((ApplicationInfo) appAdapterIcon.getItem(i)).packageName);
                    final Adapter appsAdapterWide = ((GridView) rootView.findViewById(R.id.appsViewWide)).getAdapter();
                    for (int i=0; i<appsAdapterWide.getCount(); i++) currentSelectedApps.add(((ApplicationInfo) appsAdapterWide.getItem(i)).packageName);
                    selectionHintText.setText(R.string.selection_hint_all);
                } else {
                    currentSelectedApps.clear();
                    selectionHintText.setText(R.string.selection_hint_cleared);
                }
                selectionHint.postDelayed(this::updateSelectionHint, 2000);
                rootView.findViewById(R.id.uninstallBulk).setVisibility(View.GONE);
            });
            uninstallButton.setOnClickListener(view -> {
                int delay = 0;
                for (String currentSelectedApp : currentSelectedApps) {
                    mainView.postDelayed(() -> App.uninstall(this, currentSelectedApp), delay);
                    if (!App.isWebsite(currentSelectedApp)) delay += 1000;
                }
            });
        }
        if (editFooter.getVisibility() == View.GONE && editMode) {
            editFooter.setTranslationY(100f);
            editFooter.setVisibility(View.VISIBLE);
        }
        ObjectAnimator aF = ObjectAnimator.ofFloat(editFooter, "TranslationY", editMode ?0f:100f);
        aF.setDuration(200);
        aF.start();
        if (!editMode) editFooter.postDelayed(() -> {
            if (!editMode) editFooter.setVisibility(View.GONE);
        }, 200);

        if (!editMode) {
            currentSelectedApps.clear();
            updateSelectionHint();
        }
    }

    @Override
    protected void clickGroup(int position) {
        final List<String> groupsSorted = settingsManager.getAppGroupsSorted(false);

        // If the new group button was selected, create and select a new group
        if (position >= groupsSorted.size()) {
            final String newName = settingsManager.addGroup();
            settingsManager.selectGroup(newName);
            refreshInterface();
            return;
        }
        final String group = groupsSorted.get(position);

        // Move apps if any are selected
        if (!currentSelectedApps.isEmpty()) {
            GroupsAdapter groupsAdapter = (GroupsAdapter) groupPanelGridView.getAdapter();
            for (String app : currentSelectedApps) groupsAdapter.setGroup(app, group);
            TextView selectionHintText = rootView.findViewById(R.id.selectionHintText);
            selectionHintText.setText( currentSelectedApps.size()==1 ?
                    getString(R.string.selection_moved_single, group) :
                    getString(R.string.selection_moved_multiple, currentSelectedApps.size(), group)
            );
            rootView.findViewById(R.id.uninstallBulk).setVisibility(View.GONE);
            selectionHintText.postDelayed(this::updateSelectionHint, 2000);

            currentSelectedApps.clear();

            SettingsManager.storeValues();
            refreshInterface();
        } else super.clickGroup(position);
    }

    // Function overrides
    @Override
    public void setEditMode(boolean value) {
        editMode = value;
        if (sharedPreferenceEditor == null) return;
        sharedPreferenceEditor.putBoolean(Settings.KEY_EDIT_MODE, editMode);
        refreshInterface();
    }

    @Override
    public boolean selectApp(String app) {
        if (currentSelectedApps.contains(app)) {
            currentSelectedApps.remove(app);
            updateSelectionHint();
            return false;
        } else {
            currentSelectedApps.add(app);
            updateSelectionHint();
            return true;
        }
    }

    @Override
    protected void startWithExistingActivity() {
        super.startWithExistingActivity();
        // Load edit things if loading from an existing activity
        final View editFooter = rootView.findViewById(R.id.editFooter);
        if (editFooter.getVisibility() == View.VISIBLE) refreshInterfaceAll();
    }

    @Override
    public void refreshAppDisplayLists() {
        super.refreshAppDisplayLists();

        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, new HashSet<>());
        Set<String> packages = getAllPackages();

        try {
            for (String appPackage : currentSelectedApps)
                if (!packages.contains(appPackage) && !webApps.contains(appPackage))
                    currentSelectedApps.remove(appPackage);
        } catch (ConcurrentModificationException ignored) {}
        updateSelectionHint();
    }

    @Override
    public boolean isSelected(String app) { return currentSelectedApps.contains(app); }
    @Override
    protected int getBottomBarHeight() { return Boolean.TRUE.equals(editMode) ? dp(60) : 0; }
    @Override
    public boolean isEditing() { return Boolean.TRUE.equals(editMode); }
    @Override
    public boolean canEdit() { return true; }

    // Utility functions
    void updateSelectionHint() {
        TextView selectionHintText = rootView.findViewById(R.id.selectionHintText);
        final View uninstallButton = rootView.findViewById(R.id.uninstallBulk);
        uninstallButton.setVisibility(currentSelectedApps.isEmpty() ? View.GONE : View.VISIBLE);

        final int size = currentSelectedApps.size();
        if (size == 0)      selectionHintText.setText(R.string.selection_hint_none);
        else if (size == 1) selectionHintText.setText(R.string.selection_hint_single);
        else selectionHintText.setText(getString(R.string.selection_hint_multiple, size));
    }

    void addWebsite(Context context) {
        sharedPreferenceEditor.apply();
        AlertDialog dialog = Dialog.build(this, R.layout.dialog_new_website);

        // Set group to (one of) selected
        String group;
        final ArrayList<String> appGroupsSorted = settingsManager.getAppGroupsSorted(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N && !appGroupsSorted.isEmpty())
            group = appGroupsSorted.get(0);
        else group = SettingsManager.getDefaultGroup(false, true);

        dialog.findViewById(R.id.cancel).setOnClickListener(view -> dialog.cancel());
        ((TextView) dialog.findViewById(R.id.addText)).setText(getString(R.string.add_website_group, group));
        EditText urlEdit = dialog.findViewById(R.id.appUrl);

        TextView badUrl  = dialog.findViewById(R.id.badUrl);
        TextView usedUrl = dialog.findViewById(R.id.usedUrl);

        dialog.findViewById(R.id.confirm).setOnClickListener(view -> {
            String url  = urlEdit.getText().toString().toLowerCase();
            if (StringLib.isInvalidUrl(url)) {
                badUrl .setVisibility(View.VISIBLE);
                usedUrl.setVisibility(View.GONE);
                return;
            }
            String foundGroup = Platform.findWebsite(sharedPreferences, url);
            if (foundGroup != null) {
                badUrl .setVisibility(View.GONE);
                usedUrl.setVisibility(View.VISIBLE);
                usedUrl.setText(context.getString(R.string.add_website_used_url, foundGroup));
                return;
            }
            Platform.addWebsite(sharedPreferences, url);
            settingsManager.setAppGroup(StringLib.fixUrl(url), group);
            dialog.cancel();
            refreshAppDisplayLists();
        });
        dialog.findViewById(R.id.info).setOnClickListener(view -> {
            dialog.dismiss();
            showWebsiteInfo();
        });

        // Presets
        dialog.findViewById(R.id.presetGoogle).setOnClickListener(view -> urlEdit.setText(R.string.preset_google));
        dialog.findViewById(R.id.presetYoutube).setOnClickListener(view -> urlEdit.setText(R.string.preset_youtube));
        dialog.findViewById(R.id.presetDiscord).setOnClickListener(view -> urlEdit.setText(R.string.preset_discord));
        dialog.findViewById(R.id.presetSpotify).setOnClickListener(view -> urlEdit.setText(R.string.preset_spotify));
        dialog.findViewById(R.id.presetTidal).setOnClickListener(view -> urlEdit.setText(R.string.preset_tidal));
    }

    void showWebsiteInfo() {
        AlertDialog subDialog = Dialog.build(this, R.layout.dialog_website_info);
        subDialog.findViewById(R.id.confirm).setOnClickListener(view -> {
            sharedPreferenceEditor.putBoolean(Settings.KEY_SEEN_WEBSITE_POPUP, true).apply();
            addWebsite(this);
            subDialog.dismiss();
        });
    }

    @Override
    void updateToolBars() {
        super.updateToolBars();
        if (!isEditing()) return;

        BlurView blurViewE = rootView.findViewById(R.id.editFooter);
        blurViewE.setOverlayColor(Color.parseColor(darkMode ? "#4A000000" : "#50FFFFFF"));

        float blurRadiusDp = 15f;

        View windowDecorView = getWindow().getDecorView();
        ViewGroup rootViewGroup = windowDecorView.findViewById(android.R.id.content);

        Drawable windowBackground = windowDecorView.getBackground();
        blurViewE.setupWith(rootViewGroup, new RenderScriptBlur(getApplicationContext())) // or RenderEffectBlur
                .setFrameClearDrawable(windowBackground) // Optional
                .setBlurRadius(blurRadiusDp);

        // Update then deactivate bv
        blurViewE.setActivated(false);
        blurViewE.setActivated(true);
        blurViewE.setActivated(false);
    }
}
