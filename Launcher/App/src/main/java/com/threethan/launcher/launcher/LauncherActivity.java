package com.threethan.launcher.launcher;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
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
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowMetrics;
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
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.IconRepo;
import com.threethan.launcher.helper.Keyboard;
import com.threethan.launcher.helper.AppData;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.support.SafeSharedPreferenceEditor;
import com.threethan.launcher.support.SettingsDialog;
import com.threethan.launcher.support.SettingsManager;
import com.threethan.launcher.support.Updater;
import com.threethan.launcher.view.DynamicHeightGridView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

/*
    LauncherActivity

    The class handles most of what the launcher does, though it is extended by it's child classes

    It relies on LauncherService to provide it with the main view/layout of the launcher, but all
    actual usage of that view (mainView) and its children is done here.

    It contains functions for initializing, refreshing, and updating various parts of the interface.
 */

/** @noinspection deprecation*/
public class LauncherActivity extends Activity {
    public boolean darkMode = true;
    public boolean groupsEnabled = true;
    DynamicHeightGridView appGridViewSquare;
    DynamicHeightGridView appGridViewBanner;
    ScrollView scrollView;
    public ApplicationInfo currentTopSearchResult = null;
    public Set<String> clearFocusPackageNames = new HashSet<>();
    GridView groupGridView;
    public SharedPreferences sharedPreferences;
    public SafeSharedPreferenceEditor sharedPreferenceEditor;
    public View mainView;
    private int prevViewWidth;
    public boolean isKillable = false;
    public boolean needsUpdateCleanup = false;
    // Settings
    public SettingsManager settingsManager;
    public boolean settingsVisible;
    public LauncherService launcherService;
    protected static String TAG = "LightningLauncher";
    private int groupHeight;
    @Override
    protected void onStart() {
        super.onStart();
        isKillable = false;

        // Bind to Launcher Service
        Intent intent = new Intent(this, LauncherService.class);
        bindService(intent, launcherServiceConnection, Context.BIND_AUTO_CREATE);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        int background = sharedPreferences.getInt(Settings.KEY_BACKGROUND,
                Platform.isTv(this)
                        ? Settings.DEFAULT_BACKGROUND_TV
                        : Settings.DEFAULT_BACKGROUND_VR);
        boolean custom = background < 0 || background >= SettingsManager.BACKGROUND_COLORS.length;
        int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[background];
        getWindow().setBackgroundDrawable(new ColorDrawable(backgroundColor));
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
        ValueAnimator an = android.animation.ObjectAnimator.ofFloat(scrollView, "alpha", 1f);
        an.setDuration(150);
        scrollView.post(an::start);
    }
    protected void startWithExistingActivity() {
        Log.v(TAG, "Starting with existing view");
        rootView = launcherService.getExistingView(this);

        ViewGroup containerView = findViewById(R.id.container);
        containerView.addView(rootView);

        try {
            init();

            scrollView.setAlpha(1f); // Just in case the app was closed before it faded in

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
        sharedPreferenceEditor = new SafeSharedPreferenceEditor(sharedPreferences.edit());
        settingsManager = SettingsManager.getInstance(this);

        mainView = rootView.findViewById(R.id.mainLayout);
        mainView.addOnLayoutChangeListener(this::onLayoutChaged);
        appGridViewSquare = rootView.findViewById(R.id.appsViewSquare);
        appGridViewBanner = rootView.findViewById(R.id.appsViewBanner);
        scrollView = rootView.findViewById(R.id.mainScrollView);
        groupGridView = rootView.findViewById(R.id.groupsView);

        // Set logo button
        ImageView settingsImageView = rootView.findViewById(R.id.settingsIcon);
        settingsImageView.setOnClickListener(view -> {
            if (!settingsVisible) SettingsDialog.showSettings(this);
        });
        if (sharedPreferences.getBoolean(Settings.KEY_BACKGROUND_OVERLAY,
                Platform.isTv(this) ? Settings.DEFAULT_BACKGROUND_OVERLAY_TV
                                            : Settings.DEFAULT_BACKGROUND_OVERLAY_VR))
            startBackgroundOverlay();
    }
    protected void onLayoutChaged(View v, int left, int top, int right, int bottom,
                                  int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (Math.abs(oldBottom-bottom) > 10 || Math.abs(oldRight-right) > 10) { // Only on significant diff
            new BackgroundTask().execute(this);
            updateGridViews();
        }
    }

    public boolean clickGroup(int position) {
        lastSelectedGroup = position;
        // This method is replaced with a greatly expanded one in the child class
        final List<String> groupsSorted = settingsManager.getAppGroupsSorted(false);
        final String group = groupsSorted.get(position);

        if (settingsManager.selectGroup(group)) {
            refreshInterface();
            return false;
        } else {
            recheckPackages();
            return isEditing() && !Objects.equals(group, Settings.HIDDEN_GROUP);
        }
    }
    public boolean longClickGroup(int position) {
        lastSelectedGroup = position;

        List<String> groups = settingsManager.getAppGroupsSorted(false);
        Set<String> selectedGroups = settingsManager.getSelectedGroups();

        if (position >= groups.size() || position < 0) return false;

        String item = groups.get(position);
        if (selectedGroups.contains(item)) selectedGroups.remove(item);
        else selectedGroups.add(item);
        if (selectedGroups.isEmpty()) {
            selectedGroups.add(item);
            return true;
        }
        settingsManager.setSelectedGroups(selectedGroups);
        refreshInterface();
        return false;
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "Activity is being destroyed - "
                + (isFinishing() ? "Finishing" : "Not Finishing"));
        try {
            launcherService.destroyed(this);
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

    private int myPlatformChangeIndex = 0;
    @Override
    protected void onResume() {
        isKillable = false;
        super.onResume();
        try {
            // Hide KB
            Keyboard.hide(this, mainView);

            // Bind service
            AppsAdapter.animateClose(this);
            BrowserService.bind(this, browserServiceConnection, false);
        } catch (Exception ignored) {} // Will fail if service hasn't bound yet

        Dialog.setActivityContext(this);

        post(this::recheckPackages);
        postDelayed(this::recheckPackages, 1000);

        postDelayed(() -> new Updater(this).checkForAppUpdate(), 1000);

    }

    public void reloadPackages() {
        if (sharedPreferenceEditor == null) return;

        sharedPreferenceEditor.apply();
        Platform.clearPackageLists(this);
        PackageManager packageManager = getPackageManager();
        Platform.installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        Platform.installedApps = Collections.synchronizedList(Platform.installedApps);
        Compat.recheckSupported(this);
        refreshAppDisplayListsAll();
    }

    public void recheckPackages() {
        if (Platform.changeIndex > myPlatformChangeIndex) reloadPackages();
        else try {
            new RecheckPackagesTask().execute(this);
        } catch (Exception ignore) {
            reloadPackages();
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
        BlurView[] blurViews = new BlurView[]{
                rootView.findViewById(R.id.blurViewGroups),
                rootView.findViewById(R.id.blurViewSettingsIcon),
                rootView.findViewById(R.id.blurViewSearchIcon),
                rootView.findViewById(R.id.blurViewSearchBar),
        };

        final boolean hide = !groupsEnabled;
        for (int i = 0; i<blurViews.length-1; i++) blurViews[i].setVisibility(hide ? View.GONE : View.VISIBLE);
        if (isEditing() && hide) setEditMode(false); // If groups were disabled while in edit mode

        if (!hide) {
            float blurRadiusDp = 15f;

            View windowDecorView = getWindow().getDecorView();
            ViewGroup rootViewGroup = (ViewGroup) windowDecorView;
            Drawable windowBackground = windowDecorView.getBackground();

            for (BlurView blurView : blurViews) {
                blurView.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                blurView.setOverlayColor((Color.parseColor(darkMode ? "#29000000" : "#40FFFFFF")));
                blurView.setupWith(rootViewGroup, new RenderScriptBlur(getApplicationContext())) // or RenderEffectBlur
                        .setFrameClearDrawable(windowBackground) // Optional
                        .setBlurRadius(blurRadiusDp);
                blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                blurView.setClipToOutline(true);
            }

            ImageView settingsIcon = rootView.findViewById(R.id.settingsIcon);
            settingsIcon.setImageTintList(ColorStateList.valueOf(darkMode ? Color.WHITE : Color.BLACK));
            ImageView searchIcon = rootView.findViewById(R.id.searchIcon);
            searchIcon.setImageTintList(ColorStateList.valueOf(darkMode ? Color.WHITE : Color.BLACK));
        }

        post(this::postRefresh);
    }
    protected void postRefresh(){
        BrowserService.bind(this, browserServiceConnection, false);
        if (needsUpdateCleanup) Compat.doUpdateCleanup(this);
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
    public void refreshAppDisplayListsAll() {
        try {
            launcherService.refreshAppDisplayListsAll();
        } catch (Exception ignored) {
            Log.w(TAG, "Failed to call refresh on service!");
        }
    }
    public int lastSelectedGroup;

    public void refreshInterface() {
        // Fix focus
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

        refreshAdapters();

        // Fix some focus issues
        final View focused = getCurrentFocus();
        if (focused != null) focused.clearFocus();
        post(() -> {
            if (focused != null && getCurrentFocus() == null) focused.requestFocus();
        });
    }
    public void refreshAdapters() {
        updatePadding();

        final int marginPx = dp(sharedPreferences.getInt(Settings.KEY_MARGIN, Settings.DEFAULT_MARGIN));

        boolean namesSquare = sharedPreferences.getBoolean(Settings.KEY_SHOW_NAMES_SQUARE, Settings.DEFAULT_SHOW_NAMES_SQUARE);
        boolean namesBanner = sharedPreferences.getBoolean(Settings.KEY_SHOW_NAMES_BANNER, Settings.DEFAULT_SHOW_NAMES_BANNER);

        appGridViewSquare.setMargin(marginPx, namesSquare, dp(22));
        appGridViewBanner.setMargin(marginPx, namesBanner, dp(22));

        setAdapters(namesSquare, namesBanner);

        groupGridView.setAdapter(new GroupsAdapter(this, isEditing()));

        prevViewWidth = -1;
        updateGridViews();

        post(this::updateToolBars);
    }
    protected void resetScroll() {
        scrollView.scrollTo(0,0); // Reset scroll
        scrollView.smoothScrollTo(0,0); // Cancel inertia
    }
    protected void setAdapters(boolean namesSquare, boolean namesBanner) {
        if (getAdapterSquare() == null)
            appGridViewSquare.setAdapter(
                    new AppsAdapter(this, namesSquare, false, Platform.appListSquare));
        else {
            getAdapterSquare().updateAppList(this);
            appGridViewSquare.setAdapter(appGridViewSquare.getAdapter());
        }
        if (getAdapterBanner() == null)
            appGridViewBanner.setAdapter(
                    new AppsAdapter(this, namesBanner, true, Platform.appListBanner));
        else {
            getAdapterBanner().updateAppList(this);
            appGridViewBanner.setAdapter(appGridViewBanner.getAdapter());
        }
    }

    // Updates the heights and layouts of grid views
    // group grid view, square app grid view & banner app grid view
    public void updateGridViews() {
        if (mainView.getWidth() == prevViewWidth) return;
        prevViewWidth = mainView.getWidth();

        // Group rows and relevant values
        if (getAdapterGroups() != null && groupsEnabled) {
            final int group_columns =
                    Math.min(getAdapterGroups().getCount(), prevViewWidth / dp(Settings.GROUP_WIDTH_DP));
            groupGridView.setNumColumns(group_columns);
            final int groupRows = (int) Math.ceil((double) getAdapterGroups().getCount() / group_columns);
            groupHeight = dp(40) * groupRows;

            if (groupHeight > mainView.getMeasuredHeight() / 3) {
                // Scroll groups if more than 1/3 the screen
                groupHeight = mainView.getMeasuredHeight() / 3;
                groupGridView.setLayoutParams(new FrameLayout.LayoutParams
                        (ViewGroup.LayoutParams.MATCH_PARENT,groupHeight));
            } else {
                // Otherwise don't
                groupGridView.setLayoutParams(new FrameLayout.LayoutParams
                        (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
        updatePadding();

        int targetSizePx = dp(sharedPreferences.getInt(Settings.KEY_SCALE, Settings.DEFAULT_SCALE));
        int estimatedWidth = prevViewWidth;
        appGridViewSquare.setNumColumns((int) Math.round((double) estimatedWidth/targetSizePx));
        appGridViewBanner.setNumColumns((int) Math.round((double) estimatedWidth/targetSizePx/2));
        groupGridView.post(() -> groupGridView.setVisibility(View.VISIBLE));
    }



    // Updates padding on the app grid views:
    // - Top padding to account for the groups bar
    // - Side padding to account for icon margins (otherwise icons would touch window edges)
    // - Bottom padding to account for icon margin, as well as the edit mode footer if applicable
    protected void updatePadding() {
        final int marginPx = dp(sharedPreferences.getInt(Settings.KEY_MARGIN, Settings.DEFAULT_MARGIN));
        final boolean groupsVisible = getAdapterGroups() != null && groupsEnabled && !getSearching();
        final int topAdd = groupsVisible ? dp(23 + 22) + groupHeight : 0;
        final int bottomAdd = groupsVisible ? getBottomBarHeight() : marginPx/2+getBottomBarHeight();

        appGridViewBanner.setPadding(
                marginPx,
                Math.max(0,marginPx+(groupsVisible ? 0 : dp(22)))+topAdd,
                marginPx,
                0);
        appGridViewSquare.setPadding(
                marginPx,
                dp(5), // Margin top is -5dp
                marginPx,
                bottomAdd);

        scrollView.setFadingEdgeLength(groupsVisible ? dp(23 + 22) + groupHeight : 0);
    }
    // Accounts for the height of the edit mode footer when visible, actual function in child class
    protected int getBottomBarHeight() {
        return 0;
    }
    // Sets the background value in the settings, then refreshes the background for all views
    public void setBackground(int index) {
        if (index >= SettingsManager.BACKGROUND_DRAWABLES.length || index < 0) index = -1;
        else sharedPreferenceEditor.putBoolean(Settings.KEY_DARK_MODE, SettingsManager.BACKGROUND_DARK[index]);
        sharedPreferenceEditor.putInt(Settings.KEY_BACKGROUND, index);
        isKillable = false;
        launcherService.refreshBackgroundAll();
    }
    // Sets a background color based on your chosen background,
    // then calls an async task to actually load the background
    public void refreshBackground() {
        sharedPreferenceEditor.apply();

        // Set initial color, execute background task
        int background = sharedPreferences.getInt(Settings.KEY_BACKGROUND,
                Platform.isTv(this)
                        ? Settings.DEFAULT_BACKGROUND_TV
                        : Settings.DEFAULT_BACKGROUND_VR);
        boolean custom = background < 0 || background >= SettingsManager.BACKGROUND_COLORS.length;
        int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[background];

        getWindow().setNavigationBarColor(backgroundColor);
        getWindow().setStatusBarColor(backgroundColor);

        new BackgroundTask().execute(this);
    }


    public void refreshAppDisplayLists() {
        refreshAppDisplayListsWithoutInterface();
        refreshInterface();
    }
    protected void refreshAppDisplayListsWithoutInterface() {
        sharedPreferenceEditor.apply();

        Platform.appListSquare = Collections.synchronizedList(new ArrayList<>());
        Platform.appListBanner = Collections.synchronizedList(new ArrayList<>());

        for (ApplicationInfo app: Platform.installedApps) {
            if (App.isBanner(this, app)) Platform.appListBanner.add(app);
            else Platform.appListSquare.add(app);
        }
        // Add web apps
        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        for (String url:webApps) {
            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = url;
            (App.typeIsBanner(App.Type.TYPE_WEB) ?
                    Platform.appListBanner : Platform.appListSquare)
                    .add(applicationInfo);
        }
        // Add panel apps (Quest Only)
        if (Platform.isQuest(this)) {
            for (ApplicationInfo panelApp : AppData.getFullPanelAppList()) {
                (App.typeIsBanner(App.Type.TYPE_PANEL) ?
                        Platform.appListBanner : Platform.appListSquare)
                        .add(panelApp);
            }
        }

        if (getAdapterSquare() != null)
            getAdapterSquare().setFullAppList(Platform.appListSquare);
        if (getAdapterBanner() != null)
            getAdapterBanner().setFullAppList(Platform.appListBanner);
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
        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, new HashSet<>());
        setAll.addAll(webApps);
        if (Platform.isQuest(this)) {
            for (ApplicationInfo panelApp : AppData.getFullPanelAppList())
                setAll.add(panelApp.packageName);
        }
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
    public void addWebsite(Context context) {}
    protected boolean getSearching() { return false; }
    // Wallpaper Animated Gradient Overlay
    protected void startBackgroundOverlay() {
        findViewById(R.id.overlayGradient).setVisibility(View.VISIBLE);
        final View gradView = findViewById(R.id.overlayGradient);
        AnimationDrawable anim = ((AnimationDrawable) gradView.getBackground());
        anim.setExitFadeDuration(15000);
        anim.setEnterFadeDuration(10);
        anim.start();
    }
    public void setBackgroundOverlay(boolean val) {
        sharedPreferenceEditor.putBoolean(Settings.KEY_BACKGROUND_OVERLAY, val).apply();
        if (val) startBackgroundOverlay();
        else findViewById(R.id.overlayGradient).setVisibility(View.GONE);
    }
}