package com.threethan.launcher.launcher;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

import androidx.annotation.Nullable;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.threethan.launcher.R;
import com.threethan.launcher.adapter.AppsAdapter;
import com.threethan.launcher.adapter.GroupsAdapter;
import com.threethan.launcher.browser.BrowserService;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Compat;
import com.threethan.launcher.helper.Debug;
import com.threethan.launcher.helper.IconRepo;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.lib.SettingsDialog;
import com.threethan.launcher.support.SettingsManager;
import com.threethan.launcher.support.Updater;
import com.threethan.launcher.view.DynamicHeightGridView;
import com.threethan.launcher.view.FadingTopScrollView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
    GridView groupGridView;
    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor sharedPreferenceEditor;
    public View mainView;
    public View fadeView;
    private int prevViewWidth;
    public boolean isKillable = false;

    // Settings
    public SettingsManager settingsManager;
    public boolean settingsVisible;
    public LauncherService launcherService;
    protected static String TAG = "LightningLauncher";
    @Override
    protected void onStart() {
        super.onStart();
        isKillable = false;

        // Bind to Launcher Service
        Intent intent = new Intent(this, LauncherService.class);
        bindService(intent, launcherServiceConnection, Context.BIND_AUTO_CREATE);

        ViewGroup container = findViewById(R.id.container);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        int background = sharedPreferences.getInt(Settings.KEY_BACKGROUND, Settings.DEFAULT_BACKGROUND);
        boolean custom = background < 0 || background > SettingsManager.BACKGROUND_COLORS.length;
        int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[background];
        container.setBackgroundColor(backgroundColor);
    }

    @Override
    protected void onStop() {
        isKillable = true;
        super.onStop();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag") // Can't be fixed on this android API
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);
    }
    public View rootView;


    private void onBound() {
        isKillable = false;
        final boolean hasView = launcherService.checkForExistingView();

        if (hasView) startWithExistingActivity();
        else         startWithNewActivity();

        AppsAdapter.shouldAnimateClose = false;
        AppsAdapter.animateClose(this);

        postDelayed(() -> new Updater(this).checkForAppUpdate(), 1000);
    }
    protected void startWithNewActivity() {
        Log.v(TAG, "Starting with new view");
        rootView = launcherService.getNewView(this);

        ViewGroup containerView = findViewById(R.id.container);
        containerView.addView(rootView);

        init();
        Compat.checkCompatibilityUpdate(this);

        reloadPackages();
        // Load Interface
        refreshBackground();
        refreshInterface();

        // Animate in the apps
        ValueAnimator an = android.animation.ObjectAnimator.ofFloat(fadeView, "alpha", 1f);
        an.setDuration(150);
        fadeView.post(an::start);
    }
    protected void startWithExistingActivity() {
        Log.v(TAG, "Starting with existing view");
        rootView = launcherService.getExistingView(this);

        ViewGroup containerView = findViewById(R.id.container);
        containerView.addView(rootView);

        try {
            init();

            fadeView.setAlpha(1f); // Just in case the app was closed before it faded in

            // Take ownership of adapters (which are currently referencing a dead activity)
            Objects.requireNonNull(getAdapterSquare()).setLauncherActivity(this);
            Objects.requireNonNull(getAdapterBanner()).setLauncherActivity(this);
            Objects.requireNonNull(getAdapterGroups()).setLauncherActivity(this);
            recheckPackages(); // Just check, don't force it

            groupsEnabled = sharedPreferences.getBoolean(Settings.KEY_GROUPS_ENABLED, Settings.DEFAULT_GROUPS_ENABLED);
            post(this::updateToolBars); // Fix visual bugs with the blur views

        } catch (Exception e) {
            // Attempt to work around problems with backgrounded activities
            Log.e(TAG, "Crashed due to exception while re-initiating existing activity");
            e.printStackTrace();
            Log.e(TAG, "Attempting to start with a new activity...");
        }
    }

    protected void init() {
        sharedPreferenceEditor = sharedPreferences.edit();
        settingsManager = SettingsManager.getInstance(this);

        mainView = rootView.findViewById(R.id.mainLayout);
        mainView.addOnLayoutChangeListener((view, i, i1, i2, i3, i4, i5, i6, i7)
                -> post(this::updateGridViewHeights));
        appGridViewSquare = rootView.findViewById(R.id.appsViewSquare);
        appGridViewBanner = rootView.findViewById(R.id.appsViewBanner);
        scrollView = rootView.findViewById(R.id.mainScrollView);
        fadeView = scrollView;
        backgroundImageView = rootView.findViewById(R.id.background);
        groupGridView = rootView.findViewById(R.id.groupsView);

        // Handle group click listener
        groupGridView.setOnItemClickListener((parent, view, position, id) -> clickGroup(position));

        // Multiple group selection
        groupGridView.setOnItemLongClickListener((parent, view, position, id) -> longClickGroup(view, position));

        // Set logo button
        ImageView settingsImageView = rootView.findViewById(R.id.settingsIcon);
        settingsImageView.setOnClickListener(view -> {
            if (!settingsVisible) SettingsDialog.showSettings(this);
        });
    }

    protected void clickGroup(int position) {
        // This method is replaced with a greatly expanded one in the child class
        final List<String> groupsSorted = settingsManager.getAppGroupsSorted(false);
        final String group = groupsSorted.get(position);

        if (settingsManager.selectGroup(group)) refreshInterface();
        else recheckPackages(); // If clicking on the same single group, check if there are any new packages
    }
    protected boolean longClickGroup(View view, int position) {
        List<String> groups = settingsManager.getAppGroupsSorted(false);
        Set<String> selectedGroups = settingsManager.getSelectedGroups();

        if (position >= groups.size() || position < 0) return false;

        String item = groups.get(position);
        if (selectedGroups.contains(item)) selectedGroups.remove(item);
        else selectedGroups.add(item);
        if (selectedGroups.isEmpty()) {
            view.findViewById(R.id.menu).callOnClick();
            selectedGroups.add(item);
            return true;
        }
        settingsManager.setSelectedGroups(selectedGroups);
        refreshInterface();
        return true;
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "Activity is being destroyed - "
                + (isFinishing() ? "Finishing" : "Not Finishing"));
        launcherService.destroyed(this);
        try {
            unbindService(launcherServiceConnection); // Should rarely cause exception
            unbindService(browserServiceConnection); // Will ofter cause an exception if uncaught
        } catch (RuntimeException ignored) {} //Runtime exception called when a service is invalid
        // For the GC & easier debugging
        settingsManager = null;
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        if (AppsAdapter.animateClose(this)) return;
        if (!settingsVisible) SettingsDialog.showSettings(this);
    }
    @Override
    protected void onResume() {
        isKillable = false;
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
        Platform.installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        Platform.installedApps = Collections.synchronizedList(Platform.installedApps);
        refreshAppDisplayListsWithoutInterface();
    }

    public void recheckPackages() {
        try {
            new RecheckPackagesTask().execute(this);
        } catch (Exception ignore) {
            Log.w("LightningLauncher", "Exception while starting recheck package task");
        }
    }

    private String selectedPackageName;
    private ImageView selectedImageView;
    public void setSelectedIconImage(ImageView imageView, String packageName) {
        selectedImageView = imageView;
        selectedPackageName = packageName;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Settings.PICK_ICON_CODE) {
            if (getAdapterSquare() == null) return;
            if (resultCode == RESULT_OK) {
                for (Image image : ImagePicker.getImages(data)) {
                    IconRepo.dontDownloadIconFor(this, selectedPackageName);
                    getAdapterSquare().onImageSelected(image.getPath(), selectedImageView);
                    break;
                }
            } else getAdapterSquare().onImageSelected(null, selectedImageView);
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
    void updateToolBars() {
        BrowserService.bind(this, browserServiceConnection);

        BlurView[] blurViews = new BlurView[]{
                rootView.findViewById(R.id.blurViewGroups),
                rootView.findViewById(R.id.blurViewSettingsIcon),
                rootView.findViewById(R.id.blurViewSearchIcon),
        };

        if (!groupsEnabled) {
            for (BlurView blurView: blurViews) blurView.setVisibility(View.GONE);
            return;
        }

        float blurRadiusDp = 15f;

        View windowDecorView = getWindow().getDecorView();
        ViewGroup rootViewGroup = windowDecorView.findViewById(android.R.id.content);
        Drawable windowBackground = windowDecorView.getBackground();

        for (BlurView blurView: blurViews) {
            blurView.setVisibility(View.VISIBLE);
            blurView.setOverlayColor(Color.parseColor(darkMode ? "#4A000000" : "#50FFFFFF"));
            blurView.setupWith(rootViewGroup, new RenderScriptBlur(getApplicationContext())) // or RenderEffectBlur
                    .setFrameClearDrawable(windowBackground) // Optional
                    .setBlurRadius(blurRadiusDp);
            blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            blurView.setClipToOutline(true);
            blurView.setActivated(false);
            blurView.setActivated(true);
            blurView.setActivated(false);
        }

        ImageView settingsIcon = rootView.findViewById(R.id.settingsIcon);
        settingsIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#FFFFFF" : "#000000")));
        ImageView searchIcon   = rootView.findViewById(R.id.searchIcon);
        searchIcon  .setImageTintList(ColorStateList.valueOf(Color.parseColor(darkMode ? "#FFFFFF" : "#000000")));
    }


    public void refreshInterfaceAll() {
        isKillable = false;
        if (sharedPreferenceEditor != null) sharedPreferenceEditor.apply();
        try {
            launcherService.refreshInterfaceAll();
        } catch (Exception ignored) {
            Log.w(TAG, "Failed to call refresh on service!");
        }
    }
    public void refreshInterface() {
        if (sharedPreferenceEditor != null) sharedPreferenceEditor.apply();
        try {
            post(this::refreshInternal);
        } catch (Exception ignored) {
            Log.w(TAG, "Failed to post refreshInternal. Called by something else?");
        }
    }
    protected void refreshInternal() {
        sharedPreferenceEditor.apply();

        darkMode = sharedPreferences.getBoolean(Settings.KEY_DARK_MODE, Settings.DEFAULT_DARK_MODE);
        groupsEnabled = sharedPreferences.getBoolean(Settings.KEY_GROUPS_ENABLED, Settings.DEFAULT_GROUPS_ENABLED);

        if (!groupsEnabled && isEditing()) groupsEnabled = true;
        refreshAdapters();
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

        setAdapters(namesSquare, namesBanner);

        groupGridView.setAdapter(new GroupsAdapter(this, isEditing()));

        scrollView.scrollTo(0,0); // Reset scroll
        scrollView.smoothScrollTo(0,0); // Cancel inertia

        prevViewWidth = -1;
        updateGridViewHeights();

        post(this::updateToolBars);
    }
    protected void setAdapters(boolean namesSquare, boolean namesBanner) {
        if (getAdapterSquare() == null)
            appGridViewSquare.setAdapter(
                    new AppsAdapter(this, namesSquare, Platform.appListSquare));
        else {
            getAdapterSquare().updateAppList(this);
            appGridViewSquare.setAdapter(appGridViewSquare.getAdapter());
        }
        if (getAdapterBanner() == null)
            appGridViewBanner.setAdapter(
                    new AppsAdapter(this, namesBanner, Platform.appListBanner));
        else {
            getAdapterBanner().updateAppList(this);
            appGridViewBanner.setAdapter(appGridViewBanner.getAdapter());
        }
    }

    public void updateGridViewHeights() {
        if (mainView.getWidth() == prevViewWidth) return;
        prevViewWidth = mainView.getWidth();

        // Group rows and relevant values
        View scrollInterior = rootView.findViewById(R.id.mainScrollInterior);
        if (getAdapterGroups() != null && groupsEnabled) {
            final int group_columns = Math.min(getAdapterGroups().getCount(), prevViewWidth / 400);
            groupGridView.setNumColumns(group_columns);
            final int groupRows = (int) Math.ceil((double) getAdapterGroups().getCount() / group_columns);
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
        groupGridView.post(() -> groupGridView.setVisibility(View.VISIBLE));
    }
    protected int getBottomBarHeight() {
        return 0; // To be overridden by child
    }

    public void setBackground(int index) {
        if (index >= SettingsManager.BACKGROUND_DRAWABLES.length || index < 0) index = -1;
        else sharedPreferenceEditor.putBoolean(Settings.KEY_DARK_MODE, SettingsManager.BACKGROUND_DARK[index]);
        sharedPreferenceEditor.putInt(Settings.KEY_BACKGROUND, index);
        isKillable = false;
        launcherService.refreshBackgroundAll();
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
        refreshAppDisplayListsWithoutInterface();
        refreshInterfaceAll();
    }
    protected void refreshAppDisplayListsWithoutInterface() {
        sharedPreferenceEditor.apply();

        Platform.appListBanner = Collections.synchronizedList(new ArrayList<>());
        Platform.appListSquare = Collections.synchronizedList(new ArrayList<>());

        for (ApplicationInfo app: Platform.installedApps) {
            if (App.isBanner(app, this)) Platform.appListBanner.add(app);
            else Platform.appListSquare.add(app);
        }
        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        for (String url:webApps) {
            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = url;
            (sharedPreferences.getBoolean(Settings.KEY_WIDE_WEB, Settings.DEFAULT_WIDE_WEB) ?
                    Platform.appListBanner : Platform.appListSquare)
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

    public @Nullable AppsAdapter getAdapterSquare() {
        return (AppsAdapter) appGridViewSquare.getAdapter();
    }
    public @Nullable AppsAdapter getAdapterBanner() {
        return (AppsAdapter) appGridViewBanner.getAdapter();
    }
    public @Nullable GroupsAdapter getAdapterGroups() {
        return (GroupsAdapter) groupGridView.getAdapter();
    }

    public HashSet<String> getAllPackages() {
        HashSet<String> setAll = new HashSet<>();
        for (ApplicationInfo app : Platform.installedApps) setAll.add(app.packageName);
        return setAll;
    }

    // Services
    public BrowserService browserService;

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
            browserService = binder.getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };

    public void clearAdapterCaches() {
        if (getAdapterSquare() != null) getAdapterSquare().clearViewCache();
        if (getAdapterBanner() != null) getAdapterBanner().clearViewCache();
    }

    // Edit mode stubs, to be overridden by child
    public void setEditMode(boolean b) {
        Log.w(TAG, "Tried to set edit mode on an uneditable activity");
    }
    public boolean selectApp(String packageName) {
        Log.w(TAG, "Tried to select app on an uneditable activity");
        return false;
    }
    public boolean isSelected(String packageName) {
        return false;
    }
    public boolean isEditing() { return false; }
    public boolean canEdit() { return false; }
}