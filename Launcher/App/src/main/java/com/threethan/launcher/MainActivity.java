package com.threethan.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.threethan.launcher.platforms.AbstractPlatform;
import com.threethan.launcher.ui.AppsAdapter;
import com.threethan.launcher.ui.GroupsAdapter;
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;


class BackgroundTask extends AsyncTask {

    Drawable backgroundThemeDrawable;
    MainActivity owner;
    @Override
    protected Object doInBackground(Object[] objects) {
        Log.i("LauncherStartup", "BackgroundFetch");

        owner = (MainActivity) objects[0];
        int backgroundThemeIndex = owner.sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_THEME, owner.DEFAULT_THEME);
        if (backgroundThemeIndex < owner.THEME_DRAWABLES.length) {
            backgroundThemeDrawable = owner.getDrawable(owner.THEME_DRAWABLES[backgroundThemeIndex]);
        } else {
            File file = new File(owner.getApplicationInfo().dataDir, owner.CUSTOM_THEME);
            Bitmap themeBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            backgroundThemeDrawable = new BitmapDrawable(owner.getResources(), themeBitmap);
        }
        Log.i("LauncherStartup", "BackgroundReady");
        return null;
    }

    @Override
    protected void onPostExecute(Object _n) {
        owner.backgroundImageView.setImageDrawable(backgroundThemeDrawable);
        Log.i("LauncherStartup", "BackgroundApplied");
        owner.initBlur();
    }
}

class RecheckPackagesTask extends AsyncTask {

    List<ApplicationInfo> foundApps;
    MainActivity owner;
    boolean changeFound;
    @Override
    protected Object doInBackground(Object[] objects) {
        Log.i("PackageCheck", "Checking for package changes");

        owner = (MainActivity) objects[0];

        PackageManager packageManager = owner.getPackageManager();
        foundApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        changeFound = owner.allApps.size() != foundApps.size();
        return null;
    }
    @Override
    protected void onPostExecute(Object _n) {
        if (changeFound) {
            Log.i("PackageCheck", "Package change detected!");

            owner.allApps = foundApps;
            owner.reloadUI();
        }
    }
}
public class MainActivity extends Activity {
    public static final int PICK_ICON_CODE = 450;
    public static final int PICK_THEME_CODE = 95;
    public static final String CUSTOM_THEME = "theme.png";
    static final boolean DEFAULT_NAMES = true;
    private static final int DEFAULT_OPACITY = 255;
    static final int DEFAULT_SCALE = 105;
    static final int DEFAULT_THEME = 0;
    static final int[] THEME_DRAWABLES = {
            R.drawable.bg_px_purple,
            R.drawable.bg_px_red,
            R.drawable.bg_px_green,
            R.drawable.bg_px_orange,
            R.drawable.bg_px_white,
    };
    static final int[] THEME_COLORS = {
            Color.parseColor("#74575c"),
            Color.parseColor("#d26f5d"),
            Color.parseColor("#e4eac8"),
            Color.parseColor("#f9ce9b"),
            Color.parseColor("#d9d4da"),
    };
    private ImageView[] selectedThemeImageViews;
    GridView appGridView;
    ImageView backgroundImageView;
    GridView groupPanelGridView;
    SharedPreferences sharedPreferences;
    private SettingsProvider settingsProvider;
    private ImageView selectedImageView;
    private boolean settingsPageOpen = false;
    private boolean lookPageOpen = false;
    private boolean platformsPageOpen = false;
    private boolean loaded = false;
    public ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("LauncherStartup", "1. Set View");

        setContentView(R.layout.activity_main);

        Log.i("LauncherStartup", "2. Get Setting Provider");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        settingsProvider = SettingsProvider.getInstance(this);

        Log.i("LauncherStartup", "3. Get UI Instances");

        // Get UI instances
        RelativeLayout mainView = findViewById(R.id.linearLayoutMain);
        appGridView = findViewById(R.id.appsView);
        backgroundImageView = findViewById(R.id.background);
        groupPanelGridView = findViewById(R.id.groupsView);

        // Handle group click listener
        groupPanelGridView.setOnItemClickListener((parent, view, position, id) -> {
            List<String> groups = settingsProvider.getAppGroupsSorted(false);
            if (hasSelection) {
                GroupsAdapter groupsAdapter = (GroupsAdapter) groupPanelGridView.getAdapter();
                for (String app : currentSelectedApps) groupsAdapter.setGroup(app, groups.get(position));
                currentSelectedApps = new HashSet<>();
                hasSelection = false;
                reloadUI();
            } else {
                if (position == groups.size()) {
                    settingsProvider.selectGroup(settingsProvider.addGroup());
                } else {
                    settingsProvider.selectGroup(groups.get(position));
                }
                reloadUI();
            }
        });

        // Multiple group selection
        groupPanelGridView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!sharedPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false)) {
                List<String> groups = settingsProvider.getAppGroupsSorted(false);
                Set<String> selectedGroups = settingsProvider.getSelectedGroups();

                String item = groups.get(position);
                if (selectedGroups.contains(item)) {
                    selectedGroups.remove(item);
                } else {
                    selectedGroups.add(item);
                }
                if (selectedGroups.isEmpty()) {
                    selectedGroups.add(groups.get(0));
                }
                settingsProvider.setSelectedGroups(selectedGroups);
                reloadUI();
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

        Log.i("LauncherStartup", "4. Done");
    }

    @Override
    public void onBackPressed() {
        if (!settingsPageOpen) {
            if (editMode) {
                editMode = false;
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(SettingsProvider.KEY_EDITMODE, editMode);
                editor.apply();
                reloadUI();
            } else {
                showSettingsMain();
                settingsPageOpen = true;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide throbber if going back from another activity
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
            progressBar = null;
        }

        if (!loaded) {
            // Load Packages
            PackageManager packageManager = getPackageManager();
            allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            // Reload UI
            reloadUI();
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
                    setTheme(selectedThemeImageViews, THEME_DRAWABLES.length);
                    break;
                }
            }
        }
    }

    void initBlur() {
        BlurView blurView0 = findViewById(R.id.blurView0);
        BlurView blurView1 = findViewById(R.id.blurView1);

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
    public void reloadUI() {
        Log.i("LauncherStartup", "R0. Execute Background Task");

        int backgroundThemeIndex = sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_THEME, DEFAULT_THEME);
        if (backgroundThemeIndex < THEME_DRAWABLES.length) {
            backgroundImageView.setBackgroundColor(THEME_COLORS[backgroundThemeIndex]);
        }
        new BackgroundTask().execute(this);

        Log.i("LauncherStartup", "R1. Get Preferences");

        boolean names = sharedPreferences.getBoolean(SettingsProvider.KEY_CUSTOM_NAMES, DEFAULT_NAMES);
        int newScaleValueIndex = getPixelFromDip(sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_SCALE, DEFAULT_SCALE));
        appGridView.setColumnWidth(newScaleValueIndex);

        newScaleValueIndex += getPixelFromDip(8);
        editMode = sharedPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false);

        Log.i("LauncherStartup", "R2. Set Apps Adapter");

        appGridView.setAdapter(new AppsAdapter(this, editMode, newScaleValueIndex, names, allApps));

        Log.i("LauncherStartup", "R3. Set Groups Adapter");

        groupPanelGridView.setAdapter(new GroupsAdapter(this, editMode));

        Log.i("LauncherStartup", "R4. Set Columns");

        groupPanelGridView.setNumColumns(Math.min(groupPanelGridView.getAdapter().getCount(), GroupsAdapter.MAX_GROUPS - 1));

        Log.i("LauncherStartup", "R5. Done");
    }

    public void setTheme(ImageView[] views, int index) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SettingsProvider.KEY_CUSTOM_THEME, index);
        editor.apply();
        reloadUI();
    }

    private boolean editMode = false;

    private void showSettingsMain() {
        Dialog dialog = PopupUtils.showPopup(this, R.layout.dialog_settings);

        dialog.setOnDismissListener(dialogInterface -> settingsPageOpen = false);

        ImageView editIcon = dialog.findViewById(R.id.settings_edit_image);
        TextView editText = dialog.findViewById(R.id.settings_edit_text);
        editIcon.setImageResource(editMode ? R.drawable.ic_editing_on : R.drawable.ic_editing_off);
        editText.setText(editMode ? R.string.edit_on : R.string.edit_off);
        dialog.findViewById(R.id.settings_edit).setOnClickListener(view1 -> {
            editMode = !editMode;
            ArrayList<String> selectedGroups = settingsProvider.getAppGroupsSorted(true);
            if (editMode && (selectedGroups.size() > 1)) {
                Set<String> selectFirst = new HashSet<>();
                selectFirst.add(selectedGroups.get(0));
                settingsProvider.setSelectedGroups(selectFirst);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_EDITMODE, editMode);
            editor.apply();
            reloadUI();
            editIcon.setImageResource(editMode ? R.drawable.ic_editing_on : R.drawable.ic_editing_off);
            editText.setText(editMode ? R.string.edit_on : R.string.edit_off);
        });

        dialog.findViewById(R.id.settings_look).setOnClickListener(view -> {
            if (!lookPageOpen) {
                showSettingsLook();
                lookPageOpen = true;
            }
        });

        dialog.findViewById(R.id.settings_service).setOnClickListener(view -> {
            Intent localIntent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
            localIntent.setPackage("com.android.settings");
            startActivity(localIntent);
        });
    }

    public void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private void showSettingsLook() {
        Dialog dialog = PopupUtils.showPopup(this, R.layout.dialog_look);

        dialog.setOnDismissListener(dialogInterface -> lookPageOpen = false);

        Switch names = dialog.findViewById(R.id.switch_names);
        names.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_CUSTOM_NAMES, DEFAULT_NAMES));
        names.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_CUSTOM_NAMES, value);
            editor.apply();
            reloadUI();
        });

        SeekBar scale = dialog.findViewById(R.id.bar_scale);
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(SettingsProvider.KEY_CUSTOM_SCALE, value + 55);
                editor.apply();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                reloadUI();
            }
        });
        scale.setProgress(sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_SCALE, DEFAULT_SCALE) -55);
        scale.setMax(210 - 55);
        // scale.setMin(55);

        int theme = sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_THEME, DEFAULT_THEME);
        ImageView[] views = {
                dialog.findViewById(R.id.theme0),
                dialog.findViewById(R.id.theme1),
                dialog.findViewById(R.id.theme2),
                dialog.findViewById(R.id.theme3),
                dialog.findViewById(R.id.theme4),
                dialog.findViewById(R.id.theme_custom)
        };

        for (ImageView image : views) {
            image.setBackground(getDrawable(R.drawable.bkg_button));
            image.setImageAlpha(255);
        }
        views[theme].setBackground(getDrawable(R.drawable.bkg_button_sel));
        views[theme].setImageAlpha(192);
        for (int i = 0; i < views.length; i++) {
            int index = i;
            views[i].setOnClickListener(view -> {
                if (index >= THEME_DRAWABLES.length) {
                    selectedThemeImageViews = views;
                    ImageUtils.showImagePicker(this, PICK_THEME_CODE);
                } else {
                    setTheme(views, index);
                }
                for (ImageView image : views) {
                    image.setBackground(getDrawable(R.drawable.bkg_button));
                    image.setImageAlpha(255);
                }
                views[index].setBackground(getDrawable(R.drawable.bkg_button_sel));
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
        Log.w("Upkg", pkg);
        intent.setData(Uri.parse("package:" + pkg));
        startActivity(intent);
    }

    // Edit Mode
    Set<String> currentSelectedApps = new HashSet<>();
    boolean hasSelection = false;
    public boolean selectApp(String app) {

        if (currentSelectedApps.contains(app)) {
            currentSelectedApps.remove(app);
            if (currentSelectedApps.isEmpty()) hasSelection = false;
            updateSelectionHint();
            return false;
        } else {
            currentSelectedApps.add(app);
            hasSelection = true;
            updateSelectionHint();

            return true;
        }
    }

    void updateSelectionHint() {
        TextView selectionHint = findViewById(R.id.SelectionHint);

        final int size = currentSelectedApps.size();
        if (size == 1) selectionHint.setText("Click a group to move the selected app.");
        else selectionHint.setText(String.valueOf(size) +" apps selected. Click a group to move them.");
        selectionHint.setVisibility(hasSelection ? View.VISIBLE : View.INVISIBLE);
    }
}