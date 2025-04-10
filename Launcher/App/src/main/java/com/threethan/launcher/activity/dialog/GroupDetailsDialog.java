package com.threethan.launcher.activity.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.PlatformExt;
import com.threethan.launcher.data.Settings;
import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launcher.activity.support.SettingsManager;
import com.threethan.launchercore.lib.StringLib;
import com.threethan.launchercore.util.App;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
/**
 * Provides the dialog which appears when pressing the three-dots icon on a group,
 * or when long-pressing the single selected group in edit mode
 */
public class GroupDetailsDialog extends BasicDialog<LauncherActivity> {
    private final int groupPosition;

    /**
     * Constructs a new GroupDetailsDialog. Make sure to call .show()!
     *
     * @param launcherActivity Parent activity
     * @param groupPosition Position index of the group to show details for
     */
    public GroupDetailsDialog(LauncherActivity launcherActivity, int groupPosition) {
        super(launcherActivity,  R.layout.dialog_details_group);
        this.groupPosition = groupPosition;
    }

    public AlertDialog show() {
        final Map<String, String> apps = SettingsManager.getAppGroupMap();
        final Set<String> appGroupsSet = SettingsManager.getAppGroups();
        SettingsManager settingsManager = a.settingsManager;
        final String groupName = settingsManager.getAppGroupsSorted(false).get(groupPosition);
        if (groupName == null) return null;

        AlertDialog dialog = super.show();
        if (dialog == null) return null;

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
        switchByType.put(App.Type.PHONE, dialog.findViewById(R.id.defaultPhoneSwitch));
        switchByType.put(App.Type.VR, dialog.findViewById(R.id.defaultVrSwitch));
        switchByType.put(App.Type.TV, dialog.findViewById(R.id.defaultTvSwitch));
        switchByType.put(App.Type.PANEL, dialog.findViewById(R.id.defaultPanelSwitch));
        switchByType.put(App.Type.WEB, dialog.findViewById(R.id.defaultWebsiteSwitch));

        for (App.Type type : switchByType.keySet()) {
            if (PlatformExt.getSupportedAppTypes().contains(type)) {
                Objects.requireNonNull(switchByType.get(type)).setVisibility(View.VISIBLE);

                @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch cSwitch = switchByType.get(type);
                if (cSwitch == null) continue;
                cSwitch.setChecked(SettingsManager.getDefaultGroupFor(type).equals(groupName));
                cSwitch.setOnCheckedChangeListener((switchView, value) -> {
                    String newDefault = value ? groupName : Settings.FALLBACK_GROUPS.get(type);
                    if ((!value && groupName.equals(newDefault)) || !appGroupsSet.contains(newDefault))
                        newDefault = null;
                    SettingsManager.setDefaultGroupFor(type, newDefault);
                    final boolean newChecked = SettingsManager.getDefaultGroupFor(type).equals(groupName);
                    if (newChecked && !value) {
                        cSwitch.setChecked(true);
                        BasicDialog.toast(a.getString(R.string.toast_cant_unset_group));
                    }
                    else cSwitch.setChecked(newChecked);
                });
            } else {
                Objects.requireNonNull(switchByType.get(type)).setVisibility(View.GONE);
            }
        }

        dialog.findViewById(R.id.install).setOnClickListener(view1 -> {
            String newGroupName = StringLib.setStarred(groupNameInput.getText().toString(), starred[0]);
            if (newGroupName.equals(Settings.UNSUPPORTED_GROUP)) newGroupName = "UNSUPPORTED"; // Prevent permanently hiding apps

            // Move the default group when we rename
            for (App.Type type : PlatformExt.getSupportedAppTypes())
                if (SettingsManager.getDefaultGroupFor(type).equals(groupName))
                    a.dataStoreEditor.putString(Settings.KEY_DEFAULT_GROUP + type, newGroupName);

            if (!newGroupName.isEmpty()) {
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
                a.launcherService.forEachActivity(LauncherActivity::refreshInterface);
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
                settingsManager.resetGroupsAndSort();
            } else {
                settingsManager.setAppGroups(appGroupsSet);
                Set<String> firstSelectedGroup = new HashSet<>();
                firstSelectedGroup.add(settingsManager.getAppGroupsSorted(false).get(0));
                settingsManager.setSelectedGroups(firstSelectedGroup);
            }
            dialog.dismiss();

            a.launcherService.forEachActivity(LauncherActivity::refreshInterface);
        });
        return dialog;
    }
}
