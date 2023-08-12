package com.threethan.launcher.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.threethan.launcher.MainActivity;
import com.threethan.launcher.R;
import com.threethan.launcher.SettingsProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GroupsAdapter extends BaseAdapter {
    public static final int MAX_GROUPS = 12;
    public static final String HIDDEN_GROUP = "HIDDEN!";
    private final MainActivity mainActivity;
    private final List<String> appGroups;
    private final Set<String> selectedGroups;
    private final SettingsProvider settingsProvider;
    private final boolean isEditMode;

    /**
     * Create new adapter
     */
    public GroupsAdapter(MainActivity activity, boolean editMode) {
        mainActivity = activity;
        isEditMode = editMode;
        settingsProvider = SettingsProvider.getInstance(activity);

        SettingsProvider settings = SettingsProvider.getInstance(mainActivity);
        appGroups = settings.getAppGroupsSorted(false, mainActivity);
        if (!editMode) {
            appGroups.remove(GroupsAdapter.HIDDEN_GROUP);
        }
        if (editMode && appGroups.size() < MAX_GROUPS) {
            appGroups.add("+ " + mainActivity.getString(R.string.add_group));
        }
        selectedGroups = settings.getSelectedGroups(mainActivity);
    }

    public int getCount() {
        return appGroups.size();
    }

    public String getItem(int position) {
        return appGroups.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        final TextView textView;
        final View menu;

        ViewHolder(View itemView) {
            textView = itemView.findViewById(R.id.textLabel);
            menu = itemView.findViewById(R.id.menu);
        }
    }

    private void setTextViewValue(TextView textView, String value) {
        if (HIDDEN_GROUP.equals(value)) {
            textView.setText(mainActivity.getString(R.string.apps_hidden));
        } else {
            textView.setText(value);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.lv_group, parent, false);

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        setTextViewValue(holder.textView, getItem(position));

        // set menu action
        holder.menu.getContext().getDrawable(R.drawable.ic_info);
        holder.menu.setOnClickListener(view -> {

            final Map<String, String> apps = settingsProvider.getAppList(mainActivity);
            final Set<String> appGroupsList = settingsProvider.getAppGroups(mainActivity);
            final String oldGroupName = settingsProvider.getAppGroupsSorted(false,mainActivity).get(position);

            AlertDialog dialog = new AlertDialog.Builder(mainActivity).setView(R.layout.dialog_group_details).create();

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.bkg_dialog);
            dialog.show();

            final EditText groupNameInput = dialog.findViewById(R.id.group_name);
            groupNameInput.setText(oldGroupName);

            dialog.findViewById(R.id.cancel).setOnClickListener(view1 -> {
                String newGroupName = groupNameInput.getText().toString();
                if (newGroupName.length() > 0) {
                    appGroupsList.remove(oldGroupName);
                    appGroupsList.add(newGroupName);
                    Map<String, String> updatedAppList = new HashMap<>();
                    for (String packageName : apps.keySet()) {
                        if (Objects.requireNonNull(apps.get(packageName)).compareTo(oldGroupName) == 0) {
                            updatedAppList.put(packageName, newGroupName);
                        } else {
                            updatedAppList.put(packageName, apps.get(packageName));
                        }
                    }
                    HashSet<String> selectedGroup = new HashSet<>();
                    selectedGroup.add(newGroupName);
                    settingsProvider.setSelectedGroups(selectedGroup, mainActivity);
                    settingsProvider.setAppGroups(appGroupsList, mainActivity);
                    settingsProvider.setAppList(updatedAppList, mainActivity);
                    mainActivity.reloadUI();
                }
                dialog.cancel();
            });

            dialog.findViewById(R.id.group_delete).setOnClickListener(view2 -> {
                HashMap<String, String> newAppList = new HashMap<>();
                for (String packageName : apps.keySet()) {
                    if (oldGroupName.equals(apps.get(packageName))) {
                        newAppList.put(packageName, HIDDEN_GROUP);
                    } else {
                        newAppList.put(packageName, apps.get(packageName));
                    }
                }
                settingsProvider.setAppList(newAppList, mainActivity);

                appGroupsList.remove(oldGroupName);

                if (appGroupsList.size() <= 1) {
                    settingsProvider.resetGroups();
                } else {

                    settingsProvider.setAppGroups(appGroupsList, mainActivity);

                    Set<String> firstSelectedGroup = new HashSet<>();
                    firstSelectedGroup.add(settingsProvider.getAppGroupsSorted(false,mainActivity).get(0));
                    settingsProvider.setSelectedGroups(firstSelectedGroup,mainActivity);
                }
                dialog.dismiss();

                mainActivity.reloadUI();
            });
        });

        // set the look
        setLook(position, convertView, holder.menu);

        TextView textView = convertView.findViewById(R.id.textLabel);
        setTextViewValue(textView, appGroups.get(position));

        return convertView;
    }

    public void setGroup(String packageName, String groupName, Context context) {
        Map<String, String> apps = settingsProvider.getAppList(context);
        apps.remove(packageName);
        apps.put(packageName, groupName);
        settingsProvider.setAppList(apps, context);
    }

    private void setLook(int position, View itemView, View menu) {
        boolean isSelected = selectedGroups.contains(appGroups.get(position));

        if (isSelected) {
            boolean isLeft = (position == 0) || !selectedGroups.contains(appGroups.get(position - 1));
            boolean isRight = (position + 1 >= appGroups.size()) || !selectedGroups.contains(appGroups.get(position + 1));

            int shapeResourceId;
            if (isLeft && isRight) {
                shapeResourceId = R.drawable.selected_tab;
            } else if (isLeft) {
                shapeResourceId = R.drawable.left_selected_tab;
            } else if (isRight) {
                shapeResourceId = R.drawable.right_selected_tab;
            } else {
                shapeResourceId = R.drawable.middle_selected_tab;
            }
            itemView.setBackgroundResource(shapeResourceId);
            TextView textView = itemView.findViewById(R.id.textLabel);
            textView.setTextColor(Color.parseColor("#FFFFFFFF")); // set selected tab text color
            if (isEditMode && (position < getCount() - 2)) {
                menu.setVisibility(View.VISIBLE);
            } else {
                menu.setVisibility(View.GONE);
            }
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
            TextView textView = itemView.findViewById(R.id.textLabel);
            textView.setTextColor(Color.parseColor("#98FFFFFF")); // set unselected tab text color
            menu.setVisibility(View.GONE);
        }
    }
}