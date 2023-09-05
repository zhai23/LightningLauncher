package com.threethan.launcher;

import android.animation.ValueAnimator;
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
import android.view.animation.DecelerateInterpolator;
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
import com.threethan.launcher.ui.FadingTopScrollView;
import com.threethan.launcher.ui.GroupsAdapter;
import com.threethan.launcher.ui.ImageUtils;

import java.io.File;
import java.util.ArrayList;
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
        int background = owner.sharedPreferences.getInt(SettingsManager.KEY_BACKGROUND, SettingsManager.DEFAULT_BACKGROUND);
        if (background >= 0 && background < SettingsManager.BACKGROUND_DRAWABLES.length) {
            backgroundThemeDrawable = owner.getDrawable(SettingsManager.BACKGROUND_DRAWABLES[background]);
        } else {
            File file = new File(owner.getApplicationInfo().dataDir, MainActivity.CUSTOM_THEME);
            Bitmap backgroundBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            backgroundThemeDrawable = new BitmapDrawable(owner.getResources(), backgroundBitmap);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object _n) {
        owner.mainView.post(() -> {
            owner.backgroundImageView.setImageDrawable(backgroundThemeDrawable);
            owner.ready = true;
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
        Log.v("PackageCheck", "Checking for package changes");

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

            owner.sharedPreferences.edit().putBoolean(SettingsManager.NEEDS_META_DATA, true).apply();
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
    public static final String CUSTOM_THEME = "background.png";
    public boolean darkMode = true;
    public boolean groupsEnabled = true;
    DynamicHeightGridView appGridView;
    DynamicHeightGridView appGridViewWide;
    ScrollView scrollView;
    ImageView backgroundImageView;
    GridView groupPanelGridView;
    public SharedPreferences sharedPreferences;
    public SettingsManager settingsManager;
    private ImageView selectedImageView;
    private boolean settingsPageOpen = false;
    private boolean loaded = false;
    boolean ready  = false;
    public View mainView;
    private int prevViewWidth;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v("LauncherStartup", "1. Set View");

        setContentView(R.layout.activity_main);

        Log.v("LauncherStartup", "2. Get Setting Provider");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        settingsManager = SettingsManager.getInstance(this);
        settingsManager.checkCompatibilityUpdate(this);

        Log.v("LauncherStartup", "3. Get UI Instances");

        mainView = findViewById(R.id.mainLayout);
        mainView.addOnLayoutChangeListener((view, i, i1, i2, i3, i4, i5, i6, i7) -> updateGridViewHeights());
        appGridView = findViewById(R.id.appsView);
        appGridViewWide = findViewById(R.id.appsViewWide);
        scrollView = findViewById(R.id.mainScrollView);
        backgroundImageView = findViewById(R.id.background);
        groupPanelGridView = findViewById(R.id.groupsView);

        // Handle group click listener
        groupPanelGridView.setOnItemClickListener((parent, view, position, id) -> {
            List<String> groups = settingsManager.getAppGroupsSorted(false);
            // If the new group button was selected, create and select a new group
            if (position == groups.size()) {
                final String newName = settingsManager.addGroup();
                groups = settingsManager.getAppGroupsSorted(false);
                position = groups.indexOf(newName);
            }
            // Move apps if any are selected
            if (!currentSelectedApps.isEmpty()) {
                GroupsAdapter groupsAdapter = (GroupsAdapter) groupPanelGridView.getAdapter();
                for (String app : currentSelectedApps) groupsAdapter.setGroup(app, groups.get(position));
                currentSelectedApps.clear();
                updateSelectionHint();
            }
            settingsManager.selectGroup(groups.get(position));

            refreshInterface();
        });

        // Multiple group selection
        groupPanelGridView.setOnItemLongClickListener((parent, view, position, id) -> {
            List<String> groups = settingsManager.getAppGroupsSorted(false);
            Set<String> selectedGroups = settingsManager.getSelectedGroups();

            if (position >= groups.size() || position < 0) return false;

            String item = groups.get(position);
            if (selectedGroups.contains(item)) {
                selectedGroups.remove(item);
            } else {
                selectedGroups.add(item);
            }
            if (selectedGroups.isEmpty()) {
                view.findViewById(R.id.menu).callOnClick();
                selectedGroups.add(item);
                return true;
            }
            settingsManager.setSelectedGroups(selectedGroups);
            refreshInterface();
            return true;
        });

        // Set logo button
        ImageView settingsImageView = findViewById(R.id.settingsIcon);
        settingsImageView.setOnClickListener(view -> {
            if (!settingsPageOpen) {
                showSettings();
                settingsPageOpen = true;
            }
        });

        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter(FINISH_ACTION);
        registerReceiver(finishReceiver, filter);

        Log.v("LauncherStartup", "4. Done");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
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
                editor.putBoolean(SettingsManager.KEY_EDIT_MODE, editMode);
                editor.apply();
                refreshInterface();
            } else {
                showSettings();
                settingsPageOpen = true;
            }
        }
    }
    public static final String FINISH_ACTION = "com.threethan.launcher.FINISH";
    // Stuff to finish the activity when it's in the background;
    // More straightforward methods don't work on Quest.
    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { finish();}
    };

    @Override
    @SuppressWarnings("unchecked")
    protected void onResume() {
        resetOpenAnim();

        super.onResume();

        if (!loaded) {
            // Load Packages
            PackageManager packageManager = getPackageManager();
            allApps = packageManager.getInstalledApplications(0);
            // Load sets & check that they're not empty (Sometimes string sets are emptied on reinstall but not booleans)
            Set<String> setAll = new HashSet<>();
            sharedPreferences.getStringSet(SettingsManager.KEY_VR_SET, setAll);
            Set<String> set2d = new HashSet<>();
            sharedPreferences.getStringSet(SettingsManager.KEY_2D_SET, set2d);
            setAll.addAll(set2d);

            if (setAll.isEmpty()) sharedPreferences.edit().putBoolean(SettingsManager.NEEDS_META_DATA, true).apply();
            // Check if we need metadata and load accordingly
            final boolean needsMeta = sharedPreferences.getBoolean(SettingsManager.NEEDS_META_DATA, true);
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
                    Bitmap backgroundBitmap = ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(image.getPath()), 1280);
                    ImageUtils.saveBitmap(backgroundBitmap, new File(getApplicationInfo().dataDir, CUSTOM_THEME));
                    setBackground(SettingsManager.BACKGROUND_DRAWABLES.length);
                    break;
                }
            }
        }
    }

    void updateTopBar() {
        BlurView blurView0 = findViewById(R.id.blurView0);
        BlurView blurView1 = findViewById(R.id.blurView1);

        if (!groupsEnabled) {
            blurView0.setVisibility(View.GONE);
            blurView1.setVisibility(View.GONE);
            return;
        }
        blurView0.setVisibility(View.VISIBLE);
        blurView1.setVisibility(View.VISIBLE);

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
        int background = sharedPreferences.getInt(SettingsManager.KEY_BACKGROUND, SettingsManager.DEFAULT_BACKGROUND);
        boolean custom = background < 0 || background > SettingsManager.BACKGROUND_COLORS.length;
        int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[background];

        backgroundImageView.setBackgroundColor(backgroundColor);
        getWindow().setNavigationBarColor(backgroundColor);
        getWindow().setStatusBarColor(backgroundColor);

        new BackgroundTask().execute(this);
    }
    public void refreshInterface() {
        Log.v("LightningLauncher","Reloading UI");

        darkMode = sharedPreferences.getBoolean(SettingsManager.KEY_DARK_MODE, SettingsManager.DEFAULT_DARK_MODE);
        editMode = sharedPreferences.getBoolean(SettingsManager.KEY_EDIT_MODE, false);
        groupsEnabled = sharedPreferences.getBoolean(SettingsManager.KEY_GROUPS_ENABLED, SettingsManager.DEFAULT_GROUPS_ENABLED);
        if (!groupsEnabled && editMode) {
            groupsEnabled = true;
            updateTopBar();
        }

        ArrayList<String> selectedGroups;
        // Switch off of hidden if we just exited edit mode
        settingsManager.readValues();

        selectedGroups = settingsManager.getAppGroupsSorted(groupsEnabled);
        if (!editMode && selectedGroups.contains(GroupsAdapter.HIDDEN_GROUP)) {
            final ArrayList<String> validGroups = settingsManager.getAppGroupsSorted(false);
            validGroups.remove(GroupsAdapter.HIDDEN_GROUP);
            settingsManager.setSelectedGroups(new HashSet<>(validGroups));
        }
        if (!editMode) currentSelectedApps.clear();
        updateSelectionHint();

        // Set adapters
        mainView.post(this::setAdapters);
    }
    public void setAdapters() {
        // Get and apply margin
        int marginPx = dp(sharedPreferences.getInt(SettingsManager.KEY_MARGIN, SettingsManager.DEFAULT_MARGIN));
        boolean names = sharedPreferences.getBoolean(SettingsManager.KEY_SHOW_NAMES_ICON, SettingsManager.DEFAULT_SHOW_NAMES_ICON);
        boolean namesWide = sharedPreferences.getBoolean(SettingsManager.KEY_SHOW_NAMES_WIDE, SettingsManager.DEFAULT_SHOW_NAMES_WIDE);

        appGridView.setMargin(marginPx, names);
        appGridViewWide.setMargin(marginPx, namesWide);
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(marginPx, Math.max(0,marginPx+(groupsEnabled ? dp(-23) : 0)), marginPx, marginPx+dp(20));
        findViewById(R.id.mainScrollInterior).setLayoutParams(lp);

        appGridView.setAdapter(new AppsAdapter(this, editMode, names, squareApps));
        appGridViewWide.setAdapter(new AppsAdapter(this, editMode, namesWide, wideApps));

        scrollView.scrollTo(0,0); // Reset scroll
        scrollView.smoothScrollTo(0,0); // Cancel inertia

        groupPanelGridView.setAdapter(new GroupsAdapter(this, editMode));

        prevViewWidth = -1;
        updateGridViewHeights();
        mainView.post(this::updateTopBar);

    }
    public void runUpdater() {
        new Updater(this).checkForUpdate();
    }

    public void updateGridViewHeights() {
        if (mainView.getWidth() == prevViewWidth) return;
        prevViewWidth = mainView.getWidth();

        // Group rows and relevant values
        View scrollInterior = findViewById(R.id.mainScrollInterior);
        if (groupPanelGridView.getAdapter() != null && groupsEnabled) {
            final int group_columns = Math.min(groupPanelGridView.getAdapter().getCount(), prevViewWidth / 400);
            groupPanelGridView.setNumColumns(group_columns);
            final int groupRows = (int) Math.ceil((double) groupPanelGridView.getAdapter().getCount() / group_columns);
            scrollInterior.setPadding(0, dp(23 + 22) + dp(40) * groupRows, 0, 0);
            scrollView.setFadingEdgeLength(dp(23 + 22) + dp(40) * groupRows);
        } else {
            int marginPx = dp(sharedPreferences.getInt(SettingsManager.KEY_MARGIN, SettingsManager.DEFAULT_MARGIN));
            scrollInterior.setPadding(0, 0, 0, marginPx);
            FadingTopScrollView scrollView = findViewById(R.id.mainScrollView);
            scrollView.setFadingEdgeLength(0);
        }

        int targetSizePx = dp(sharedPreferences.getInt(SettingsManager.KEY_SCALE, SettingsManager.DEFAULT_SCALE));
        int estimatedWidth = prevViewWidth;
        appGridView.setNumColumns((int) Math.round((double) estimatedWidth/targetSizePx));
        appGridViewWide.setNumColumns((int) Math.round((double) estimatedWidth/targetSizePx/2));
    }

    public void setBackground(int index) {
        if (index >= SettingsManager.BACKGROUND_DRAWABLES.length || index < 0) index = -1;
        else sharedPreferences.edit().putBoolean(SettingsManager.KEY_DARK_MODE, SettingsManager.BACKGROUND_DARK[index]).apply();
        sharedPreferences.edit().putInt(SettingsManager.KEY_BACKGROUND, index).apply();
        refreshBackground();
    }

    @Override
    public void finish() {
        Log.i("Finishing activity!", "---");
        super.finish();
    }
    private boolean editMode = false;
    @SuppressLint("UseCompatLoadingForDrawables")
    private void showSettings() {
        settingsPageOpen = true;
        AlertDialog dialog = DialogHelper.build(this, R.layout.dialog_settings);
        dialog.setOnDismissListener(dialogInterface -> settingsPageOpen = false);

        // Functional
        dialog.findViewById(R.id.shortcut_service).setOnClickListener(view -> {
            AlertDialog subDialog = DialogHelper.build(this, R.layout.dialog_service_info);

            subDialog.findViewById(R.id.confirm).setOnClickListener(view1 -> {
                // Navigate to accessibility settings
                Intent localIntent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
                localIntent.setPackage("com.android.settings");
                startActivity(localIntent);
            });
        });
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch editSwitch = dialog.findViewById(R.id.edit_mode);
        editSwitch.setChecked(editMode);
        editSwitch.setOnClickListener(view1 -> {
            editMode = !editMode;
            ArrayList<String> selectedGroups = settingsManager.getAppGroupsSorted(true);
            if (editMode && (selectedGroups.size() > 1)) {
                Set<String> selectFirst = new HashSet<>();
                selectFirst.add(selectedGroups.get(0));
                settingsManager.setSelectedGroups(selectFirst);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsManager.KEY_EDIT_MODE, editMode);
            editor.apply();
            refreshInterface();
        });


        // Wallpaper and style
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch dark = dialog.findViewById(R.id.switch_dark_mode);
        dark.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_DARK_MODE, SettingsManager.DEFAULT_DARK_MODE));
        dark.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsManager.KEY_DARK_MODE, value);
            editor.apply();
            refreshInterface();
        });
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch groups = dialog.findViewById(R.id.switch_group_mode);
        groups.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_GROUPS_ENABLED, SettingsManager.DEFAULT_GROUPS_ENABLED));
        groups.setOnCheckedChangeListener((sw, value) -> {
            if (!sharedPreferences.getBoolean(SettingsManager.KEY_SEEN_HIDDEN_GROUPS_POPUP, false) && value != SettingsManager.DEFAULT_GROUPS_ENABLED) {
                AlertDialog subDialog = DialogHelper.build(this, R.layout.dialog_hide_groups_info);
                subDialog.findViewById(R.id.confirm).setOnClickListener(view -> {
                    sharedPreferences.edit().putBoolean(SettingsManager.KEY_SEEN_HIDDEN_GROUPS_POPUP, true)
                                            .putBoolean(SettingsManager.KEY_GROUPS_ENABLED, value).apply();
                    refreshInterface();
                    subDialog.dismiss();
                });
                subDialog.findViewById(R.id.cancel).setOnClickListener(view -> {
                    subDialog.dismiss(); // Dismiss without setting
                    groups.setChecked(SettingsManager.DEFAULT_GROUPS_ENABLED); // Revert switch
                });
            } else {
                sharedPreferences.edit().putBoolean(SettingsManager.KEY_GROUPS_ENABLED, value).apply();
                refreshInterface();
            }
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
                dialog.findViewById(R.id.background_custom)
        };
        int background = sharedPreferences.getInt(SettingsManager.KEY_BACKGROUND, SettingsManager.DEFAULT_BACKGROUND);
        if (background < 0) background = views.length-1;

        for (ImageView image : views) {
            image.setClipToOutline(true);
        }
        final int wallpaperWidth = 32;
        final int selectedWallpaperWidthPx = dp(360+20-(wallpaperWidth+4)*(views.length-1)-wallpaperWidth);
        views[background].getLayoutParams().width = selectedWallpaperWidthPx;
        views[background].requestLayout();
        for (int i = 0; i < views.length; i++) {
            int index = i;
            views[i].setOnClickListener(view -> {

                int lastIndex = sharedPreferences.getInt(SettingsManager.KEY_BACKGROUND, SettingsManager.DEFAULT_BACKGROUND);
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

                ValueAnimator lastAnimator = ValueAnimator.ofInt(last.getWidth(), dp(wallpaperWidth));
                lastAnimator.setDuration(250);
                lastAnimator.setInterpolator(new DecelerateInterpolator());
                lastAnimator.addUpdateListener(animation -> {
                    last.getLayoutParams().width = (int) animation.getAnimatedValue();
                    last.requestLayout();
                });
                lastAnimator.start();

                if (index == SettingsManager.BACKGROUND_DRAWABLES.length) {
                    ImageUtils.showImagePicker(this, PICK_THEME_CODE);
                } else {
                    setBackground(index);
                    dark.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_DARK_MODE, SettingsManager.DEFAULT_DARK_MODE));
                }
            });
        }


        // Icons & Layout
        SeekBar scale = dialog.findViewById(R.id.bar_scale);
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(SettingsManager.KEY_SCALE, value);
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
        scale.setProgress(sharedPreferences.getInt(SettingsManager.KEY_SCALE, SettingsManager.DEFAULT_SCALE));
        scale.setMax(174);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) scale.setMin(50);

        SeekBar margin = dialog.findViewById(R.id.bar_margin);
        margin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(SettingsManager.KEY_MARGIN, value);
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
        margin.setProgress(sharedPreferences.getInt(SettingsManager.KEY_MARGIN, SettingsManager.DEFAULT_MARGIN));
        margin.setMax(59);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) margin.setMin(5);



        // Wide display
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch wideVR = dialog.findViewById(R.id.switch_wide_vr);
        wideVR.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_WIDE_VR, SettingsManager.DEFAULT_WIDE_VR));
        wideVR.setOnCheckedChangeListener((compoundButton, value) -> {
            AbstractPlatform.clearIconCache();
            sharedPreferences.edit().putBoolean(SettingsManager.KEY_WIDE_VR, value).apply();
            updateAppLists();
            refreshInterface();
        });
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch wide2D = dialog.findViewById(R.id.switch_wide_2d);
        wide2D.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_WIDE_2D, SettingsManager.DEFAULT_WIDE_2D));
        wide2D.setOnCheckedChangeListener((compoundButton, value) -> {
            sharedPreferences.edit().putBoolean(SettingsManager.KEY_WIDE_2D, value).apply();
            updateAppLists();
            refreshInterface();
        });


        // Names
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch names = dialog.findViewById(R.id.switch_names);
        names.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_SHOW_NAMES_ICON, SettingsManager.DEFAULT_SHOW_NAMES_ICON));
        names.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsManager.KEY_SHOW_NAMES_ICON, value);
            editor.apply();
            refreshInterface();
        });
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch wideNames = dialog.findViewById(R.id.switch_names_wide);
        wideNames.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_SHOW_NAMES_WIDE, SettingsManager.DEFAULT_SHOW_NAMES_WIDE));
        wideNames.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsManager.KEY_SHOW_NAMES_WIDE, value);
            editor.apply();
            refreshInterface();
        });
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
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }
}