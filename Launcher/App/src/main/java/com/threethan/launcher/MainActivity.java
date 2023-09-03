package com.threethan.launcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.threethan.launcher.platforms.AbstractPlatform;
import com.threethan.launcher.ui.AppsAdapter;
import com.threethan.launcher.ui.DialogHelper;
import com.threethan.launcher.ui.DynamicHeightGridView;
import com.threethan.launcher.ui.GroupsAdapter;
import com.threethan.launcher.ui.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

/** @noinspection deprecation */
class BackgroundTask extends AsyncTask<Object, Void, Object> {

    Drawable backgroundThemeDrawable;
    @SuppressLint("StaticFieldLeak")
    MainActivity owner;
    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected Object doInBackground(Object... objects) {
        owner = (MainActivity) objects[0];
        int backgroundThemeIndex = owner.sharedPreferences.getInt(SettingsProvider.KEY_BACKGROUND, SettingsProvider.DEFAULT_BACKGROUND);
        if (backgroundThemeIndex < SettingsProvider.BACKGROUND_DRAWABLES.length) {
            backgroundThemeDrawable = owner.getDrawable(SettingsProvider.BACKGROUND_DRAWABLES[backgroundThemeIndex]);
        } else {
            File file = new File(owner.getApplicationInfo().dataDir, MainActivity.CUSTOM_THEME);
            Bitmap themeBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            backgroundThemeDrawable = new BitmapDrawable(owner.getResources(), themeBitmap);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object _n) {
        owner.mainView.post(() -> {
            owner.backgroundImageView.setImageDrawable(backgroundThemeDrawable);
            owner.ready = true;
            owner.initBlur();
        });
    }

}

/** @noinspection deprecation, rawtypes */
class RecheckPackagesTask extends AsyncTask {

    List<ApplicationInfo> foundApps;
    @SuppressLint("StaticFieldLeak")
    MainActivity owner;
    boolean changeFound;
    @Override
    protected Object doInBackground(Object[] objects) {
        Log.i("PackageCheck", "Checking for package changes");

        owner = (MainActivity) objects[0];

        PackageManager packageManager = owner.getPackageManager();
        foundApps = packageManager.getInstalledApplications(0);

        changeFound = owner.allApps.size() != foundApps.size();
        return null;
    }
    @Override
    protected void onPostExecute(Object _n) {
        if (changeFound) {
            Log.i("PackageCheck", "Package change detected!");

            owner.sharedPreferences.edit().putBoolean(SettingsProvider.NEEDS_META_DATA, true).apply();
            owner.allApps = foundApps;
            owner.updateAppLists();
            owner.refreshInterface();
        }
    }
}
/** @noinspection deprecation*/
public class MainActivity extends Activity {
    public static final int PICK_ICON_CODE = 450;
    public static final int PICK_THEME_CODE = 95;
    public static final String CUSTOM_THEME = "theme.png";
    public boolean darkMode = false;
    DynamicHeightGridView appGridView;
    DynamicHeightGridView appGridViewWide;
    ScrollView scrollView;
    ImageView backgroundImageView;
    GridView groupPanelGridView;
    public SharedPreferences sharedPreferences;
    private SettingsProvider settingsProvider;
    private ImageView selectedImageView;
    private boolean settingsPageOpen = false;
    private boolean lookPageOpen = false;
    private boolean loaded = false;
    boolean ready  = false;
    public View mainView;
    private int prevViewWidth;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("LauncherStartup", "1. Set View");

        setContentView(R.layout.activity_main);

        Log.i("LauncherStartup", "2. Get Setting Provider");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        settingsProvider = SettingsProvider.getInstance(this);
        settingsProvider.checkCompatibilityUpdate(this);

        Log.i("LauncherStartup", "3. Get UI Instances");

        mainView = findViewById(R.id.mainLayout);
        mainView.addOnLayoutChangeListener((view, i, i1, i2, i3, i4, i5, i6, i7) -> updateGridViewHeights());
        appGridView = findViewById(R.id.appsView);
        appGridViewWide = findViewById(R.id.appsViewWide);
        scrollView = findViewById(R.id.scrollView);
        backgroundImageView = findViewById(R.id.background);
        groupPanelGridView = findViewById(R.id.groupsView);

        // Handle group click listener
        groupPanelGridView.setOnItemClickListener((parent, view, position, id) -> {
            List<String> groups = settingsProvider.getAppGroupsSorted(false,getApplicationContext());
            // If the new group button was selected, create and select a new group
            if (position == groups.size()) {
                final String newName = settingsProvider.addGroup(getApplicationContext());
                groups = settingsProvider.getAppGroupsSorted(false,getApplicationContext());
                position = groups.indexOf(newName);
            }
            // Move apps if any are selected
            if (!currentSelectedApps.isEmpty()) {
                GroupsAdapter groupsAdapter = (GroupsAdapter) groupPanelGridView.getAdapter();
                for (String app : currentSelectedApps) groupsAdapter.setGroup(app, groups.get(position), getApplicationContext());
                currentSelectedApps.clear();
                updateSelectionHint();
            }
            settingsProvider.selectGroup(groups.get(position), getApplicationContext());

            refreshInterface();
        });

        // Multiple group selection
        groupPanelGridView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!sharedPreferences.getBoolean(SettingsProvider.KEY_EDIT_MODE, false)) {
                List<String> groups = settingsProvider.getAppGroupsSorted(false, getApplicationContext());
                Set<String> selectedGroups = settingsProvider.getSelectedGroups(getApplicationContext());

                String item = groups.get(position);
                if (selectedGroups.contains(item)) {
                    selectedGroups.remove(item);
                } else {
                    selectedGroups.add(item);
                }
                if (selectedGroups.isEmpty()) {
                    selectedGroups.add(groups.get(0));
                }
                settingsProvider.setSelectedGroups(selectedGroups,getApplicationContext());
                refreshInterface();
            }
            return true;
        });

        // Set logo button
        ImageView settingsImageView = findViewById(R.id.settingsIcon);
        settingsImageView.setOnClickListener(view -> {
            if (!settingsPageOpen) {
                showSettingsMain();
                settingsPageOpen = true;
            }
        });

        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter(FINISH_ACTION);
        registerReceiver(finishReceiver, filter);
        IntentFilter filter2 = new IntentFilter(DONT_FINISH_ACTION);
        registerReceiver(dontFinishReceiver, filter2);

        Log.i("LauncherStartup", "4. Done");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
        unregisterReceiver(dontFinishReceiver);
    }

    private boolean resetOpenAnim() {
        final boolean rv = (findViewById(R.id.openAnim).getVisibility() == View.VISIBLE);
        findViewById(R.id.openAnim).setVisibility(View.INVISIBLE);
        findViewById(R.id.openAnim).setScaleX(1);
        findViewById(R.id.openAnim).setScaleY(1);
        findViewById(R.id.openIcon).setAlpha(1.0f);
        findViewById(R.id.openProgress).setVisibility(View.INVISIBLE);
        return rv;
    }
    @Override
    public void onBackPressed() {
        if (resetOpenAnim()) return;
        if (!settingsPageOpen) {
            if (editMode) {
                editMode = false;
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(SettingsProvider.KEY_EDIT_MODE, editMode);
                editor.apply();
                refreshInterface();
            } else {
                showSettingsMain();
                settingsPageOpen = true;
            }
        }
    }

    public static final String FINISH_ACTION = "com.threethan.launcher.FINISH";
    public static final String DONT_FINISH_ACTION = "com.threethan.launcher.DONT_FINISH";

    // Stuff to finish the activity when it's in the background;
    // More straightforward methods don't work on Quest.
    private boolean canFinishOnStop;
    private boolean canFinishOnIntent;
    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { canFinishOnStop = true; if (canFinishOnIntent) finish();}
    };
    private final BroadcastReceiver dontFinishReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {canFinishOnStop = false;}
    };
    @Override
    protected void onStop() {
        super.onStop();
        if (canFinishOnStop) finish();
        else canFinishOnIntent = true;
    }
    @Override
    @SuppressWarnings("unchecked")
    protected void onResume() {
        canFinishOnIntent = false;
        resetOpenAnim();

        super.onResume();

        if (!loaded) {
            // Load Packages
            PackageManager packageManager = getPackageManager();
            allApps = packageManager.getInstalledApplications(0);
            // Load sets & check that they're not empty (Sometimes string sets are emptied on reinstall but not booleans)
            Set<String> setAll = new HashSet<>();
            sharedPreferences.getStringSet(SettingsProvider.KEY_VR_SET, setAll);
            Set<String> set2d = new HashSet<>();
            sharedPreferences.getStringSet(SettingsProvider.KEY_2D_SET, set2d);
            setAll.addAll(set2d);

            if (setAll.isEmpty()) sharedPreferences.edit().putBoolean(SettingsProvider.NEEDS_META_DATA, true).apply();
            // Check if we need metadata and load accordingly
            final boolean needsMeta = sharedPreferences.getBoolean(SettingsProvider.NEEDS_META_DATA, true);
            Log.i("LightningLauncher", needsMeta ? "(Re)Loading app list with meta data" : "Loading saved package list (no meta data)");
            if (needsMeta) {
                allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            } else {
                // If we don't need metadata, just use package names.
                // This is ~200ms faster than no meta data, and ~300ms faster than with meta data
                allApps = new ArrayList<>();
                for (String packageName : setAll) {
                    ApplicationInfo applicationInfo = new ApplicationInfo();
                    applicationInfo.packageName = packageName;
                    allApps.add(applicationInfo);
                }
            }

            updateAppLists();
            // Reload UI
            mainView.postDelayed(this::runUpdater, 1000);
            refreshBackground();
            refreshInterface();
            loaded = true;
        } else {
            new RecheckPackagesTask().execute(this);
        }
    }
    public void setSelectedImageView(ImageView imageView) {
        selectedImageView = imageView;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ICON_CODE) {
            if (resultCode == RESULT_OK) {
                for (Image image : ImagePicker.getImages(data)) {
                    ((AppsAdapter) appGridView.getAdapter()).onImageSelected(image.getPath(), selectedImageView);
                    break;
                }
            } else {
                ((AppsAdapter) appGridView.getAdapter()).onImageSelected(null, selectedImageView);
            }
        } else if (requestCode == PICK_THEME_CODE) {
            if (resultCode == RESULT_OK) {

                for (Image image : ImagePicker.getImages(data)) {
                    Bitmap themeBitmap = ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(image.getPath()), 1280);
                    ImageUtils.saveBitmap(themeBitmap, new File(getApplicationInfo().dataDir, CUSTOM_THEME));
                    setBackground(SettingsProvider.BACKGROUND_DRAWABLES.length);
                    break;
                }
            }
        }
    }

    void initBlur() {
        BlurView blurView0 = findViewById(R.id.blurView0);
        BlurView blurView1 = findViewById(R.id.blurView1);

        blurView0.setOverlayColor(Color.parseColor(darkMode ? "#4A000000" : "#50FFFFFF"));
        blurView1.setOverlayColor(Color.parseColor(darkMode ? "#4A000000" : "#50FFFFFF"));

        ImageView settingsIcon = findViewById(R.id.settingsIcon);
        settingsIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#FFFFFF" : "#000000")));

        float blurRadiusDp = 15f;

        View windowDecorView = getWindow().getDecorView();
        ViewGroup rootViewGroup = windowDecorView.findViewById(android.R.id.content);

        Drawable windowBackground = windowDecorView.getBackground();

        blurView0.setupWith(rootViewGroup, new RenderScriptBlur(getApplicationContext())) // or RenderEffectBlur
                .setFrameClearDrawable(windowBackground) // Optional
                .setBlurRadius(blurRadiusDp);
        blurView1.setupWith(rootViewGroup, new RenderScriptBlur(getApplicationContext())) // or RenderEffectBlur
                .setFrameClearDrawable(windowBackground) // Optional
                .setBlurRadius(blurRadiusDp);

        blurView0.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        blurView0.setClipToOutline(true);
        blurView1.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        blurView1.setClipToOutline(true);

        // Update then deactivate bv
        blurView0.setActivated(false);
        blurView0.setActivated(true);
        blurView0.setActivated(false);

        // Update then deactivate bv
        blurView1.setActivated(false);
        blurView1.setActivated(true);
        blurView1.setActivated(false);

    }

    List<ApplicationInfo> allApps;
    List<ApplicationInfo> wideApps;
    List<ApplicationInfo> squareApps;

    public void refreshBackground() {
        // Set initial color, execute background task
        int backgroundThemeIndex = sharedPreferences.getInt(SettingsProvider.KEY_BACKGROUND, SettingsProvider.DEFAULT_BACKGROUND);
        if (backgroundThemeIndex < SettingsProvider.BACKGROUND_DRAWABLES.length) {
            backgroundImageView.setBackgroundColor(SettingsProvider.BACKGROUND_COLORS[backgroundThemeIndex]);
            getWindow().setNavigationBarColor(SettingsProvider.BACKGROUND_COLORS[backgroundThemeIndex]);
            getWindow().setStatusBarColor(SettingsProvider.BACKGROUND_COLORS[backgroundThemeIndex]);
        }
        new BackgroundTask().execute(this);
    }
    public void refreshInterface() {
        Log.i("LightningLauncher","Reloading UI");

        darkMode = sharedPreferences.getBoolean(SettingsProvider.KEY_DARK_MODE, SettingsProvider.DEFAULT_DARK_MODE);
        editMode = sharedPreferences.getBoolean(SettingsProvider.KEY_EDIT_MODE, false);

        // Switch off of hidden if we just exited edit mode
        settingsProvider.readValues(getApplicationContext());
        ArrayList<String> selectedGroups = settingsProvider.getAppGroupsSorted(true, getApplicationContext());
        if (!editMode && selectedGroups.contains(GroupsAdapter.HIDDEN_GROUP))
            settingsProvider.setSelectedGroups(Collections.singleton(settingsProvider.getAppGroupsSorted(false, getApplicationContext()).get(0)), getApplicationContext());
        if (!editMode) currentSelectedApps.clear();
        updateSelectionHint();

        // Set adapters
        mainView.post(this::setAdapters);
    }
    public void setAdapters() {
        // Get and apply margin
        int marginValue = getPixelFromDip(sharedPreferences.getInt(SettingsProvider.KEY_MARGIN, SettingsProvider.DEFAULT_MARGIN));
        boolean names = sharedPreferences.getBoolean(SettingsProvider.KEY_SHOW_NAMES_ICON, SettingsProvider.DEFAULT_SHOW_NAMES_ICON);
        boolean namesWide = sharedPreferences.getBoolean(SettingsProvider.KEY_SHOW_NAMES_WIDE, SettingsProvider.DEFAULT_SHOW_NAMES_WIDE);

        appGridView.setMargin(marginValue, names);
        appGridViewWide.setMargin(marginValue, namesWide);
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(marginValue, Math.max(0,marginValue-dp(23)), marginValue, marginValue+dp(20));
        findViewById(R.id.scrollerLayout).setLayoutParams(lp);

        appGridView.setAdapter(new AppsAdapter(this, editMode, names, squareApps));
        appGridViewWide.setAdapter(new AppsAdapter(this, editMode, namesWide, wideApps));

        scrollView.scrollTo(0,0); // Reset scroll
        scrollView.smoothScrollTo(0,0); // Cancel inertia

        groupPanelGridView.setAdapter(new GroupsAdapter(this, editMode));

        prevViewWidth = -1;
        updateGridViewHeights();
    }
    public void runUpdater() {
        new Updater(this).checkForUpdate();
    }

    public void updateGridViewHeights() {
        if (mainView.getWidth() == prevViewWidth) return;
        prevViewWidth = mainView.getWidth();

        // Group rows and relevant values
        if (groupPanelGridView.getAdapter() != null) {
            final int group_columns = Math.min(groupPanelGridView.getAdapter().getCount(), prevViewWidth / 400);
            groupPanelGridView.setNumColumns(group_columns);
            final int group_rows = (int) Math.ceil((double) groupPanelGridView.getAdapter().getCount() / group_columns);
            View scrollInterior = findViewById(R.id.scrollerLayout);
            scrollInterior.setPadding(0, dp(23 + 22) + dp(40) * group_rows, 0, 0);
        }

        int scaleValue = dp(sharedPreferences.getInt(SettingsProvider.KEY_SCALE, SettingsProvider.DEFAULT_SCALE));
        int estimatedWidth = prevViewWidth;
        appGridView.setNumColumns((int) Math.round((double) estimatedWidth/scaleValue));
        appGridViewWide.setNumColumns((int) Math.round((double) estimatedWidth/scaleValue/2));
    }

    public void setBackground(int index) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SettingsProvider.KEY_BACKGROUND, index);
        editor.putBoolean(SettingsProvider.KEY_DARK_MODE, SettingsProvider.BACKGROUND_DARK[index]);
        editor.apply();
        refreshBackground();
    }

    @Override
    public void finish() {
        Log.i("Finishing activity!", "---");
        super.finish();
    }
    private boolean editMode = false;

    private void showSettingsMain() {
        AlertDialog dialog = DialogHelper.build(this, R.layout.dialog_settings);
        dialog.setOnDismissListener(dialogInterface -> settingsPageOpen = false);

        ImageView editIcon = dialog.findViewById(R.id.settings_edit_image);
        TextView editText = dialog.findViewById(R.id.settings_edit_text);
        editIcon.setImageResource(editMode ? R.drawable.ic_editing_on : R.drawable.ic_editing_off);
        editText.setText(editMode ? R.string.edit_on : R.string.edit_off);
        dialog.findViewById(R.id.settings_edit).setOnClickListener(view1 -> {
            editMode = !editMode;
            prevViewWidth = 0; // Indicates the need to update top padding
            ArrayList<String> selectedGroups = settingsProvider.getAppGroupsSorted(true, getApplicationContext());
            if (editMode && (selectedGroups.size() > 1)) {
                Set<String> selectFirst = new HashSet<>();
                selectFirst.add(selectedGroups.get(0));
                settingsProvider.setSelectedGroups(selectFirst, getApplicationContext());
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_EDIT_MODE, editMode);
            editor.apply();
            refreshInterface();
            editIcon.setImageResource(editMode ? R.drawable.ic_editing_on : R.drawable.ic_editing_off);
            editText.setText(editMode ? R.string.edit_on : R.string.edit_off);
        });

        dialog.findViewById(R.id.settings_look).setOnClickListener(view -> {
            if (!lookPageOpen) {
                showSettingsLook();
            }
        });

        dialog.findViewById(R.id.settings_service).setOnClickListener(view -> {
            AlertDialog subDialog = DialogHelper.build(this, R.layout.dialog_service_info);

            subDialog.findViewById(R.id.confirm).setOnClickListener(view1 -> {
                // Navigate to accessibility settings
                Intent localIntent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
                localIntent.setPackage("com.android.settings");
                startActivity(localIntent);
            });
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void showSettingsLook() {
        lookPageOpen = true;

        AlertDialog dialog = DialogHelper.build(this, R.layout.dialog_look);
        dialog.setOnDismissListener(dialogInterface -> lookPageOpen = false);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch dark = dialog.findViewById(R.id.switch_dark_mode);
        dark.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_DARK_MODE, SettingsProvider.DEFAULT_DARK_MODE));
        dark.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_DARK_MODE, value);
            editor.apply();
            refreshInterface();
        });

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch names = dialog.findViewById(R.id.switch_names);
        names.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_SHOW_NAMES_ICON, SettingsProvider.DEFAULT_SHOW_NAMES_ICON));
        names.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_SHOW_NAMES_ICON, value);
            editor.apply();
            refreshInterface();
        });
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch wideNames = dialog.findViewById(R.id.switch_names_wide);
        wideNames.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_SHOW_NAMES_WIDE, SettingsProvider.DEFAULT_SHOW_NAMES_WIDE));
        wideNames.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_SHOW_NAMES_WIDE, value);
            editor.apply();
            refreshInterface();
        });

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch wideVR = dialog.findViewById(R.id.switch_wide_vr);
        wideVR.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_WIDE_VR, SettingsProvider.DEFAULT_WIDE_VR));
        wideVR.setOnCheckedChangeListener((compoundButton, value) -> {
            AbstractPlatform.clearIconCache();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_WIDE_VR, value);
            editor.apply();
            updateAppLists();
            refreshInterface();
        });
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch wide2D = dialog.findViewById(R.id.switch_wide_2d);
        wide2D.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_WIDE_2D, SettingsProvider.DEFAULT_WIDE_2D));
        wide2D.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_WIDE_2D, value);
            editor.apply();
            updateAppLists();
            refreshInterface();
        });

        SeekBar scale = dialog.findViewById(R.id.bar_scale);
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(SettingsProvider.KEY_SCALE, value);
                editor.apply();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                refreshInterface();
            }
        });
        scale.setProgress(sharedPreferences.getInt(SettingsProvider.KEY_SCALE, SettingsProvider.DEFAULT_SCALE));
        scale.setMax(174);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) scale.setMin(50);

        SeekBar margin = dialog.findViewById(R.id.bar_margin);
        margin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(SettingsProvider.KEY_MARGIN, value);
                editor.apply();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                refreshInterface();
            }
        });
        margin.setProgress(sharedPreferences.getInt(SettingsProvider.KEY_MARGIN, SettingsProvider.DEFAULT_MARGIN));
        margin.setMax(59);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) margin.setMin(5);

        int theme = sharedPreferences.getInt(SettingsProvider.KEY_BACKGROUND, SettingsProvider.DEFAULT_BACKGROUND);
        ImageView[] views = {
                dialog.findViewById(R.id.theme0),
                dialog.findViewById(R.id.theme1),
                dialog.findViewById(R.id.theme2),
                dialog.findViewById(R.id.theme3),
                dialog.findViewById(R.id.theme4),
                dialog.findViewById(R.id.theme5),
                dialog.findViewById(R.id.theme_custom)
        };

        for (ImageView image : views) {
            image.setBackground(getDrawable(R.drawable.bkg_button_wallpaper));
            image.setImageAlpha(255);
            image.setClipToOutline(true);
        }
        views[theme].setBackground(getDrawable(R.drawable.bkg_button_wallpaper_sel));
        views[theme].setImageAlpha(192);
        for (int i = 0; i < views.length; i++) {
            int index = i;
            views[i].setOnClickListener(view -> {
                if (index == SettingsProvider.BACKGROUND_DRAWABLES.length) {
                    ImageUtils.showImagePicker(this, PICK_THEME_CODE);
                } else {
                    setBackground(index);
                    dark.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_DARK_MODE, SettingsProvider.DEFAULT_DARK_MODE));
                }
                for (ImageView image : views) {
                    image.setBackground(getDrawable(R.drawable.bkg_button_wallpaper));
                    image.setImageAlpha(255);
                }
                views[index].setBackground(getDrawable(R.drawable.bkg_button_wallpaper_sel));
                views[index].setImageAlpha(192);
            });
        }
    }

    int getPixelFromDip(int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }

    public void openApp(ApplicationInfo app) {
        //open the app
        AbstractPlatform platform = AbstractPlatform.getPlatform(app);
        platform.runApp(this, app);
    }

    public void openAppDetails(String pkg) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + pkg));
        startActivity(intent);
    }
    public void uninstallApp(String pkg) {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + pkg));
        startActivity(intent);
    }

    // Edit Mode
    Set<String> currentSelectedApps = new HashSet<>();
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

    void updateSelectionHint() {
        TextView selectionHint = findViewById(R.id.SelectionHint);

        final int size = currentSelectedApps.size();
        if (size == 1) selectionHint.setText(R.string.selection_hint);
        else selectionHint.setText(getString(R.string.selection_hint_multiple, size));
        selectionHint.setVisibility(currentSelectedApps.isEmpty() ? View.INVISIBLE : View.VISIBLE);
    }

    void updateAppLists() {
        wideApps = new ArrayList<>();
        squareApps = new ArrayList<>();
        for (ApplicationInfo app: allApps) {
            if (AbstractPlatform.isWideApp(app, this)) wideApps.add(app);
            else squareApps.add(app);
        }
    }

    public int dp(float dip) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.getDisplayMetrics()
        );
        return ((int) px);
    }
}