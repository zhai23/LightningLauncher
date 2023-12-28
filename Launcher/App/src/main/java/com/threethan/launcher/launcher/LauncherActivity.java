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
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.threethan.launcher.R;
import com.threethan.launcher.adapter.AppsAdapter;
import com.threethan.launcher.adapter.GroupsAdapter;
import com.threethan.launcher.helper.AppData;
import com.threethan.launcher.helper.Compat;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Icon;
import com.threethan.launcher.helper.IconRepo;
import com.threethan.launcher.helper.Keyboard;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.support.AppDetailsDialog;
import com.threethan.launcher.support.SafeSharedPreferenceEditor;
import com.threethan.launcher.support.SettingsDialog;
import com.threethan.launcher.support.SettingsManager;
import com.threethan.launcher.support.Updater;
import com.threethan.launcher.view.MarginDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;
import eightbitlab.com.blurview.RenderScriptBlur;

/*
    LauncherActivity

    The class handles most of what the launcher does, though it is extended by it's child classes

    It relies on LauncherService to provide it with the main view/layout of the launcher, but all
    actual usage of that view (mainView) and its children is done here.

    It contains functions for initializing, refreshing, and updating various parts of the interface.
 */

public class LauncherActivity extends Activity {
    public boolean darkMode = true;
    public boolean groupsEnabled = true;
    RecyclerView appsView;
    public ApplicationInfo currentTopSearchResult = null;
    public Set<String> clearFocusPackageNames = new HashSet<>();
    RecyclerView groupsView;
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
    private RecyclerView.ItemDecoration marginDecoration;
    public static boolean namesBanner;
    public static boolean namesSquare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        sharedPreferences = Compat.getSharedPreferences(this);

        Intent intent = new Intent(this, LauncherService.class);
        bindService(intent, launcherServiceConnection, Context.BIND_AUTO_CREATE);

        int background = sharedPreferences.getInt(Settings.KEY_BACKGROUND,
        Platform.isTv(this)
            ? Settings.DEFAULT_BACKGROUND_TV
            : Settings.DEFAULT_BACKGROUND_VR);
        boolean custom = background < 0 || background >= SettingsManager.BACKGROUND_COLORS.length;
        int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[background];
        int alpha = sharedPreferences.getInt(Settings.KEY_BACKGROUND_ALPHA, Settings.DEFAULT_ALPHA);
        Drawable cd = new ColorDrawable(backgroundColor);
        if (alpha < 255) cd.setAlpha(alpha);
        post(() -> getWindow().setBackgroundDrawable(cd));
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to Launcher Service




        isKillable = false;
    }

    @Override
    protected void onStop() {
        isKillable = true;
        super.onStop();
    }
    public View rootView;

    private void onBound() {
        hasBound = true;
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
        ValueAnimator an = android.animation.ObjectAnimator.ofFloat(appsView, "alpha", 1f);
        an.setDuration(350);
        appsView.post(an::start);
    }
    protected void startWithExistingActivity() {
        Log.v(TAG, "Starting with existing view");
        rootView = launcherService.getExistingView(this);

        ViewGroup containerView = findViewById(R.id.container);
        containerView.addView(rootView);

        try {
            init();

            appsView.setAlpha(1f); // Just in case the app was closed before it faded in

            // Take ownership of adapters (which are currently referencing a dead activity)
            Objects.requireNonNull(getAppAdapter()).setLauncherActivity(this);
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

        refreshBackground();
    }

    protected void init() {
        sharedPreferenceEditor = new SafeSharedPreferenceEditor(sharedPreferences.edit());
        settingsManager = SettingsManager.getInstance(this);

        mainView = rootView.findViewById(R.id.mainLayout);
        mainView.addOnLayoutChangeListener(this::onLayoutChaged);
        appsView = rootView.findViewById(R.id.apps);
        groupsView = rootView.findViewById(R.id.groupsView);

        // Set logo button
        ImageView settingsImageView = rootView.findViewById(R.id.settingsIcon);
        settingsImageView.setOnClickListener(view -> {
            if (!settingsVisible) SettingsDialog.showSettings(this);
        });
        if (sharedPreferences.getBoolean(Settings.KEY_BACKGROUND_OVERLAY,
                Platform.isTv(this) ? Settings.DEFAULT_BACKGROUND_OVERLAY_TV
                                            : Settings.DEFAULT_BACKGROUND_OVERLAY_VR))
            startBackgroundOverlay();

        Icon.init(this);
    }
    protected void onLayoutChaged(View v, int left, int top, int right, int bottom,
                                  int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (Math.abs(oldBottom-bottom) > 10 || Math.abs(oldRight-right) > 10) { // Only on significant diff
            new WallpaperExecutor().execute(this);
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
        if (isFinishing()) try {
            launcherService.finished(this);
            unbindService(launcherServiceConnection); // Should rarely cause exception
            unbindService(browserServiceConnection); // Will ofter cause an exception if uncaught
            // For the GC & easier debugging
            settingsManager = null;
        } catch (RuntimeException ignored) {} //Runtime exception called when a service is invalid
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
            // Hide KB
            Keyboard.hide(this, mainView);

            // Bind service
            AppsAdapter.animateClose(this);
            com.threethan.launcher.browser.BrowserService.bind(this, browserServiceConnection, false);
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
        int myPlatformChangeIndex = 0;
        if (Platform.changeIndex > myPlatformChangeIndex) reloadPackages();
        else try {
            new RecheckPackagesExecutor().execute(this);
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
            if (getAppAdapter() == null) return;
            if (resultCode == RESULT_OK) {
                for (Image image : ImagePicker.getImages(data)) {
                    IconRepo.dontDownloadIconFor(this, selectedPackageName);
                    AppDetailsDialog.onImageSelected(image.getPath(), selectedImageView, this);
                    break;
                }
            } else AppDetailsDialog.onImageSelected(null, selectedImageView, this);
        } else if (requestCode == Settings.PICK_WALLPAPER_CODE) {
            if (resultCode == RESULT_OK) {
                for (Image image : ImagePicker.getImages(data)) {
                    try {
                        Bitmap bitmap = ImageLib.bitmapFromFile(this, new File(image.getPath()));
                        if (bitmap == null) return;
                        bitmap = ImageLib.getResizedBitmap(bitmap, 1280);
                        ImageLib.saveBitmap(bitmap, new File(getApplicationInfo().dataDir, Settings.CUSTOM_BACKGROUND_PATH));
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

        if (groupsEnabled || Platform.isTv(this)) {
            for (BlurView blurView : (groupsEnabled
                    ? blurViews
                    : new BlurView[]{rootView.findViewById(R.id.blurViewSearchBar)})
            ) {
                blurView.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                blurView.setOverlayColor((Color.parseColor(darkMode ? "#29000000" : "#40FFFFFF")));
                setupBlurView(blurView);

                blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                blurView.setClipToOutline(true);
            }

            if (groupsEnabled) {
                ImageView settingsIcon = rootView.findViewById(R.id.settingsIcon);
                settingsIcon.setImageTintList(ColorStateList.valueOf(darkMode ? Color.WHITE : Color.BLACK));
                ImageView searchIcon = rootView.findViewById(R.id.searchIcon);
                searchIcon.setImageTintList(ColorStateList.valueOf(darkMode ? Color.WHITE : Color.BLACK));
            }
        }

        post(this::postRefresh);
    }
    protected void setupBlurView(BlurView blurView) {
        View windowDecorView = getWindow().getDecorView();
        ViewGroup rootViewGroup = (ViewGroup) windowDecorView;
        Drawable windowBackground = windowDecorView.getBackground();

        //noinspection deprecation
        blurView.setupWith(rootViewGroup,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                ? new RenderEffectBlur()
                                : new RenderScriptBlur(this))
                .setFrameClearDrawable(windowBackground) // Optional
                .setBlurRadius(15f);
    }
    protected void postRefresh(){
        com.threethan.launcher.browser.BrowserService.bind(this, browserServiceConnection, false);
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
        } catch (Exception e) {
            Log.w(TAG, "Failed to post refreshInternal. Called too soon?");
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

        final int margin = sharedPreferences.getInt(Settings.KEY_MARGIN, Settings.DEFAULT_MARGIN) + 11;

        namesSquare = sharedPreferences.getBoolean(Settings.KEY_SHOW_NAMES_SQUARE, Settings.DEFAULT_SHOW_NAMES_SQUARE);
        namesBanner = sharedPreferences.getBoolean(Settings.KEY_SHOW_NAMES_BANNER, Settings.DEFAULT_SHOW_NAMES_BANNER);

        // Margins
        if (marginDecoration != null) appsView.removeItemDecoration(marginDecoration);
        int m = margin-22*2;
        // Height of a square icon. May be useful in the future...
        // int h = dp((groupGridView.getMeasuredWidth() - (m * (columns-1))*2))/(columns*2)-22*3;
        marginDecoration = new MarginDecoration(m);
        appsView.addItemDecoration(marginDecoration);

        groupsView.setAdapter(new GroupsAdapter(this, isEditing()));

        prevViewWidth = -1;
        updateGridViews();
        setAdapters();

        post(this::updateToolBars);
    }
    protected void resetScroll() {
        appsView.scrollToPosition(0);
    }
    protected void setAdapters() {

        if (getAppAdapter() == null) {
            appsView.setAdapter(
                    new AppsAdapter(this, null));
            refreshAppDisplayLists();
        }else {
            getAppAdapter().updateAppList(this);
            appsView.setAdapter(appsView.getAdapter());
        }
    }

    // Updates the heights and layouts of grid views
    // group grid view, square app grid view & banner app grid view
    public void updateGridViews() {

        if (mainView.getWidth() == prevViewWidth) return;
        prevViewWidth = mainView.getWidth();

        // Group rows and relevant values
        if (getAdapterGroups() != null && groupsEnabled) {
            if (prevViewWidth < 1) return;
            final int group_columns =
                    Math.min(getAdapterGroups().getCount(), prevViewWidth / dp(Settings.GROUP_WIDTH_DP));
            groupsView.setLayoutManager(new GridLayoutManager(this, Math.max(1,group_columns)));
            final int groupRows = (int) Math.ceil((double) getAdapterGroups().getCount() / group_columns);
            groupHeight = dp(40) * groupRows;

            if (groupHeight > mainView.getMeasuredHeight() / 3) {
                // Scroll groups if more than 1/3 the screen
                groupHeight = mainView.getMeasuredHeight() / 3;
                groupsView.setLayoutParams(new FrameLayout.LayoutParams
                        (ViewGroup.LayoutParams.MATCH_PARENT,groupHeight));
            } else {
                // Otherwise don't
                groupsView.setLayoutParams(new FrameLayout.LayoutParams
                        (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
        updatePadding();

        int targetSize = dp(sharedPreferences.getInt(Settings.KEY_SCALE, Settings.DEFAULT_SCALE));
        int estimatedWidth = prevViewWidth;

        final int nCol = estimatedWidth/(targetSize*2) * 2; // To nearest 2
        if (nCol <= 0) return;

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, nCol);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return Objects.requireNonNull(appsView.getAdapter()).getItemViewType(position);
            }
        });
        appsView.setLayoutManager(gridLayoutManager);

        groupsView.post(() -> groupsView.setVisibility(View.VISIBLE));
    }



    // Updates padding on the app grid views:
    // - Top padding to account for the groups bar
    // - Side padding to account for icon margins (otherwise icons would touch window edges)
    // - Bottom padding to account for icon margin, as well as the edit mode footer if applicable
    protected void updatePadding() {
        final int margin = 22+ sharedPreferences.getInt(Settings.KEY_MARGIN, Settings.DEFAULT_MARGIN);
        final boolean groupsVisible = getAdapterGroups() != null && groupsEnabled && !getSearching();
        final int topAdd = groupsVisible ? dp(23) + 22 + groupHeight : dp(23);
        final int bottomAdd = groupsVisible ? getBottomBarHeight() : margin/2+getBottomBarHeight();

        appsView.setClipToPadding(false);
        appsView.setPadding(
                margin,
                topAdd,
                margin,
                bottomAdd);

        appsView.setFadingEdgeLength(groupsVisible ? dp(23 + 22) + groupHeight : 0);
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

        new WallpaperExecutor().execute(this);
    }


    public void refreshAppDisplayLists() {
        sharedPreferenceEditor.apply();

        Platform.appList = Collections.synchronizedList(new ArrayList<>());

        Platform.appList.addAll(Platform.installedApps);
        // Add web apps
        Set<String> webApps = sharedPreferences.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        for (String url:webApps) {
            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = url;
            Platform.appList.add(applicationInfo);
        }
        // Add panel apps (Quest Only)
        if (Platform.isQuest(this))
            Platform.appList.addAll(AppData.getFullPanelAppList());

        if (getAppAdapter() != null)
            getAppAdapter().setFullAppList(Platform.appList);
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

    public @Nullable AppsAdapter getAppAdapter() {
        return (AppsAdapter) appsView.getAdapter();
    }
    public @Nullable GroupsAdapter getAdapterGroups() {
        if (groupsView == null) return null;
        return (GroupsAdapter) groupsView.getAdapter();
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
    public com.threethan.launcher.browser.BrowserService browserService;
    private boolean hasBound = false;

    /** Defines callbacks for service binding, passed to bindService(). */
    private final ServiceConnection launcherServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            LauncherService.LocalBinder binder = (LauncherService.LocalBinder) service;
            launcherService = binder.getService();
            if (!hasBound) onBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };
    private final ServiceConnection browserServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            com.threethan.launcher.browser.BrowserService.LocalBinder binder = (com.threethan.launcher.browser.BrowserService.LocalBinder) service;
            browserService = binder.getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };

    @SuppressLint("NotifyDataSetChanged")
    public void clearAdapterCaches() {
        if (getAppAdapter() != null) getAppAdapter().notifyDataSetChanged();
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