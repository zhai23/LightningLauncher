package com.threethan.launcher.launcher;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
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

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.threethan.launcher.R;
import com.threethan.launcher.adapter.AppsAdapter;
import com.threethan.launcher.adapter.GroupsAdapter;
import com.threethan.launcher.browser.BrowserService;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Compat;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.support.SettingsManager;
import com.threethan.launcher.support.Updater;
import com.threethan.launcher.view.DynamicHeightGridView;
import com.threethan.launcher.view.FadingTopScrollView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

/** @noinspection deprecation*/
public class LauncherActivity extends Activity {
    public boolean darkMode = true;
    public boolean groupsEnabled = true;
    DynamicHeightGridView appGridViewSquare;
    DynamicHeightGridView appGridViewBanner;
    ScrollView scrollView;
    ImageView backgroundImageView;
    GridView groupPanelGridView;
    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor sharedPreferenceEditor;
    private ImageView selectedImageView;
    public View mainView;
    public View fadeView;
    private int prevViewWidth;

    // Settings
    public SettingsManager settingsManager;
    public boolean settingsVisible;
    public LauncherService launcherService;

    public static String FINISH_ACTION = "com.threethan.launcher.FINISH";
    protected BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finishAndRemoveTask();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService.
        Intent intent = new Intent(this, LauncherService.class);
        bindService(intent, launcherServiceConnection, Context.BIND_AUTO_CREATE);

        ViewGroup container = findViewById(R.id.container);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        int background = sharedPreferences.getInt(Settings.KEY_BACKGROUND, Settings.DEFAULT_BACKGROUND);
        boolean custom = background < 0 || background > SettingsManager.BACKGROUND_COLORS.length;
        int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[background];
        container.setBackgroundColor(backgroundColor);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag") // Can't be fixed on this android API
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);
        registerReceiver(finishReceiver, new IntentFilter(FINISH_ACTION));
    }
    public View rootView;
    private void onBound() {
        final boolean firstStart = !launcherService.hasView(getId());

        ViewGroup containerView = findViewById(R.id.container);

        if (firstStart) {
            Log.v("LauncherStartup", "No existing activity found for ID "+getId());
            rootView = launcherService.getView(this);
            Compat.checkCompatibilityUpdate(this);
            containerView.addView(rootView);
            // Load Packages
            init();
            reloadPackages();
            // Load Interface
            refreshBackground();
            refresh();
        } else {
            Log.v("LauncherStartup", "Using existing view for ID "+getId());
            rootView = launcherService.getView(this);
            containerView.addView(rootView);
            init();// Set callbacks and variables
            // Take ownership of adapters (which are currently referencing a dead activity)
            ((AppsAdapter) appGridViewSquare.getAdapter()).setLauncherActivity(this);
            ((AppsAdapter) appGridViewBanner.getAdapter()).setLauncherActivity(this);
            ((GroupsAdapter) groupPanelGridView.getAdapter()).setLauncherActivity(this);
            recheckPackages(); // Just check, don't force it
            post(this::updateTopBar); // Fix visual bugs with the blur views
        }
        AppsAdapter.shouldAnimateClose = false;
        AppsAdapter.animateClose(this);

        postDelayed(() -> new Updater(this).checkForUpdate(), 1000);
    }

    protected void init() {
        sharedPreferenceEditor = sharedPreferences.edit();

        settingsManager = SettingsManager.getInstance(this);

        mainView = rootView.findViewById(R.id.mainLayout);
        mainView.addOnLayoutChangeListener((view, i, i1, i2, i3, i4, i5, i6, i7)
                -> post(this::updateGridViewHeights));
        appGridViewSquare = rootView.findViewById(R.id.appsViewIcon);
        appGridViewBanner = rootView.findViewById(R.id.appsViewWide);
        scrollView = rootView.findViewById(R.id.mainScrollView);
        fadeView = scrollView;
        backgroundImageView = rootView.findViewById(R.id.background);
        groupPanelGridView = rootView.findViewById(R.id.groupsView);

        // Handle group click listener
        groupPanelGridView.setOnItemClickListener((parent, view, position, id) -> changeGroup(position));

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
            refresh();
            return true;
        });

        // Set logo button
        ImageView settingsImageView = rootView.findViewById(R.id.settingsIcon);
        settingsImageView.setOnClickListener(view -> {
            if (!settingsVisible) SettingsDialog.showSettings(this);
        });
    }

    protected void changeGroup(int position) {
        // This method is replaced with a greatly expanded one in the child class
        final List<String> groupsSorted = settingsManager.getAppGroupsSorted(false);
        final String group = groupsSorted.get(position);

        if (settingsManager.selectGroup(group)) refresh();
        else recheckPackages(); // If clicking on the same single group, check if there are any new packages
    }

    @Override
    protected void onDestroy() {
        Log.v("LauncherActivity", "Activity is being destroyed" + (isFinishing() ? "Finishing" : "Not Finishing"));
        unbindService(launcherServiceConnection);
        try {unbindService(browserServiceConnection);} catch (Exception ignored) {}
        // For the GC & easier debugging
        settingsManager = null;
        unregisterReceiver(finishReceiver);
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        if (AppsAdapter.animateClose(this)) return;
        if (!settingsVisible) SettingsDialog.showSettings(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        try {
            AppsAdapter.animateClose(this);
            recheckPackages();
            BrowserService.bind(this, browserServiceConnection);
        } catch (Exception ignored) {} // Will fail if service hasn't bound yet
    }

    public void reloadPackages() {
        if (sharedPreferenceEditor == null) return;
        sharedPreferenceEditor.apply();
        sharedPreferenceEditor
                .remove(Settings.KEY_VR_SET)
                .remove(Settings.KEY_2D_SET);
        Platform.clearPackageLists();
        PackageManager packageManager = getPackageManager();
        installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        installedApps = Collections.synchronizedList(installedApps);
        refreshAppDisplayLists();
    }

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
        if (requestCode == Settings.PICK_ICON_CODE) {
            if (resultCode == RESULT_OK) {
                for (Image image : ImagePicker.getImages(data)) {
                    ((AppsAdapter) appGridViewSquare.getAdapter()).onImageSelected(image.getPath(), selectedImageView);
                    break;
                }
            } else {
                ((AppsAdapter) appGridViewSquare.getAdapter()).onImageSelected(null, selectedImageView);
            }
        } else if (requestCode == Settings.PICK_THEME_CODE) {
            if (resultCode == RESULT_OK) {
                for (Image image : ImagePicker.getImages(data)) {
                    try {
                        Bitmap bitmap = ImageLib.bitmapFromFile(this, new File(image.getPath()));
                        if (bitmap == null) return;
                        bitmap = ImageLib.getResizedBitmap(bitmap, 1280);
                        ImageLib.saveBitmap(bitmap, new File(getApplicationInfo().dataDir, Settings.CUSTOM_BACKGROUND_PATH));
                        setBackground(SettingsManager.BACKGROUND_DRAWABLES.length);
                        break;
                    } catch (Exception e) {e.printStackTrace();}
                }
            }
        }
    }

    void updateTopBar() {
        BlurView blurView0 = rootView.findViewById(R.id.blurView0);
        BlurView blurView1 = rootView.findViewById(R.id.blurView1);

        BrowserService.bind(this, browserServiceConnection);

        groupsEnabled = sharedPreferences.getBoolean(Settings.KEY_GROUPS_ENABLED, Settings.DEFAULT_GROUPS_ENABLED);
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

        ImageView settingsIcon = rootView.findViewById(R.id.settingsIcon);
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
    private void animateIn() {
        // Animate opacity
        ValueAnimator an = android.animation.ObjectAnimator.ofFloat(fadeView, "alpha", 1f);
        an.setDuration(100);
        fadeView.post(an::start);
    }
    List<ApplicationInfo> installedApps;
    List<ApplicationInfo> appListBanner;
    List<ApplicationInfo> appListSquare;

    public void refresh() {
        if (sharedPreferenceEditor != null) sharedPreferenceEditor.apply();
        try {
            post(this::refreshInternal);
        } catch (Exception ignored) {
            Log.w("LightningLauncher", "Failed to post refresh refreshInterfaceInternal. Called by something else?");
        }
    }
    protected void refreshInternal() {
        sharedPreferenceEditor.apply();

        fadeView.setAlpha(0); // Set opacity for animation

        darkMode = sharedPreferences.getBoolean(Settings.KEY_DARK_MODE, Settings.DEFAULT_DARK_MODE);
        groupsEnabled = sharedPreferences.getBoolean(Settings.KEY_GROUPS_ENABLED, Settings.DEFAULT_GROUPS_ENABLED);

        post(this::refreshAdapters);
    }
    public void refreshAdapters() {

        // Get and apply margin
        int marginPx = dp(sharedPreferences.getInt(Settings.KEY_MARGIN, Settings.DEFAULT_MARGIN));

        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(marginPx, Math.max(0,marginPx+(groupsEnabled ? dp(-23) : 0)), marginPx, marginPx+dp(20));
        rootView.findViewById(R.id.mainScrollInterior).setLayoutParams(lp);

        boolean namesSquare = sharedPreferences.getBoolean(Settings.KEY_SHOW_NAMES_SQUARE, Settings.DEFAULT_SHOW_NAMES_SQUARE);
        boolean namesBanner = sharedPreferences.getBoolean(Settings.KEY_SHOW_NAMES_BANNER, Settings.DEFAULT_SHOW_NAMES_BANNER);

        appGridViewSquare.setMargin(marginPx, namesSquare);
        appGridViewBanner.setMargin(marginPx, namesBanner);

        appGridViewSquare.setAdapter(new AppsAdapter(this, isEditing(), namesSquare, appListSquare));
        appGridViewBanner.setAdapter(new AppsAdapter(this, isEditing(), namesBanner, appListBanner));

        groupPanelGridView.setAdapter(new GroupsAdapter(this, isEditing()));

        scrollView.scrollTo(0,0); // Reset scroll
        scrollView.smoothScrollTo(0,0); // Cancel inertia

        prevViewWidth = -1;
        updateGridViewHeights();

        post(this::updateTopBar);
    }

    public void updateGridViewHeights() {
        if (mainView.getWidth() == prevViewWidth) return;
        prevViewWidth = mainView.getWidth();

        // Group rows and relevant values
        View scrollInterior = rootView.findViewById(R.id.mainScrollInterior);
        if (groupPanelGridView.getAdapter() != null && groupsEnabled) {
            final int group_columns = Math.min(groupPanelGridView.getAdapter().getCount(), prevViewWidth / 400);
            groupPanelGridView.setNumColumns(group_columns);
            final int groupRows = (int) Math.ceil((double) groupPanelGridView.getAdapter().getCount() / group_columns);
            scrollInterior.setPadding(0, dp(23 + 22) + dp(40) * groupRows, 0, getBottomBarHeight());
            scrollView.setFadingEdgeLength(dp(23 + 22) + dp(40) * groupRows);
        } else {
            int marginPx = dp(sharedPreferences.getInt(Settings.KEY_MARGIN, Settings.DEFAULT_MARGIN));
            scrollInterior.setPadding(0, 0, 0, marginPx+getBottomBarHeight());
            FadingTopScrollView scrollView = rootView.findViewById(R.id.mainScrollView);
            scrollView.setFadingEdgeLength(0);
        }

        int targetSizePx = dp(sharedPreferences.getInt(Settings.KEY_SCALE, Settings.DEFAULT_SCALE));
        int estimatedWidth = prevViewWidth;
        appGridViewSquare.setNumColumns((int) Math.round((double) estimatedWidth/targetSizePx));
        appGridViewBanner.setNumColumns((int) Math.round((double) estimatedWidth/targetSizePx/2));
    }
    protected int getBottomBarHeight() {
        return 0; // To be overridden by child
    }

    public void setBackground(int index) {
        if (index >= SettingsManager.BACKGROUND_DRAWABLES.length || index < 0) index = -1;
        else sharedPreferenceEditor.putBoolean(Settings.KEY_DARK_MODE, SettingsManager.BACKGROUND_DARK[index]);
        sharedPreferenceEditor.putInt(Settings.KEY_BACKGROUND, index);
        refreshBackground();
    }
    public void refreshBackground() {
        sharedPreferenceEditor.apply();

        // Set initial color, execute background task
        int background = sharedPreferences.getInt(Settings.KEY_BACKGROUND, Settings.DEFAULT_BACKGROUND);
        boolean custom = background < 0 || background > SettingsManager.BACKGROUND_COLORS.length;
        int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[background];

        backgroundImageView.setBackgroundColor(backgroundColor);
        getWindow().setNavigationBarColor(backgroundColor);
        getWindow().setStatusBarColor(backgroundColor);

        new BackgroundTask().execute(this);
    }

    public void refreshAppDisplayLists() {
        sharedPreferenceEditor.apply();

        appListBanner = Collections.synchronizedList(new ArrayList<>());
        appListSquare = Collections.synchronizedList(new ArrayList<>());

        for (ApplicationInfo app: installedApps) {
            if (App.isBanner(app, this)) appListBanner.add(app);
            else appListSquare.add(app);
        }
        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        for (String url:webApps) {
            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = url;
            (sharedPreferences.getBoolean(Settings.KEY_WIDE_WEB, Settings.DEFAULT_WIDE_WEB) ? appListBanner : appListSquare)
                    .add(applicationInfo);
        }
    }

    // Utility functions
    public void post(Runnable action) {
        if (mainView == null) action.run();
        else mainView.post(action);
    }
    public void postDelayed(Runnable action, int ms) {
        if (mainView == null) action.run();
        else mainView.postDelayed(action, ms);
    }
    public void postSharedPreferenceApply() {
        post(() -> sharedPreferenceEditor.apply());
    }

    public int dp(float dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }

    public HashSet<String> getAllPackages() {
        HashSet<String> setAll = new HashSet<>();
        for (ApplicationInfo app : installedApps) setAll.add(app.packageName);
        return setAll;
    }

    // Services
    public BrowserService wService;

    /** Defines callbacks for service binding, passed to bindService(). */
    private final ServiceConnection launcherServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            LauncherService.LocalBinder binder = (LauncherService.LocalBinder) service;
            launcherService = binder.getService();
            onBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };
    private final ServiceConnection browserServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            BrowserService.LocalBinder binder = (BrowserService.LocalBinder) service;
            wService = binder.getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };

    // Edit mode stubs, to be overridden by child
    public void setEditMode(boolean b) {
        Log.w("LauncherActivity", "Tried to set edit mode on an uneditable activity");
    }
    public boolean selectApp(String packageName) {
        Log.w("LauncherActivity", "Tried to select app on an uneditable activity");
        return false;
    }
    public boolean isSelected(String packageName) {
        return false;
    }
    public boolean isEditing() { return false; }
    public boolean canEdit() { return false; }

    public String getId() { return "default"; }
}