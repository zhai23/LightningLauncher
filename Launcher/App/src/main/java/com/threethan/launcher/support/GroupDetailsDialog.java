package com.threethan.launcher.support;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.StringLib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class GroupDetailsDialog {
    public static void showGroupDetails(int groupPosition, LauncherActivity launcherActivity) {
        final Map<String, String> apps = SettingsManager.getAppGroupMap();
        final Set<String> appGroupsSet = SettingsManager.getAppGroups();
        SettingsManager settingsManager = launcherActivity.settingsManager;
        final String groupName = settingsManager.getAppGroupsSorted(false).get(groupPosition);
        if (groupName == null) return;

        AlertDialog dialog = Dialog.build(launcherActivity, R.layout.dialog_group_details);
        if (dialog == null) return;

        final EditText groupNameInput = dialog.findViewById(R.id.groupName);
        groupNameInput.setText(StringLib.withoutStar(groupName));

        final boolean[] starred = {StringLib.hasStar(groupName)};
        ImageView starButton = dialog.findViewById(R.id.starGroupButton);
        starButton.setImageResource(starred[0] ? R.drawable.ic_star_on : R.drawable.ic_star_off);
        starButton.setOnClickListener(view1 -> {
            starred[0] = !starred[0];
            starButton.setImageResource(starred[0] ? R.drawable.ic_star_on : R.drawable.ic_star_off);
        });

        final Map<App.Type, Switch> switchByType = new HashMap<>();
        switchByType.put(App.Type.TYPE_PHONE, dialog.findViewById(R.id.default2dSwitch));
        switchByType.put(App.Type.TYPE_VR, dialog.findViewById(R.id.defaultVrSwitch));
        switchByType.put(App.Type.TYPE_TV, dialog.findViewById(R.id.defaultTvSwitch));
        switchByType.put(App.Type.TYPE_PANEL, dialog.findViewById(R.id.defaultPanelSwitch));
        switchByType.put(App.Type.TYPE_WEB, dialog.findViewById(R.id.defaultWebSwitch));
        final Map<App.Type, View> switchContainerByType = new HashMap<>();
        switchContainerByType.put(App.Type.TYPE_PHONE, dialog.findViewById(R.id.defaultPhoneContainer));
        switchContainerByType.put(App.Type.TYPE_VR, dialog.findViewById(R.id.defaultVrContainer));
        switchContainerByType.put(App.Type.TYPE_TV, dialog.findViewById(R.id.defaultTvContainer));
        switchContainerByType.put(App.Type.TYPE_PANEL, dialog.findViewById(R.id.defaultPanelContainer));
        switchContainerByType.put(App.Type.TYPE_WEB, dialog.findViewById(R.id.defaultWebsiteContainer));

        for (App.Type type : switchByType.keySet()) {
            if (Platform.getSupportedAppTypes(launcherActivity).contains(type)) {
                Objects.requireNonNull(switchContainerByType.get(type)).setVisibility(View.VISIBLE);

                @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch cSwitch = switchByType.get(type);
                if (cSwitch == null) continue;
                cSwitch.setChecked(App.getDefaultGroupFor(type).equals(groupName));
                cSwitch.setOnCheckedChangeListener((switchView, value) -> {
                    String newDefault = value ? groupName : Settings.FALLBACK_GROUPS.get(type);
                    if ((!value && groupName.equals(newDefault)) || !appGroupsSet.contains(newDefault))
                        newDefault = null;
                    launcherActivity.sharedPreferenceEditor.putString(Settings.KEY_DEFAULT_GROUP + type, newDefault).apply();
                    final boolean newChecked = App.getDefaultGroupFor(type).equals(groupName);
                    if (newChecked && !value) Dialog.toast(launcherActivity.getString(R.string.toast_cant_unset_group));
                    cSwitch.setChecked(newChecked);
                });
            } else {
                Objects.requireNonNull(switchContainerByType.get(type)).setVisibility(View.GONE);
            }
        }

        dialog.findViewById(R.id.confirm).setOnClickListener(view1 -> {
            String newGroupName = StringLib.setStarred(groupNameInput.getText().toString(), starred[0]);
            if (newGroupName.equals(Settings.UNSUPPORTED_GROUP)) newGroupName = "UNSUPPORTED"; // Prevent permanently hiding apps

            // Move the default group when we rename
            for (App.Type type : Platform.getSupportedAppTypes(launcherActivity))
                if (App.getDefaultGroupFor(type).equals(groupName))
                    launcherActivity.sharedPreferenceEditor.putString(Settings.KEY_DEFAULT_GROUP + type, newGroupName);

            if (newGroupName.length() > 0) {
                appGroupsSet.remove(groupName);
                appGroupsSet.add(newGroupName);

                // Move apps when we rename
                Map<String, String> updatedAppGroupMap = new HashMap<>();
                for (String packageName : apps.keySet()) {
                    if (apps.get(packageName) != null) {
                        if (Objects.requireNonNull(apps.get(packageName)).compareTo(groupName) == 0)
                            updatedAppGroupMap.put(packageName, newGroupName);
                        else
                            updatedAppGroupMap.put(packageName, apps.get(packageName));
                    }
                }
                HashSet<String> selectedGroup = new HashSet<>();
                selectedGroup.add(newGroupName);
                settingsManager.setSelectedGroups(selectedGroup);
                settingsManager.setAppGroups(appGroupsSet);
                SettingsManager.setAppGroupMap(updatedAppGroupMap);
                launcherActivity.refreshInterfaceAll();
            }
            dialog.cancel();
        });

        dialog.findViewById(R.id.deleteGroupButton).setOnClickListener(view1 -> {
            HashMap<String, String> appGroupMap = new HashMap<>();
            for (String packageName : apps.keySet())
                if (!Objects.equals(groupName, apps.get(packageName)))
                    appGroupMap.put(packageName, apps.get(packageName));

            SettingsManager.setAppGroupMap(appGroupMap);
            appGroupsSet.remove(groupName);

            boolean hasNormalGroup = false;
            for (String groupNameIterator : appGroupsSet) {
                if (Objects.equals(groupNameIterator, Settings.HIDDEN_GROUP)) continue;
                if (Objects.equals(groupNameIterator, Settings.UNSUPPORTED_GROUP)) continue;
                hasNormalGroup = true;
                break;
            }
            if (!hasNormalGroup) {
                settingsManager.resetGroups();
            } else {
                settingsManager.setAppGroups(appGroupsSet);
                Set<String> firstSelectedGroup = new HashSet<>();
                firstSelectedGroup.add(settingsManager.getAppGroupsSorted(false).get(0));
                settingsManager.setSelectedGroups(firstSelectedGroup);
            }
            dialog.dismiss();

            launcherActivity.refreshInterfaceAll();
        });
    }
}
