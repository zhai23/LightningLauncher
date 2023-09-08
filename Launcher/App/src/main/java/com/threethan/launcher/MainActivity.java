package com.threethan.launcher;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.threethan.launcher.helpers.CompatHelper;
import com.threethan.launcher.helpers.SettingsManager;
import com.threethan.launcher.helpers.Updater;
import com.threethan.launcher.platforms.AbstractPlatform;
import com.threethan.launcher.ui.AppsAdapter;
import com.threethan.launcher.ui.DialogHelper;
import com.threethan.launcher.ui.DynamicHeightGridView;
import com.threethan.launcher.ui.FadingTopScrollView;
import com.threethan.launcher.ui.GroupsAdapter;
import com.threethan.launcher.ui.ImageUtils;
import com.threethan.launcher.web.WebViewActivity;
import com.threethan.launcher.web.WebViewService;

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
        owner.post(() -> {
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

        changeFound = owner.installedApps.size() != foundApps.size();
        return null;
    }
    @Override
    protected void onPostExecute(Object _n) {
        if (changeFound) {
            Log.i("PackageCheck", "Package change detected!");
            owner.sharedPreferenceEditor.putBoolean(SettingsManager.NEEDS_META_DATA, true);
            owner.reloadPackages();
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
    DynamicHeightGridView appGridViewIcon;
    DynamicHeightGridView appGridViewWide;
    ScrollView scrollView;
    ImageView backgroundImageView;
    GridView groupPanelGridView;
    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor sharedPreferenceEditor;
    public SettingsManager settingsManager;
    private ImageView selectedImageView;
    private boolean settingsPageOpen = false;
    private boolean loaded = false;
    boolean ready  = false;
    public View mainView;
    public View fadeView;
    private int prevViewWidth;
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v("LauncherStartup", "1A. Set View");

        setContentView(R.layout.activity_main);

        Log.v("LauncherStartup", "1B. Get Setting Provider");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        sharedPreferenceEditor = sharedPreferences.edit();

        settingsManager = SettingsManager.getInstance(this);
        CompatHelper.checkCompatibilityUpdate(this);

        Log.v("LauncherStartup", "1C. Get UI Instances");

        mainView = findViewById(R.id.mainLayout);
        mainView.addOnLayoutChangeListener((view, i, i1, i2, i3, i4, i5, i6, i7) -> updateGridViewHeights());
        appGridViewIcon = findViewById(R.id.appsViewIcon);
        appGridViewWide = findViewById(R.id.appsViewWide);
        scrollView = findViewById(R.id.mainScrollView);
        fadeView = scrollView;
        backgroundImageView = findViewById(R.id.background);
        groupPanelGridView = findViewById(R.id.groupsView);
        post(new Runnable() {
            @Override
            public void run() {
                sharedPreferenceEditor.apply();
                post(this);
            }
        });

        // Handle group click listener
        groupPanelGridView.setOnItemClickListener((parent, view, position, id) -> {
            List<String> groups = settingsManager.getAppGroupsSorted(false);

            // If the new group button was selected, create and select a new group
            if (position == groups.size()) {
                final String newName = settingsManager.addGroup();
                groups = settingsManager.getAppGroupsSorted(false);
                position = groups.indexOf(newName);
                refreshInterface();

            }
            final String group = groups.get(position);
            // Move apps if any are selected
            if (!currentSelectedApps.isEmpty()) {
                GroupsAdapter groupsAdapter = (GroupsAdapter) groupPanelGridView.getAdapter();
                for (String app : currentSelectedApps) groupsAdapter.setGroup(app, group);
                TextView selectionHint = findViewById(R.id.selectionHint);
                selectionHint.setText( currentSelectedApps.size()==1 ?
                    getString(R.string.selection_moved_single, group) :
                    getString(R.string.selection_moved_multiple, currentSelectedApps.size(), group)
                );
                selectionHint.postDelayed(this::updateSelectionHint, 2000);
                currentSelectedApps.clear();
                settingsManager.setSelectedGroups(Collections.singleton(group));
                refreshInterface();
            } else if (settingsManager.selectGroup(group)) refreshInterface();
            else recheckPackages(); // If clicking on the same single group, check if there are any new packages
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
        View addWebsiteButton = findViewById(R.id.addWebsite);
        addWebsiteButton.setOnClickListener(view -> addWebsite());
        View stopEditingButton = findViewById(R.id.stopEditing);
        stopEditingButton.setOnClickListener(view -> setEditMode(false));

        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter(FINISH_ACTION);
        registerReceiver(finishReceiver, filter);

        Log.v("LauncherStartup", "1D. Init Done");
    }
    public void setEditMode(boolean value) {
        editMode = value;
        sharedPreferenceEditor.putBoolean(SettingsManager.KEY_EDIT_MODE, editMode);
        refreshInterface();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
        unbindWebViewService();
    }
    @Override
    public void onBackPressed() {
        if (AppsAdapter.animateClose(this)) return;
        if (!settingsPageOpen) setEditMode(!editMode);
    }
    public static final String FINISH_ACTION = "com.threethan.launcher.FINISH";
    // Stuff to finish the activity when it's in the background;
    // More straightforward methods don't work on Quest.
    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { finish();}
    };

    @Override
    protected void onResume() {
        AppsAdapter.animateClose(this);

        super.onResume();

        if (!loaded) {
            // Load Packages
            reloadPackages();
            reloadPackages();
            // Reload UI
            mainView.postDelayed(this::runUpdater, 1000);
            refreshBackground();
            refreshInterface();
            loaded = true;
        } else {
            recheckPackages();
            bindWebViewService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindWebViewService();
    }

    public void reloadPackages() {
        sharedPreferenceEditor.apply();
        boolean needsMeta = sharedPreferences.getBoolean(SettingsManager.NEEDS_META_DATA, true);
        // Load sets & check that they're not empty (Sometimes string sets are emptied on reinstall but not booleans)
        HashSet<String> setAll = AbstractPlatform.getAllPackages(this);
        // Check if we need metadata and load accordingly
        needsMeta = setAll.isEmpty() || needsMeta;
        Log.i("LightningLauncher", needsMeta ? "(Re)Loading app list with meta data" : "Loading saved package list (no meta data)");
        if (needsMeta) {
            sharedPreferenceEditor
                    .remove(SettingsManager.KEY_VR_SET)
                    .remove(SettingsManager.KEY_2D_SET)
                    ;
            AbstractPlatform.clearPackageLists();
            PackageManager packageManager = getPackageManager();
            installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        } else {
            // If we don't need metadata, just use package names.
            // This is ~200ms faster than no meta data, and ~300ms faster than with meta data
            installedApps = new ArrayList<>();
            for (String packageName : setAll) {
                ApplicationInfo applicationInfo = new ApplicationInfo();
                applicationInfo.packageName = packageName;
                installedApps.add(applicationInfo);
            }
        }
        updateAppLists();
    }
    @SuppressWarnings("unchecked")
    public void recheckPackages() {
        try {
            new RecheckPackagesTask().execute(this);
        } catch (Exception ignore) {
            Log.w("LightningLauncher", "Exception while starting recheck package task");
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
                    ((AppsAdapter) appGridViewIcon.getAdapter()).onImageSelected(image.getPath(), selectedImageView);
                    break;
                }
            } else {
                ((AppsAdapter) appGridViewIcon.getAdapter()).onImageSelected(null, selectedImageView);
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
        Log.v("LauncherStartup","3A. Update TopBar");

        BlurView blurView0 = findViewById(R.id.blurView0);
        BlurView blurView1 = findViewById(R.id.blurView1);

        bindWebViewService();

        if (!groupsEnabled) {
            blurView0.setVisibility(View.GONE);
            blurView1.setVisibility(View.GONE);
            animateIn();
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

        animateIn();
    }
    boolean fullyLoaded = false;
    private void animateIn() {
        // Animate opacity
        ValueAnimator an = android.animation.ObjectAnimator.ofFloat(fadeView, "alpha", 1f);
        an.setDuration(fullyLoaded ? 120 : 300);
        fadeView.post(an::start);
        fullyLoaded = true;
    }
    List<ApplicationInfo> installedApps;
    List<ApplicationInfo> wideApps;
    List<ApplicationInfo> iconApps;

    public void refreshBackground() {
        sharedPreferenceEditor.apply();

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
        try {
            post(this::refreshInterfaceInternal);
        } catch (Exception ignored) {
            Log.w("LauncherStartup", "Failed to post refresh refreshInterfaceInternal. Called by something else?");
        }
    }
    private void refreshInterfaceInternal() {
        sharedPreferenceEditor.apply();

        Log.v("LauncherStartup","2A. Refreshing Interface");
        fadeView.setAlpha(0); // Set opacity for animation

        darkMode = sharedPreferences.getBoolean(SettingsManager.KEY_DARK_MODE, SettingsManager.DEFAULT_DARK_MODE);
        editMode = sharedPreferences.getBoolean(SettingsManager.KEY_EDIT_MODE, false);
        groupsEnabled = sharedPreferences.getBoolean(SettingsManager.KEY_GROUPS_ENABLED, SettingsManager.DEFAULT_GROUPS_ENABLED);
        if (!groupsEnabled && editMode) {
            groupsEnabled = true;
            updateTopBar();
        }

        ArrayList<String> selectedGroups;

        selectedGroups = settingsManager.getAppGroupsSorted(groupsEnabled);
        if (!editMode && selectedGroups.contains(GroupsAdapter.HIDDEN_GROUP)) {
            final ArrayList<String> validGroups = settingsManager.getAppGroupsSorted(false);
            validGroups.remove(GroupsAdapter.HIDDEN_GROUP);
            settingsManager.setSelectedGroups(new HashSet<>(validGroups));
        }

        final View editFooter = findViewById(R.id.editFooter);
        if (editMode) { // Edit bar theming
            editFooter.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#60000000" : "#70BeBeBe")));
            TextView selectionHint = findViewById(R.id.selectionHint);
            for (TextView textView: new TextView[]{selectionHint, findViewById(R.id.addWebsite), findViewById(R.id.stopEditing)}) {
                textView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#3a3a3c" : "#FFFFFF")));
                textView.setTextColor(Color.parseColor(darkMode ? "#FFFFFF" : "#000000"));
            }
            selectionHint.setOnClickListener((view) -> {
                if (currentSelectedApps.isEmpty()) {
                    final Adapter appAdapterIcon = ((GridView) findViewById(R.id.appsViewIcon)).getAdapter();
                    for (int i=0; i<appAdapterIcon.getCount(); i++) currentSelectedApps.add(((ApplicationInfo) appAdapterIcon.getItem(i)).packageName);
                    final Adapter appsAdapterWide = ((GridView) findViewById(R.id.appsViewWide)).getAdapter();
                    for (int i=0; i<appsAdapterWide.getCount(); i++) currentSelectedApps.add(((ApplicationInfo) appsAdapterWide.getItem(i)).packageName);
                    selectionHint.setText(R.string.selection_hint_all);
                } else {
                    currentSelectedApps.clear();
                    selectionHint.setText(R.string.selection_hint_cleared);
                }
                selectionHint.postDelayed(this::updateSelectionHint, 2000);
            });
        }
        editFooter.setVisibility(editMode ? View.VISIBLE : View.GONE);

        // Set adapters
        Log.v("LauncherStartup","2B. Post Next Step");

        post(this::setAdapters);
        if (!editMode) {
            currentSelectedApps.clear();
            updateSelectionHint();
        }
    }
    public void setAdapters() {
        Log.v("LauncherStartup","3A. Get Adapter Preferences");

        // Get and apply margin
        int marginPx = dp(sharedPreferences.getInt(SettingsManager.KEY_MARGIN, SettingsManager.DEFAULT_MARGIN));
        boolean names = sharedPreferences.getBoolean(SettingsManager.KEY_SHOW_NAMES_ICON, SettingsManager.DEFAULT_SHOW_NAMES_ICON);
        boolean namesWide = sharedPreferences.getBoolean(SettingsManager.KEY_SHOW_NAMES_WIDE, SettingsManager.DEFAULT_SHOW_NAMES_WIDE);


        appGridViewIcon.setMargin(marginPx, names);
        appGridViewWide.setMargin(marginPx, namesWide);
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(marginPx, Math.max(0,marginPx+(groupsEnabled ? dp(-23) : 0)), marginPx, marginPx+dp(20));
        findViewById(R.id.mainScrollInterior).setLayoutParams(lp);

        Log.v("LauncherStartup","2B. Set Adapter");
        appGridViewIcon.setAdapter(new AppsAdapter(this, editMode, names, iconApps));

        Log.v("LauncherStartup","2C. Set AdapterWide");
        appGridViewWide.setAdapter(new AppsAdapter(this, editMode, namesWide, wideApps));

        if (sharedPreferences.getBoolean(SettingsManager.NEEDS_META_DATA, true))
            sharedPreferenceEditor.putBoolean(SettingsManager.NEEDS_META_DATA, false);

        Log.v("LauncherStartup","2D. Reset Scroll");

        scrollView.scrollTo(0,0); // Reset scroll
        scrollView.smoothScrollTo(0,0); // Cancel inertia

        Log.v("LauncherStartup","2E. Set Groups Adapter");

        groupPanelGridView.setAdapter(new GroupsAdapter(this, editMode));

        Log.v("LauncherStartup","2F. Update Heights");

        prevViewWidth = -1;
        updateGridViewHeights();

        Log.v("LauncherStartup","2G. Post Next Step");

        post(this::updateTopBar);


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
            scrollInterior.setPadding(0, dp(23 + 22) + dp(40) * groupRows, 0, editMode?dp(60):0);
            scrollView.setFadingEdgeLength(dp(23 + 22) + dp(40) * groupRows);
        } else {
            int marginPx = dp(sharedPreferences.getInt(SettingsManager.KEY_MARGIN, SettingsManager.DEFAULT_MARGIN));
            scrollInterior.setPadding(0, 0, 0, marginPx+(editMode?dp(60):0));
            FadingTopScrollView scrollView = findViewById(R.id.mainScrollView);
            scrollView.setFadingEdgeLength(0);
        }

        int targetSizePx = dp(sharedPreferences.getInt(SettingsManager.KEY_SCALE, SettingsManager.DEFAULT_SCALE));
        int estimatedWidth = prevViewWidth;
        appGridViewIcon.setNumColumns((int) Math.round((double) estimatedWidth/targetSizePx));
        appGridViewWide.setNumColumns((int) Math.round((double) estimatedWidth/targetSizePx/2));
    }

    public void setBackground(int index) {
        if (index >= SettingsManager.BACKGROUND_DRAWABLES.length || index < 0) index = -1;
        else sharedPreferenceEditor.putBoolean(SettingsManager.KEY_DARK_MODE, SettingsManager.BACKGROUND_DARK[index]);
        sharedPreferenceEditor.putInt(SettingsManager.KEY_BACKGROUND, index);
        refreshBackground();
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
            setEditMode(!editMode);
            ArrayList<String> selectedGroups = settingsManager.getAppGroupsSorted(true);
            if (editMode && (selectedGroups.size() > 1)) {
                Set<String> selectFirst = new HashSet<>();
                selectFirst.add(selectedGroups.get(0));
                settingsManager.setSelectedGroups(selectFirst);
            }
        });


        // Wallpaper and style
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch dark = dialog.findViewById(R.id.switch_dark_mode);
        dark.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_DARK_MODE, SettingsManager.DEFAULT_DARK_MODE));
        dark.setOnCheckedChangeListener((compoundButton, value) -> {
            sharedPreferenceEditor.putBoolean(SettingsManager.KEY_DARK_MODE, value);
            refreshInterface();
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
        int background = sharedPreferences.getInt(SettingsManager.KEY_BACKGROUND, SettingsManager.DEFAULT_BACKGROUND);
        if (background < 0) background = views.length-1;

        for (ImageView image : views) {
            image.setClipToOutline(true);
        }
        final int wallpaperWidth = 32;
        final int selectedWallpaperWidthPx = dp(455+20-(wallpaperWidth+4)*(views.length-1)-wallpaperWidth);
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
                sharedPreferenceEditor.putInt(SettingsManager.KEY_SCALE, value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                refreshInterface();
            }
        });
        scale.setMax(200);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) scale.setMin(80);
        scale.setProgress(sharedPreferences.getInt(SettingsManager.KEY_SCALE, SettingsManager.DEFAULT_SCALE));


        SeekBar margin = dialog.findViewById(R.id.bar_margin);
        margin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                sharedPreferenceEditor.putInt(SettingsManager.KEY_MARGIN, value);
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

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch groups = dialog.findViewById(R.id.switch_group_mode);
        groups.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_GROUPS_ENABLED, SettingsManager.DEFAULT_GROUPS_ENABLED));
        groups.setOnCheckedChangeListener((sw, value) -> {
            if (!sharedPreferences.getBoolean(SettingsManager.KEY_SEEN_HIDDEN_GROUPS_POPUP, false) && value != SettingsManager.DEFAULT_GROUPS_ENABLED) {
                groups.setChecked(SettingsManager.DEFAULT_GROUPS_ENABLED); // Revert switch
                AlertDialog subDialog = DialogHelper.build(this, R.layout.dialog_hide_groups_info);
                subDialog.findViewById(R.id.confirm).setOnClickListener(view -> {
                    final boolean newValue = !SettingsManager.DEFAULT_GROUPS_ENABLED;
                    sharedPreferenceEditor.putBoolean(SettingsManager.KEY_SEEN_HIDDEN_GROUPS_POPUP, true)
                            .putBoolean(SettingsManager.KEY_GROUPS_ENABLED, newValue)
                            .apply();
                    groups.setChecked(!SettingsManager.DEFAULT_GROUPS_ENABLED);
                    refreshInterface();
                    subDialog.dismiss();
                });
                subDialog.findViewById(R.id.cancel).setOnClickListener(view -> {
                    subDialog.dismiss(); // Dismiss without setting
                });
            } else {
                sharedPreferenceEditor.putBoolean(SettingsManager.KEY_GROUPS_ENABLED, value);
                refreshInterface();
            }
        });
        dialog.findViewById(R.id.clear_icons ).setOnClickListener(view -> CompatHelper.clearIcons (this));
        dialog.findViewById(R.id.clear_labels).setOnClickListener(view -> CompatHelper.clearLabels(this));
        dialog.findViewById(R.id.clear_sort  ).setOnClickListener(view -> CompatHelper.clearSort  (this));

        // Wide display
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch wideVR = dialog.findViewById(R.id.switch_wide_vr);
        wideVR.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_WIDE_VR, SettingsManager.DEFAULT_WIDE_VR));
        wideVR.setOnCheckedChangeListener((compoundButton, value) -> {
            CompatHelper.clearIconCache(this);
            sharedPreferenceEditor.putBoolean(SettingsManager.KEY_WIDE_VR, value);
            updateAppLists();
            refreshInterface();
        });
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch wide2D = dialog.findViewById(R.id.switch_wide_2d);
        wide2D.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_WIDE_2D, SettingsManager.DEFAULT_WIDE_2D));
        wide2D.setOnCheckedChangeListener((compoundButton, value) -> {
            sharedPreferenceEditor.putBoolean(SettingsManager.KEY_WIDE_2D, value);
            updateAppLists();
            refreshInterface();
        });
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch wideWEB = dialog.findViewById(R.id.switch_wide_web);
        wideWEB.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_WIDE_WEB, SettingsManager.DEFAULT_WIDE_WEB));
        wideWEB.setOnCheckedChangeListener((compoundButton, value) -> {
            CompatHelper.clearIcons(this);
            sharedPreferenceEditor.putBoolean(SettingsManager.KEY_WIDE_WEB, value);
            updateAppLists();
            refreshInterface();
        });

        // Names
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch names = dialog.findViewById(R.id.switch_names);
        names.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_SHOW_NAMES_ICON, SettingsManager.DEFAULT_SHOW_NAMES_ICON));
        names.setOnCheckedChangeListener((compoundButton, value) -> {
            sharedPreferenceEditor.putBoolean(SettingsManager.KEY_SHOW_NAMES_ICON, value);
            refreshInterface();
        });
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch wideNames = dialog.findViewById(R.id.switch_names_wide);
        wideNames.setChecked(sharedPreferences.getBoolean(SettingsManager.KEY_SHOW_NAMES_WIDE, SettingsManager.DEFAULT_SHOW_NAMES_WIDE));
        wideNames.setOnCheckedChangeListener((compoundButton, value) -> {
            sharedPreferenceEditor.putBoolean(SettingsManager.KEY_SHOW_NAMES_WIDE, value);
            refreshInterface();
        });
    }

    public void openAppDetails(ApplicationInfo app) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + app.packageName));
        startActivity(intent);
    }
    public void uninstallApp(ApplicationInfo app) {
        if (AbstractPlatform.isWebsite(app)) {
            Set<String> webApps = sharedPreferences.getStringSet(SettingsManager.KEY_WEBSITE_LIST, Collections.emptySet());
            webApps = new HashSet<>(webApps); // Copy since we're not supposed to modify directly
            webApps.remove(app.packageName);
            sharedPreferenceEditor
                    .putString(app.packageName, null) // set display name
                    .putStringSet(SettingsManager.KEY_WEBSITE_LIST, webApps);
            updateAppLists();
            refreshInterface();
        } else {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse("package:" + app.packageName));
            startActivity(intent);
            onPause();
        }
    }

    // Edit Mode
    public Set<String> currentSelectedApps = new HashSet<>();
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
    public boolean isSelected(String app) {
        return currentSelectedApps.contains(app);
    }

    void updateSelectionHint() {
        TextView selectionHint = findViewById(R.id.selectionHint);

        final int size = currentSelectedApps.size();
        if (size == 0)      selectionHint.setText(R.string.selection_hint_none);
        else if (size == 1) selectionHint.setText(R.string.selection_hint_single);
        else selectionHint.setText(getString(R.string.selection_hint_multiple, size));
    }

    void addWebsite() {
        sharedPreferenceEditor.apply();

        AlertDialog dialog = DialogHelper.build(this, R.layout.dialog_new_website);

        dialog.findViewById(R.id.cancel).setOnClickListener(view -> dialog.cancel());
        ((TextView) dialog.findViewById(R.id.add_text)).setText(getString(R.string.add_website_group, SettingsManager.getDefaultGroup(false, true)));
        dialog.findViewById(R.id.confirm).setOnClickListener(view -> {
            String url  = ((EditText) dialog.findViewById(R.id.app_url )).getText().toString().toLowerCase();
            if (!Patterns.WEB_URL.matcher(url).matches() || !url.contains(".")) {
                dialog.findViewById(R.id.bad_url).setVisibility(View.VISIBLE);
                return;
            }
            if (!url.contains("//")) url = "https://" + url;

            Set<String> webApps = sharedPreferences.getStringSet(SettingsManager.KEY_WEBSITE_LIST, Collections.emptySet());
            webApps = new HashSet<>(webApps); // Copy since we're not supposed to modify directly
            webApps.add(url);

            sharedPreferenceEditor.putStringSet(SettingsManager.KEY_WEBSITE_LIST, webApps);
            dialog.cancel();
            updateAppLists();
            refreshInterface();
        });
    }
    public void updateAppLists() {
        sharedPreferenceEditor.apply();

        wideApps = new ArrayList<>();
        iconApps = new ArrayList<>();
        for (ApplicationInfo app: installedApps) {
            if (AbstractPlatform.isWideApp(app, this)) wideApps.add(app);
            else iconApps.add(app);
        }
        Set<String> webApps = sharedPreferences.getStringSet(SettingsManager.KEY_WEBSITE_LIST, Collections.emptySet());
        for (String url:webApps) {
            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = url;
            (sharedPreferences.getBoolean(SettingsManager.KEY_WIDE_WEB, SettingsManager.DEFAULT_WIDE_WEB) ? wideApps : iconApps)
                    .add(applicationInfo);
        }
    }
    public int dp(float dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }

    public void post(Runnable action) {
        if (mainView == null) action.run();
        else mainView.post(action);
    }

    // Get web states
    public WebViewService wService;
    boolean wBound = false;

    /** Defines callbacks for service binding, passed to bindService(). */
    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            WebViewService.LocalBinder binder = (WebViewService.LocalBinder) service;
            wService = binder.getService();
            wBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            wBound = false;
        }
    };

    private void bindWebViewService() {
        // Bind to web service
        Intent intent = new Intent(this, WebViewService.class);

        try {
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        } catch (Exception ignored) {
            WebViewActivity.killInstances(this);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }
    private void unbindWebViewService() {
        try {
            unbindService(connection);
        } catch (Exception ignored) {}
    }
}