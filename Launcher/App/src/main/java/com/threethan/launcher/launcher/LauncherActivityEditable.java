package com.threethan.launcher.launcher;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Patterns;
import android.view.View;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import com.threethan.launcher.R;
import com.threethan.launcher.adapter.AppsAdapter;
import com.threethan.launcher.adapter.GroupsAdapter;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.support.SettingsManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class LauncherActivityEditable extends LauncherActivity {
    boolean editMode = false;
    public Set<String> currentSelectedApps = new HashSet<>();
    @Override
    public void onBackPressed() {
        if (AppsAdapter.animateClose(this)) return;
        if (!settingsPage.visible) setEditMode(!editMode);
    }

    // Startup
    @Override
    protected void initView() {
        super.initView();
        View addWebsiteButton = m.findViewById(R.id.addWebsite);
        addWebsiteButton.setOnClickListener(view -> addWebsite());
        View stopEditingButton = m.findViewById(R.id.stopEditing);
        stopEditingButton.setOnClickListener(view -> setEditMode(false));
    }

    @Override
    protected void refreshInternal() {
        super.refreshInternal();

        editMode = sharedPreferences.getBoolean(Settings.KEY_EDIT_MODE, false);

        if (!groupsEnabled && editMode) {
            groupsEnabled = true;
            updateTopBar();
        }

        final View editFooter = m.findViewById(R.id.editFooter);
        if (editMode) {
            // Edit bar theming and actions
            editFooter.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#60000000" : "#70BeBeBe")));

            final View selectionHint = m.findViewById(R.id.selectionHint);
            final TextView selectionHintText = m.findViewById(R.id.selectionHintText);
            final View uninstallButton = m.findViewById(R.id.uninstallBulk);

            for (TextView textView: new TextView[]{selectionHintText, m.findViewById(R.id.addWebsite), m.findViewById(R.id.stopEditing)}) {
                textView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#3a3a3c" : "#FFFFFF")));
                textView.setTextColor(Color.parseColor(darkMode ? "#FFFFFF" : "#000000"));
            }
            selectionHint  .setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#3a3a3c" : "#FFFFFF")));
            uninstallButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#FFFFFF" : "#3a3a3c")));

            selectionHint.setOnClickListener((view) -> {
                if (currentSelectedApps.isEmpty()) {
                    final Adapter appAdapterIcon = ((GridView) m.findViewById(R.id.appsViewIcon)).getAdapter();
                    for (int i=0; i<appAdapterIcon.getCount(); i++) currentSelectedApps.add(((ApplicationInfo) appAdapterIcon.getItem(i)).packageName);
                    final Adapter appsAdapterWide = ((GridView) m.findViewById(R.id.appsViewWide)).getAdapter();
                    for (int i=0; i<appsAdapterWide.getCount(); i++) currentSelectedApps.add(((ApplicationInfo) appsAdapterWide.getItem(i)).packageName);
                    selectionHintText.setText(R.string.selection_hint_all);
                } else {
                    currentSelectedApps.clear();
                    selectionHintText.setText(R.string.selection_hint_cleared);
                }
                selectionHint.postDelayed(this::updateSelectionHint, 2000);
                m.findViewById(R.id.uninstallBulk).setVisibility(View.GONE);
            });
            selectionHintText.setOnClickListener((view) -> {
                if (currentSelectedApps.isEmpty()) {
                    final Adapter appAdapterIcon = ((GridView) m.findViewById(R.id.appsViewIcon)).getAdapter();
                    for (int i=0; i<appAdapterIcon.getCount(); i++) currentSelectedApps.add(((ApplicationInfo) appAdapterIcon.getItem(i)).packageName);
                    final Adapter appsAdapterWide = ((GridView) m.findViewById(R.id.appsViewWide)).getAdapter();
                    for (int i=0; i<appsAdapterWide.getCount(); i++) currentSelectedApps.add(((ApplicationInfo) appsAdapterWide.getItem(i)).packageName);
                    selectionHintText.setText(R.string.selection_hint_all);
                } else {
                    currentSelectedApps.clear();
                    selectionHintText.setText(R.string.selection_hint_cleared);
                }
                selectionHint.postDelayed(this::updateSelectionHint, 2000);
                m.findViewById(R.id.uninstallBulk).setVisibility(View.GONE);
            });
            uninstallButton.setOnClickListener(view -> {
                int delay = 0;
                for (String currentSelectedApp : currentSelectedApps) {
                    mainView.postDelayed(() -> App.uninstall(this, currentSelectedApp), delay);
                    if (!App.isWebsite(currentSelectedApp)) delay += 1000;
                }
            });
        }
        editFooter.setVisibility(editMode ? View.VISIBLE : View.GONE);

        if (!editMode) {
            currentSelectedApps.clear();
            updateSelectionHint();
        }
    }

    @Override
    protected void changeGroup(int position) {
        final List<String> groupsSorted = settingsManager.getAppGroupsSorted(false);

        // If the new group button was selected, create and select a new group
        if (position >= groupsSorted.size()) {
            final String newName = settingsManager.addGroup();
            settingsManager.selectGroup(newName);
            refresh();
            return;
        }
        final String group = groupsSorted.get(position);

        // Move apps if any are selected
        if (!currentSelectedApps.isEmpty()) {
            GroupsAdapter groupsAdapter = (GroupsAdapter) groupPanelGridView.getAdapter();
            for (String app : currentSelectedApps) groupsAdapter.setGroup(app, group);
            TextView selectionHintText = m.findViewById(R.id.selectionHintText);
            selectionHintText.setText( currentSelectedApps.size()==1 ?
                    getString(R.string.selection_moved_single, group) :
                    getString(R.string.selection_moved_multiple, currentSelectedApps.size(), group)
            );
            m.findViewById(R.id.uninstallBulk).setVisibility(View.GONE);
            selectionHintText.postDelayed(this::updateSelectionHint, 2000);

            currentSelectedApps.clear();

            SettingsManager.storeValues();
            refresh();
        }
        else if (settingsManager.selectGroup(group)) refresh();
        else recheckPackages(); // If clicking on the same single group, check if there are any new packages
    }

    // Function overrides
    @Override
    public void setEditMode(boolean value) {
        editMode = value;
        sharedPreferenceEditor.putBoolean(Settings.KEY_EDIT_MODE, editMode);
        refresh();
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
    public boolean isSelected(String app) { return currentSelectedApps.contains(app); }
    @Override
    protected int getBottomBarHeight() { return editMode ? dp(60) : 0; }
    @Override
    public boolean isEditing() { return editMode; }
    @Override
    public boolean canEdit() { return true; }

    // Utility functions
    void updateSelectionHint() {
        TextView selectionHintText = m.findViewById(R.id.selectionHintText);
        final View uninstallButton = m.findViewById(R.id.uninstallBulk);
        uninstallButton.setVisibility(currentSelectedApps.isEmpty() ? View.GONE : View.VISIBLE);

        final int size = currentSelectedApps.size();
        if (size == 0)      selectionHintText.setText(R.string.selection_hint_none);
        else if (size == 1) selectionHintText.setText(R.string.selection_hint_single);
        else selectionHintText.setText(getString(R.string.selection_hint_multiple, size));
    }

    void addWebsite() {
        sharedPreferenceEditor.apply();
        AlertDialog dialog = Dialog.build(this, R.layout.dialog_new_website);

        // Set group to (one of) selected
        String group;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            group = settingsManager.getSelectedGroups().stream().sorted().findFirst().orElse(SettingsManager.getDefaultGroup(false, true));
        else group = SettingsManager.getDefaultGroup(false, true);

        dialog.findViewById(R.id.cancel).setOnClickListener(view -> dialog.cancel());
        ((TextView) dialog.findViewById(R.id.addText)).setText(getString(R.string.add_website_group, group));
        EditText urlEdit = dialog.findViewById(R.id.appUrl);

        dialog.findViewById(R.id.confirm).setOnClickListener(view -> {
            String url  = urlEdit.getText().toString().toLowerCase();
            if (!Patterns.WEB_URL.matcher(url).matches() || !url.contains(".")) {
                dialog.findViewById(R.id.badUrl).setVisibility(View.VISIBLE);
                return;
            }
            Platform.addWebsite(sharedPreferences, url, group);
            dialog.cancel();
            refreshAppDisplayLists();
            refresh();
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
            addWebsite();
            subDialog.dismiss();
        });
    }
}
