package com.threethan.launcher.launcher;

import android.annotation.SuppressLint;
import android.app.Activity;
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

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.threethan.launcher.R;
import com.threethan.launcher.adapter.AppsAdapter;
import com.threethan.launcher.adapter.CustomItemAnimator;
import com.threethan.launcher.adapter.GroupsAdapter;
import com.threethan.launcher.helper.AppData;
import com.threethan.launcher.helper.Compat;
import com.threethan.launcher.helper.DataStoreEditor;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.IconRepo;
import com.threethan.launcher.helper.Keyboard;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.support.AppDetailsDialog;
import com.threethan.launcher.support.SettingsDialog;
import com.threethan.launcher.support.SettingsManager;
import com.threethan.launcher.support.Updater;
import com.threethan.launcher.view.MarginDecoration;

import java.io.File;
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
    public static Boolean darkMode = null;
    public static Boolean groupsEnabled = true;
    RecyclerView appsView;
    public ApplicationInfo currentTopSearchResult = null;
    public Set<String> clearFocusPackageNames = new HashSet<>();
    RecyclerView groupsView;
    public DataStoreEditor dataStoreEditor;
    public View mainView;
    private int prevViewWidth;
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

        dataStoreEditor = Compat.getDataStore(this);

        Intent intent = new Intent(this, LauncherService.class);
        bindService(intent, launcherServiceConnection, Context.BIND_AUTO_CREATE);

        int background = dataStoreEditor.getInt(Settings.KEY_BACKGROUND,
        Platform.isTv(this)
            ? Settings.DEFAULT_BACKGROUND_TV
            : Settings.DEFAULT_BACKGROUND_VR);
        boolean custom = background < 0 || background >= SettingsManager.BACKGROUND_COLORS.length;
        int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[background];
        int alpha = dataStoreEditor.getInt(Settings.KEY_BACKGROUND_ALPHA, Settings.DEFAULT_ALPHA);
        Drawable cd = new ColorDrawable(backgroundColor);
        if (alpha < 255 && Platform.isQuest(this)) cd.setAlpha(alpha);
        post(() -> getWindow().setBackgroundDrawable(cd));
    }
    public View rootView;

    private void onBound() {
        if (hasBound) return;
        hasBound = true;
        final boolean hasView = launcherService.checkForExistingView();

        if (hasView) startWithExistingView();
        else         startWithNewView();

        AppsAdapter.shouldAnimateClose = false;
        AppsAdapter.animateClose(this);
    }
    protected void startWithNewView() {
        Log.v(TAG, "Starting with new view");
        ViewGroup containerView = findViewById(R.id.container);
        rootView = launcherService.getNewView(this, containerView);

        Log.v(TAG, "Init/compat staring");
        init();
        Compat.checkCompatibilityUpdate(this);
        Log.v(TAG, "Init/compat done");

        Log.v(TAG, "RP");
        reloadPackages();
        Log.v(TAG, "RB");
        refreshBackground();
        Log.v(TAG, "RI");
        refreshInterface();
        Log.v(TAG, "RX");
        post(() -> Log.v(TAG, "post"));
    }
    protected void startWithExistingView() {
        Log.v(TAG, "Starting with existing view");
        ViewGroup containerView = findViewById(R.id.container);
        rootView = launcherService.getExistingView(this, containerView);

        try {
            init();

            appsView.setAlpha(1f); // Just in case the app was closed before it faded in

            // Take ownership of adapters (which are currently referencing a dead activity)
            getAppAdapter().setLauncherActivity(this);
            getGroupAdapter().setLauncherActivity(this);

            groupsEnabled = dataStoreEditor.getBoolean(Settings.KEY_GROUPS_ENABLED, Settings.DEFAULT_GROUPS_ENABLED);
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
    }
    protected void onLayoutChaged(View v, int left, int top, int right, int bottom,
                                  int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (Math.abs(oldBottom-bottom) > 10 || Math.abs(oldRight-right) > 10) { // Only on significant diff
            new WallpaperExecutor().execute(this);
            updateGridLayouts();
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
        launcherService.destroyed(this);

        if (isFinishing()) try {
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
        super.onResume();
        try {
            // Hide KB
            Keyboard.hide(this, mainView);

            // Bind browser service
            AppsAdapter.animateClose(this);
            com.threethan.launcher.browser.BrowserService.bind(this, browserServiceConnection, false);
        } catch (Exception ignored) {} // Will fail if service hasn't started yet

        Dialog.setActivityContext(this);

        if (Platform.installedApps != null) // Will be null only on initial load
            postDelayed(this::recheckPackages, 1000);

        postDelayed(() -> new Updater(this).checkForAppUpdate(), 5000);

    }

    public void reloadPackages() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            Platform.clearPackageLists(this);
            PackageManager packageManager = getPackageManager();
            Platform.installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            Platform.installedApps = Collections.synchronizedList(Platform.installedApps);
            Compat.recheckSupported(this);
            refreshAppDisplayListsAll();
        });
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
        if (darkMode == null || groupsEnabled == null) {
            dataStoreEditor.getBoolean(Settings.KEY_DARK_MODE, Settings.DEFAULT_DARK_MODE, darkModeSet ->
                    dataStoreEditor.getBoolean(Settings.KEY_GROUPS_ENABLED, Settings.DEFAULT_GROUPS_ENABLED, groupsEnabledSet
                            -> {
                        if (darkMode == null) darkMode = darkModeSet;
                        if (groupsEnabled == null) groupsEnabled = groupsEnabledSet;

                        refreshAdapters();
                    }));
        } else refreshAdapters();

        // Fix some focus issues
        final View focused = getCurrentFocus();
        if (focused != null) focused.clearFocus();
        post(() -> {
            if (focused != null && getCurrentFocus() == null) focused.requestFocus();
        });
    }
    public void refreshAdapters() {
        updatePadding();

        prevViewWidth = -1;

        dataStoreEditor.getBoolean(Settings.KEY_SHOW_NAMES_SQUARE, Settings.DEFAULT_SHOW_NAMES_SQUARE, namesSquareSet
        -> dataStoreEditor.getBoolean(Settings.KEY_SHOW_NAMES_BANNER, Settings.DEFAULT_SHOW_NAMES_BANNER, namesBannerSet -> {
            namesSquare = namesSquareSet;
            namesBanner = namesBannerSet;
            if (getAppAdapter() == null) {
                appsView.setAdapter(
                        new AppsAdapter(this));
                appsView.setItemAnimator(new CustomItemAnimator());
            } else {
                getAppAdapter().setAppList(this);
            }
            groupsView.setAdapter(new GroupsAdapter(this, isEditing()));
        }));

        updateGridLayouts();
    }
    protected void resetScroll() {
        appsView.scrollToPosition(0);
    }

    // Updates the heights and layouts of grid layout managers
    public void updateGridLayouts() {
        if (mainView.getWidth() == prevViewWidth) return;
        prevViewWidth = mainView.getWidth();

        // Group rows and relevant values
        if (getGroupAdapter() != null && groupsEnabled) {
            if (prevViewWidth < 1) return;
            final int group_columns =
                    Math.min(getGroupAdapter().getCount(), prevViewWidth / dp(Settings.GROUP_WIDTH_DP));

            groupsView.setLayoutManager(new GridLayoutManager(this, Math.max(1,group_columns)));

            final int groupRows = (int) Math.ceil((double) getGroupAdapter().getCount() / group_columns);
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

    }



    // Updates padding on the app grid views:
    // - Top padding to account for the groups bar
    // - Side padding to account for icon margins (otherwise icons would touch window edges)
    // - Bottom padding to account for icon margin, as well as the edit mode footer if applicable
    protected void updatePadding() {
        dataStoreEditor.getInt(Settings.KEY_SCALE, Settings.DEFAULT_SCALE, scale
        -> dataStoreEditor.getInt(Settings.KEY_SPACING, Settings.DEFAULT_SPACING, spacing -> {
            int targetSize = dp(scale);
            int estimatedWidth = prevViewWidth;

            final int nCol = estimatedWidth / (targetSize * 2) * 2; // To nearest 2
            final float fCol = (float) (estimatedWidth) / (targetSize * 2) * 2;
            final float dCol = fCol - nCol;

            final float marginScale = (float) (scale
                    - Settings.MIN_SCALE) / (Settings.MAX_SCALE - Settings.MIN_SCALE) + 1f + dCol / 2;
            final int margin = dp(11f + (float) (spacing) * marginScale / 4f);

            final boolean groupsVisible = getGroupAdapter() != null && groupsEnabled && !getSearching();
            final int topAdd = groupsVisible ? dp(32) + groupHeight : dp(23);
            final int bottomAdd = groupsVisible ? getBottomBarHeight() + dp(11) : margin / 2 + getBottomBarHeight() + dp(11);

            appsView.setClipToPadding(false);
            appsView.setPadding(
                    margin,
                    topAdd,
                    margin,
                    bottomAdd);

            // Margins
            if (marginDecoration != null) appsView.removeItemDecoration(marginDecoration);
            // Height of a square icon. May be useful in the future...
            // int h = dp((groupGridView.getMeasuredWidth() - (margin * (columns-1))*2))/(columns*2)-22*3;
            marginDecoration = new MarginDecoration(margin - dp(22f));
            appsView.addItemDecoration(marginDecoration);

        }));
    }
    // Accounts for the height of the edit mode footer when visible, actual function in child class
    protected int getBottomBarHeight() {
        return 0;
    }
    // Sets the background value in the settings, then refreshes the background for all views
    public void setBackground(int index) {
        if (index >= SettingsManager.BACKGROUND_DRAWABLES.length || index < 0) index = -1;
        else dataStoreEditor.putBoolean(Settings.KEY_DARK_MODE, SettingsManager.BACKGROUND_DARK[index]);
        dataStoreEditor.putInt(Settings.KEY_BACKGROUND, index);
        LauncherActivity.background = index;
        launcherService.refreshBackgroundAll();
    }
    // Sets a background color based on your chosen background,
    // then calls an async task to actually load the background
    public void refreshBackground() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

            executorService.execute(() -> {
            // Set initial color, execute background task
            if (background == -2) {
                background = dataStoreEditor.getInt(Settings.KEY_BACKGROUND,
                        Platform.isTv(this)
                                ? Settings.DEFAULT_BACKGROUND_TV
                                : Settings.DEFAULT_BACKGROUND_VR);
            }

            boolean custom = background < 0 || background >= SettingsManager.BACKGROUND_COLORS.length;
            int backgroundColor = custom ? Color.parseColor("#404044") : SettingsManager.BACKGROUND_COLORS[background];

            runOnUiThread(() -> {
                getWindow().setNavigationBarColor(backgroundColor);
                getWindow().setStatusBarColor(backgroundColor);
                getWindow().setBackgroundDrawable(new ColorDrawable(backgroundColor));
            });

            new WallpaperExecutor().execute(this);

        });
    }
    static int background = -2;

    public void refreshAppDisplayLists() {
        runOnUiThread(this::refreshAdapters);

        if (Platform.installedApps == null) return;
        Platform.apps.clear();
        Platform.apps.addAll(Platform.installedApps);
        // Add web apps
        Set<String> webApps = dataStoreEditor.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
        for (String url:webApps) {
            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = url;
            Platform.apps.add(applicationInfo);
        }
        // Add panel apps (Quest Only)
        if (Platform.isQuest(this))
            Platform.apps.addAll(AppData.getFullPanelAppList());

        if (getAppAdapter() != null)
            getAppAdapter().setFullAppSet(Platform.apps);

        runOnUiThread(() -> getAppAdapter().setAppList(this));
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
                mainView.post(action);
            }
        }, ms);
        else mainView.postDelayed(action, ms);
    }

    public int dp(float dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }

    public AppsAdapter getAppAdapter() {
        return (AppsAdapter) appsView.getAdapter();
    }
    public GroupsAdapter getGroupAdapter() {
        if (groupsView == null) return null;
        return (GroupsAdapter) groupsView.getAdapter();
    }

    public HashSet<String> getAllPackages() {
        HashSet<String> setAll = new HashSet<>();
        if (Platform.installedApps == null) return new HashSet<>();
        for (ApplicationInfo app : Platform.installedApps) setAll.add(app.packageName);
        Set<String> webApps = dataStoreEditor.getStringSet(Settings.KEY_WEBSITE_LIST, new HashSet<>());
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
            com.threethan.launcher.browser.BrowserService.LocalBinder binder = (com.threethan.launcher.browser.BrowserService.LocalBinder) service;
            browserService = binder.getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };

    @SuppressLint("NotifyDataSetChanged")
    public void clearAdapterCaches() {
        if (getAppAdapter() != null) getAppAdapter().notifyDataSetChanged();
        if (getGroupAdapter() != null) getGroupAdapter().notifyDataSetChanged();
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
}