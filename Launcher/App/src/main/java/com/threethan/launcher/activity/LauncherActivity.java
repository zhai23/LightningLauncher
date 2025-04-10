package com.threethan.launcher.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.threethan.launcher.LauncherService;
import com.threethan.launcher.R;
import com.threethan.launcher.activity.adapter.GroupsAdapter;
import com.threethan.launcher.activity.adapter.LauncherAppsAdapter;
import com.threethan.launcher.activity.adapter.LauncherGridLayoutManager;
import com.threethan.launcher.activity.dialog.AppDetailsDialog;
import com.threethan.launcher.activity.dialog.SettingsDialog;
import com.threethan.launcher.activity.executor.WallpaperExecutor;
import com.threethan.launcher.activity.support.DataStoreEditor;
import com.threethan.launcher.activity.support.SettingsManager;
import com.threethan.launcher.activity.view.MarginDecoration;
import com.threethan.launcher.data.Settings;
import com.threethan.launcher.helper.AppExt;
import com.threethan.launcher.helper.Compat;
import com.threethan.launcher.helper.PlatformExt;
import com.threethan.launcher.updater.LauncherUpdater;
import com.threethan.launcher.updater.RemotePackageUpdater;
import com.threethan.launchercore.Core;
import com.threethan.launchercore.lib.ImageLib;
import com.threethan.launchercore.util.Keyboard;
import com.threethan.launchercore.util.Launch;
import com.threethan.launchercore.util.Platform;
import com.threethan.launchercore.util.TranslucentBlur;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eightbitlab.com.blurview.BlurView;

/**
    The class handles most of what the launcher does, though it is extended by it's child classes

    It relies on LauncherService to provide it with the main view/layout of the launcher, but all
    actual usage of that view (mainView) and its children is done here.

    It contains functions for initializing, refreshing, and updating various parts of the interface.
 */

public class LauncherActivity extends Launch.LaunchingActivity {
    public static Boolean darkMode = null;
    public static Boolean groupsEnabled = true;
    private static Boolean groupsWide = false;
    public static boolean needsForceRefresh = false;
    RecyclerView appsView;
    public ApplicationInfo currentTopSearchResult = null;
    public String currentTopSearchResultName = null;
    public Set<String> prevTopSearchResultNames = new HashSet<>();
    RecyclerView groupsView;
    public DataStoreEditor dataStoreEditor;
    public View mainView;
    public View topBar;
    private int prevViewWidth;
    public boolean needsUpdateCleanup = false;
    // Settings
    public SettingsManager settingsManager;
    public boolean settingsVisible;
    public LauncherService launcherService;
    protected static String TAG = "Lightning Launcher";
    private int groupHeight;
    private MarginDecoration marginDecoration;
    public static int iconMargin = -1;
    public static int iconScale = -1;

    public static boolean namesSquare;
    public static boolean namesBanner;
    public static boolean timesBanner;

    private static WeakReference<LauncherActivity> foregroundInstance = null;

    /**
     * Gets an instance of a LauncherActivity, preferring that which was most recently resumed
     * @return A reference to some launcher activity
     */
    public static @Nullable LauncherActivity getForegroundInstance() {
        if (foregroundInstance == null) return null;
        return foregroundInstance.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_container);

        Core.init(this);
        dataStoreEditor = Compat.getDataStore();

        Intent intent = new Intent(this, LauncherService.class);
        bindService(intent, launcherServiceConnection, Context.BIND_AUTO_CREATE);

        int background = dataStoreEditor.getInt(Settings.KEY_BACKGROUND,
        Platform.isTv()
            ? Settings.DEFAULT_BACKGROUND_TV
            : Settings.DEFAULT_BACKGROUND_VR);
        boolean custom = background < 0 || background >= SettingsManager.BACKGROUND_COLORS.length;
        int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[background];

        Drawable cd = new ColorDrawable(backgroundColor);
        if (Platform.isQuest()) cd.setAlpha(WallpaperExecutor.getBackgroundAlpha(dataStoreEditor));
        post(() -> getWindow().setBackgroundDrawable(cd));

        // Set back action
        final LauncherActivity la = this;
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (LauncherAppsAdapter.animateClose(la)) return;
                if (!settingsVisible) new SettingsDialog(la).show();
            }
        });
    }
    public View rootView;

    private void onBound() {

        if (hasBound) return;
        hasBound = true;
        final boolean hasView = launcherService.checkForExistingView();

        if (hasView) startWithExistingView();
        else startWithNewView();


    }
    protected void startWithNewView() {
        Log.v(TAG, "Startup 0: Starting with new view");
        ViewGroup containerView = findViewById(R.id.container);
        launcherService.getNewView(this, containerView, view -> {
            rootView = view;
            init();
            Compat.checkCompatibilityUpdate(this);

            refreshBackground();
            refreshAppList();
            refreshInterface();
        });
    }
    protected void startWithExistingView() {
        Log.v(TAG, "Starting with existing view");
        ViewGroup containerView = findViewById(R.id.container);
        rootView = launcherService.getExistingView(this, containerView);

        try {
            init();

            appsView.setAlpha(1f); // Just in case the app was closed before it faded in

            // Take ownership of adapters (which are currently referencing a dead activity)
            Objects.requireNonNull(getAppAdapter()).setLauncherActivity(this);
            Objects.requireNonNull(getGroupAdapter()).setLauncherActivity(this);

            post(this::updateToolBars); // Fix visual bugs with the blur views
        } catch (Exception e) {
            // Attempt to work around problems with backgrounded activities
            Log.e(TAG, "Crashed due to exception while re-initiating existing activity", e);
            Log.e(TAG, "Attempting to start with a new activity...");
        }

        refreshBackground();
    }

    protected void init() {
        Core.init(this);
        settingsManager = SettingsManager.getInstance(this);

        mainView = rootView.findViewById(R.id.mainLayout);
        topBar = mainView.findViewById(R.id.topBarLayout);

        mainView.addOnLayoutChangeListener(this::onLayoutChanged);
        appsView = rootView.findViewById(R.id.apps);
        appsView.setHasFixedSize(true);
        appsView.setItemViewCacheSize(512);
        groupsView = rootView.findViewById(R.id.groupsView);

        // Set logo button
        ImageView settingsImageView = rootView.findViewById(R.id.settingsIcon);
        settingsImageView.setOnClickListener(view -> {
            if (!settingsVisible) new SettingsDialog(this).show();
        });
    }

    protected void onLayoutChanged(View v, int left, int top, int right, int bottom,
                                   int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (Math.abs(oldBottom-bottom) > 10 || Math.abs(oldRight-right) > 10) { // Only on significant diff
            new WallpaperExecutor().execute(this);
            updateGridLayouts();
            post(this::updateToolBars);
            postDelayed(this::updateToolBars, 1000);
            while (appsView.getItemDecorationCount() > 1)
                appsView.removeItemDecorationAt(appsView.getItemDecorationCount()-1);
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "Activity is being destroyed - "
                + (isFinishing() ? "Finishing" : "Not Finishing"));
        if (launcherService != null && isFinishing()) launcherService.destroyed(this);

        if (isFinishing()) try {
            unbindService(launcherServiceConnection); // Should rarely cause exception
            // For the GC & easier debugging
            settingsManager = null;
        } catch (RuntimeException ignored) {} //Runtime exception called when a service is invalid
        super.onDestroy();
    }

    public enum FilePickerTarget {ICON, WALLPAPER, APK}
    private FilePickerTarget filePickerTarget;
    public void showFilePicker(FilePickerTarget target) {
        filePickerTarget = target;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (target.equals(FilePickerTarget.APK))
            intent.setType("application/vnd.android.package-archive");
        else
            intent.setType("image/*");

        filePicker.launch(intent);
    }

    private ImageView selectedImageView;
    public void setSelectedIconImage(ImageView imageView) {
        selectedImageView = imageView;
    }

    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    o -> {
                        if (o.getData() != null)
                            pickFile(o.getData().getData());
                    });

    private void pickFile(Uri uri) {
        if (filePickerTarget.equals(FilePickerTarget.APK)) {
            new RemotePackageUpdater(this).installApk(uri);
            return;
        }

        Bitmap bitmap;
        try {
            bitmap = ImageLib.bitmapFromStream(getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            Log.e("PhotoPicker", "Error on load", e);
            return;
        }
        if (bitmap == null) return;
        switch (filePickerTarget) {
            case ICON -> AppDetailsDialog.onImageSelected(
                    bitmap, selectedImageView, this);
            case WALLPAPER -> {
                bitmap = ImageLib.getResizedBitmap(bitmap, 720);
                ImageLib.saveBitmap(bitmap,
                        new File(getApplicationInfo().dataDir, Settings.CUSTOM_BACKGROUND_PATH));
                refreshBackground();
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        Core.init(this);

        foregroundInstance = new WeakReference<>(this);

        try {
            // Hide KB
            Keyboard.hide(this, mainView);

            // Bind browser service
            LauncherAppsAdapter.animateClose(this);
        } catch (Exception ignored) {} // Will fail if service hasn't started yet


        postDelayed(() -> new LauncherUpdater(this).checkAppUpdateInteractive(), 1000);
    }

    static ExecutorService refreshPackagesService = Executors.newSingleThreadExecutor();
    /**
     * Reloads and refreshes the current list of packages,
     * and then the resulting app list for every activity.
     */
    public void refreshPackages() {
        Log.v(TAG, "Refreshing Package List");
        if (PlatformExt.installedApps == null || needsForceRefresh) {
            forceRefreshPackages();
            return;
        }
        refreshPackagesService.execute(() -> {
            PackageManager packageManager = getPackageManager();

            List<ApplicationInfo> newApps = Collections.synchronizedList(
                    packageManager.getInstalledApplications(PackageManager.GET_META_DATA));

            if (newApps.size() != PlatformExt.installedApps.size())
                runOnUiThread(() -> this.refreshPackagesInternal(newApps));
            else {

                Set<Integer> installedSet = new HashSet<>();
                PlatformExt.installedApps.forEach(v -> installedSet.add(v.packageName.hashCode()));
                Set<Integer> newSet = new HashSet<>();
                newApps.forEach(v -> newSet.add(v.packageName.hashCode()));

                if (!newSet.equals(installedSet))
                    runOnUiThread(() -> this.refreshPackagesInternal(newApps));
            }
        });
    }
    public void forceRefreshPackages() {
        Log.v(TAG, "Package Refresh - Forced");

        needsForceRefresh = false;
        PackageManager packageManager = getPackageManager();
        refreshPackagesInternal(Collections.synchronizedList(
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)));
    }
    private void refreshPackagesInternal(List<ApplicationInfo> newApps) {
        PlatformExt.installedApps = newApps;

        Log.v(TAG, "Package reload - Found "+ PlatformExt.installedApps.size() +" packages");
        AppExt.invalidateCaches();

        launcherService.forEachActivity(LauncherActivity::refreshAppList);
    }


    /**
     * Updates various properties relating to the top bar & search bar, including visibility
     * & init-ing blurviews.
     * Note that these same views are also often manipulated in LauncherActivitySearchable
     */
    public void updateToolBars() {
        BlurView[] blurViews = new BlurView[]{
                rootView.findViewById(R.id.blurViewGroups),
                rootView.findViewById(R.id.blurViewSettingsIcon),
                rootView.findViewById(R.id.blurViewSearchIcon),
                rootView.findViewById(R.id.blurViewSearchBar),
        };

        final boolean hide = !groupsEnabled;
        for (int i = 0; i<blurViews.length-1; i++) blurViews[i].setVisibility(hide ? View.GONE : View.VISIBLE);
        if (isEditing() && hide) setEditMode(false); // If groups were disabled while in edit mode

        if (groupsEnabled) {
            for (BlurView blurView : blurViews
            ) {
                blurView.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                blurView.setOverlayColor((Color.parseColor(darkMode ? "#29000000" : "#40FFFFFF")));
                initBlurView(blurView);

                blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                blurView.setClipToOutline(true);
            }

            ImageView settingsIcon = rootView.findViewById(R.id.settingsIcon);
            settingsIcon.setImageTintList(ColorStateList.valueOf(darkMode ? Color.WHITE : Color.BLACK));
            ImageView searchIcon = rootView.findViewById(R.id.searchIcon);
            searchIcon.setImageTintList(ColorStateList.valueOf(darkMode ? Color.WHITE : Color.BLACK));
        }

        post(() -> { if (needsUpdateCleanup) Compat.doUpdateCleanup(this); });
    }

    /**
     * Initializes an eightbitlabs BlurView
     * @param blurView BlurView to setup
     */
    protected final void initBlurView(BlurView blurView) {
        View windowDecorView = getWindow().getDecorView();
        ViewGroup rootViewGroup = (ViewGroup) windowDecorView;

        blurView.setupWith(rootViewGroup, TranslucentBlur.getInstance(this))
                .setBlurRadius(15f);
    }

    public int lastSelectedGroup;

    /**
     * Refreshes most things to do with the interface, including calling refreshAdapters();
     * It is extended further by child classes
     */
    public void refreshInterface() {
        Log.v(TAG, "Refreshing interface (incl. Adapters)");

        groupsEnabled = dataStoreEditor.getBoolean(Settings.KEY_GROUPS_ENABLED, Settings.DEFAULT_GROUPS_ENABLED);
        groupsWide = dataStoreEditor.getBoolean(Settings.KEY_GROUPS_WIDE, Settings.DEFAULT_GROUPS_WIDE);

        refreshAdapters();

        // Fix some focus issues
        final View focused = getCurrentFocus();
        if (focused != null) focused.clearFocus();
        post(() -> {
            if (focused != null && getCurrentFocus() == null) focused.requestFocus();
        });
    }

    /**
     * Refreshes the display and layout of the RecyclerViews used for the groups list and app grid.
     * Includes a call to updateGridLayouts();
     */
    public void refreshAdapters() {
        Log.v("REFREHS", "ADAPTERS0");

        prevViewWidth = -1;

        darkMode = dataStoreEditor
                .getBoolean(Settings.KEY_DARK_MODE, Settings.DEFAULT_DARK_MODE);
        groupsEnabled = dataStoreEditor
                .getBoolean(Settings.KEY_GROUPS_ENABLED, Settings.DEFAULT_GROUPS_ENABLED);

        namesSquare = dataStoreEditor
                .getBoolean(Settings.KEY_SHOW_NAMES_SQUARE, Settings.DEFAULT_SHOW_NAMES_SQUARE);
        namesBanner = dataStoreEditor
                .getBoolean(Settings.KEY_SHOW_NAMES_BANNER, Settings.DEFAULT_SHOW_NAMES_BANNER);
        timesBanner = dataStoreEditor
                .getBoolean(Settings.KEY_SHOW_TIMES_BANNER, Settings.DEFAULT_SHOW_TIMES_BANNER);

        if (getAppAdapter() == null) {
            appsView.setAdapter(new LauncherAppsAdapter(this));
        } else {
            getAppAdapter().setAppList(this);
        }
        groupsView.setAdapter(new GroupsAdapter(this, isEditing()));

        updateGridLayouts();

        Log.v("REFREHS", "ADAPTERSD");

    }

    /**
     * Updates the heights and layouts of grid layout managers used by the groups bar and app grid
     */
    public void updateGridLayouts() {
        if (mainView.getWidth() == prevViewWidth) return;
        prevViewWidth = mainView.getWidth();

        // Group rows and relevant values
        if (getGroupAdapter() != null && groupsEnabled) {
            if (prevViewWidth < 1) return;
            final int targetWidth
                    = dp(groupsWide ? Settings.GROUP_WIDTH_DP_WIDE : Settings.GROUP_WIDTH_DP);
            final int groupCols
                    = Math.min(getGroupAdapter().getCount(), prevViewWidth / targetWidth);

            groupsView.setLayoutManager(new LauncherGridLayoutManager(this, Math.max(1, groupCols)));

            final int groupRows = (int) Math.ceil((double) getGroupAdapter().getCount() / groupCols);
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

        GridLayoutManager gridLayoutManager = (GridLayoutManager) appsView.getLayoutManager();
        if (gridLayoutManager == null) {
            gridLayoutManager = new GridLayoutManager(this, 3);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return Objects.requireNonNull(appsView.getAdapter()).getItemViewType(position);
                }
            });
            gridLayoutManager.setItemPrefetchEnabled(true);
            appsView.setLayoutManager(gridLayoutManager);
        }

        GridLayoutManager finalGridLayoutManager = gridLayoutManager;
        dataStoreEditor.getInt(Settings.KEY_SCALE, Settings.DEFAULT_SCALE, scale -> {
            int estimatedWidth = prevViewWidth;

            int nCol = estimatedWidth/(dp(scale)*2) * 2; // To nearest 2
            if (nCol <= 2) nCol = 2;

            finalGridLayoutManager.setSpanCount(nCol);
        });

        groupsView.post(() -> groupsView.setVisibility(View.VISIBLE));
        topBar.setVisibility(groupsEnabled ? View.VISIBLE : View.GONE);
    }
    /**
     * Called by updateGridLayouts, updates padding on the app grid views:
     * - Top padding to account for the groups bar
     * - Side padding to account for icon margins (otherwise icons would touch window edges)
     * - Bottom padding to account for icon margin, as well as the edit mode footer if applicable
     */
    private void updatePadding() {
        if (iconMargin == -1) iconMargin = dataStoreEditor.getInt(Settings.KEY_MARGIN, Settings.DEFAULT_MARGIN);
        if (iconScale  == -1) iconScale  = dataStoreEditor.getInt(Settings.KEY_SCALE , Settings.DEFAULT_SCALE );

        int targetSize = dp(iconScale);
        int margin = getMargin(targetSize);

        final boolean groupsVisible = getGroupAdapter() != null && groupsEnabled && !getSearching();
        final int topAdd = groupsVisible ? dp(32) + groupHeight : dp(23);
        final int bottomAdd = groupsVisible ? getBottomBarHeight() + dp(11) : margin / 2 + getBottomBarHeight() + dp(11);

        appsView.setClipToPadding(false);
        appsView.setPadding(
                dp(margin+25),
                topAdd,
                dp(margin+25),
                bottomAdd);

        // Margins

        if (marginDecoration == null) {
            marginDecoration = new MarginDecoration(margin);
            appsView.addItemDecoration(marginDecoration);
        } else marginDecoration.setMargin(margin);
        appsView.invalidateItemDecorations();
        while (appsView.getItemDecorationCount() > 1)
            appsView.removeItemDecorationAt(appsView.getItemDecorationCount()-1);
    }

    /** Get the margin, in dp, for the app grid */
    private int getMargin(int targetSize) {
        int estimatedWidth = prevViewWidth;

        final int nCol = estimatedWidth / (targetSize * 2) * 2; // To nearest 2
        final float fCol = (float) (estimatedWidth) / (targetSize * 2) * 2;
        final float dCol = fCol - nCol;

        final float normScale = (float) (iconScale - Settings.MIN_SCALE) / (Settings.MAX_SCALE - Settings.MIN_SCALE);
        int margin = (int) ((iconMargin) * (normScale+0.5f)/1.5f);
        margin += (int) ((dCol-0.5) * 75 / nCol);
        margin -= 20;
        if (margin < -20) margin = -20;
        return margin;
    }

    /**
     * Accounts for the height of the edit mode footer when visible.
     * Actual function in child class, as the base LauncherActivity is not editable.
     * @return Height of the bottom bar in px
     */
    protected int getBottomBarHeight() {
        return 0;
    }

    /**
     * Sets a background color to the window, navbar & statusbar  based on your chosen background,
     * then calls an additional Executor to actually load the background image
     */
    public void refreshBackground() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

            executorService.execute(() -> {
            // Set initial color, execute background task
            if (backgroundIndex == -2) {
                backgroundIndex = dataStoreEditor.getInt(Settings.KEY_BACKGROUND,
                        Platform.isTv()
                                ? Settings.DEFAULT_BACKGROUND_TV
                                : Settings.DEFAULT_BACKGROUND_VR);
            }

            boolean custom = backgroundIndex < 0 || backgroundIndex >= SettingsManager.BACKGROUND_COLORS.length;
            int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[backgroundIndex];
            Drawable cd = new ColorDrawable(backgroundColor);

            if (Platform.isQuest()) cd.setAlpha(WallpaperExecutor.getBackgroundAlpha(dataStoreEditor));

            if (Platform.isQuest()) cd.setAlpha(200);


            runOnUiThread(() -> {
                getWindow().setNavigationBarColor(backgroundColor);
                getWindow().setStatusBarColor(backgroundColor);
                getWindow().setBackgroundDrawable(cd);
            });

            new WallpaperExecutor().execute(this);
        });
    }
    public static int backgroundIndex = -2; // -2 indicated the setting needs to be loaded

    /**
     * Sets the background to the given index, automatically setting dark mode on/off if applicable
     * @param index of the new background
     */
    public void setBackground(int index) {
        if (index >= SettingsManager.BACKGROUND_DRAWABLES.length || index < 0) index = -1;
        else darkMode = SettingsManager.BACKGROUND_DARK[index];
        dataStoreEditor.putInt(Settings.KEY_BACKGROUND, index);
        LauncherActivity.backgroundIndex = index;
        launcherService.forEachActivity(LauncherActivity::refreshBackground);
    }

    /**
     * Used to update the actual content of app list used to the main app grid
     */
    public void refreshAppList() {
        if (PlatformExt.installedApps == null) {
            refreshPackages();
            return;
        }

        refreshAdapters();

        if (getAppAdapter() != null) {
            getAppAdapter().setFullAppSet(PlatformExt.listInstalledApps(this));
            getAppAdapter().setLauncherActivity(this);
        }
        final int scrollY = appsView.getScrollY();
        appsView.setScrollY(scrollY);
    }

    /**
     * Perform the action for clicking a group
     * @param position Index of the group
     */
    public void clickGroup(int position) {
        lastSelectedGroup = position;
        // This method is replaced with a greatly expanded one in the child class
        final List<String> groupsSorted = settingsManager.getAppGroupsSorted(false);
        final String group = groupsSorted.get(position);
        settingsManager.selectGroup(group);
        refreshInterface();
    }
    /**
     * Perform the action for long clicking a group
     * @param position Index of the group
     * @return True if this is the only selected group and should therefor show a menu
     */
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

    // Utility functions
    public void post(Runnable action) {
        if (mainView == null) action.run();
        else mainView.post(action);
    }
    public void postDelayed(Runnable action, int ms) {
        if (mainView == null) new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                action.run();
            }
        }, ms);
        else mainView.postDelayed(action, ms);
    }

    /**
     * Converts a value from display pixels to pixels
     * @param dip display pixel value
     * @return Pixel value
     */
    public int dp(float dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }

    @Nullable
    public LauncherAppsAdapter getAppAdapter() {
        if (appsView == null) return null;
        return (LauncherAppsAdapter) appsView.getAdapter();
    }
    @Nullable
    public GroupsAdapter getGroupAdapter() {
        if (groupsView == null) return null;
        return (GroupsAdapter) groupsView.getAdapter();
    }

    /**
     * Gets a set of the packageName of every package
     * @return Set of packageNames
     */
    public Set<String> getAllPackages() {
        Set<String> packages = new HashSet<>();
        PlatformExt.listInstalledApps(this).forEach(a -> packages.add(a.packageName));
        return packages;
    }

    // Services
    private boolean hasBound = false;

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

    /**
     * Calls a full update of group and app adapters (slow!)
     */
    @SuppressLint("NotifyDataSetChanged")
    public void resetAdapters() {
        if (getAppAdapter() != null) {
            refreshAppList();
            getAppAdapter().notifyAllChanged();
        }
        if (getGroupAdapter() != null) getGroupAdapter().notifyDataSetChanged();
        refreshInterface();
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
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F2) ActivityCapture.takeAndStoreCapture(this);
        return super.onKeyDown(keyCode, event);
    }
}