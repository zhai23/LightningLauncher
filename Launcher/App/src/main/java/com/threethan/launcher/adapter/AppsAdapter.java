package com.threethan.launcher.adapter;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Compat;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Icon;
import com.threethan.launcher.helper.Launch;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.ImageLib;
import com.threethan.launcher.lib.StringLib;
import com.threethan.launcher.support.SettingsManager;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/*
    AppsAdapter

    The adapter for app grid views. A separate instance is used for square and banner apps, as well
    as for each activity.

    Only app views matching the current search term or group filter will be shown.
    When a view is requested for an app, it gets cached, and will be reused if displayed again.
    This massively speeds up group-switching and makes search possible without immense lag.

    This class also handles clicking and long clicking apps, including the app settings dialog.
    It also handles displaying/updating the views of an app (hover interactions, background website)
 */
public class AppsAdapter extends BaseAdapter{
    private static Drawable iconDrawable;
    private static File iconFile;
    private static String packageName;
    private LauncherActivity launcherActivity;
    private List<ApplicationInfo> currentAppList;
    private List<ApplicationInfo> fullAppList;
    private final boolean isBanner;
    private boolean getEditMode() {
        return launcherActivity.isEditing();
    }
    public @Nullable View firstView;
    private boolean showTextLabels;
    public AppsAdapter(LauncherActivity activity, boolean names, boolean banner, List<ApplicationInfo> myApps) {
        launcherActivity = activity;
        showTextLabels = names;
        SettingsManager settingsManager = SettingsManager.getInstance(launcherActivity);

        firstView = null;
        currentAppList = Collections.synchronizedList(settingsManager
                .getInstalledApps(activity, settingsManager.getAppGroupsSorted(false), myApps));
        fullAppList = myApps;
        isBanner = banner;
    }
    public void setFullAppList(List<ApplicationInfo> myApps) {
        fullAppList = myApps;
    }
    public void updateAppList(LauncherActivity activity) {
        launcherActivity = activity;

        firstView = null;
        SettingsManager settingsManager = SettingsManager.getInstance(activity);
        currentAppList = Collections.synchronizedList(settingsManager
                .getInstalledApps(activity, settingsManager.getAppGroupsSorted(true), fullAppList));
    }
    public void setShowNames(boolean names) {
        showTextLabels = names;
        clearViewCache();
    }
    public synchronized void filterBy(String text) {
        SettingsManager settingsManager = SettingsManager.getInstance(launcherActivity);
        final List<ApplicationInfo> tempAppList =
                settingsManager.getInstalledApps(launcherActivity, settingsManager.getAppGroupsSorted(false), fullAppList);

        firstView = null;
        currentAppList.clear();
        for (final ApplicationInfo app : tempAppList)
            if (StringLib.forSort(SettingsManager.getAppLabel(app)).contains(StringLib.forSort(text)))
                currentAppList.add(app);

        // Add search queries
        if (!isBanner && !text.isEmpty() && !launcherActivity.isEditing()) {
            final ApplicationInfo googleProxy = new ApplicationInfo();
            googleProxy.packageName = StringLib.googleSearchForUrl(text);
            currentAppList.add(googleProxy);
            final ApplicationInfo youTubeProxy = new ApplicationInfo();
            youTubeProxy.packageName = StringLib.youTubeSearchForUrl(text);
            currentAppList.add(youTubeProxy);
            final ApplicationInfo apkPureProxy = new ApplicationInfo();
            apkPureProxy.packageName = StringLib.apkPureSearchForUrl(text);
            currentAppList.add(apkPureProxy);
        }
    }
    public void setLauncherActivity(LauncherActivity val) {
        launcherActivity = val;
    }

    private static class ViewHolder {
        View view;
        ImageView imageView;
        View clip;
        ImageView imageViewBg;
        TextView textView;
        Button moreButton;
        Button killButton;
        ApplicationInfo app;
    }

    @Override
    public int getCount() { return currentAppList.size(); }

    @Override
    public Object getItem(int position) { return currentAppList.get(position); }

    @Override
    public long getItemId(int position) {
        return position;
    }

    Map<String, ViewHolder> viewCacheSquare = new ConcurrentHashMap<>();
    Map<String, ViewHolder> viewCacheBanner = new ConcurrentHashMap<>();
    private Map<String, ViewHolder> getViewCache() {
        return isBanner ? viewCacheBanner : viewCacheSquare;
    }

    /** @noinspection deprecation*/
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ApplicationInfo currentApp = currentAppList.get(position);

        if (getViewCache().containsKey(Icon.cacheName(currentApp.packageName))) {
            ViewHolder holder = getViewCache().get(Icon.cacheName(currentApp.packageName));
            if (holder != null) {
                if (firstView == null) firstView = holder.view;
                holder.app = currentApp;
                return holder.view;
            }
        }

        ViewHolder holder;

        LayoutInflater layoutInflater = (LayoutInflater) launcherActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            // Create a new ViewHolder and inflate the view
            int layout = App.isBanner(currentApp, launcherActivity) ? R.layout.lv_app_wide : R.layout.lv_app_icon;

            convertView = layoutInflater.inflate(layout, parent, false);
            if (currentApp.packageName == null) return convertView;

            holder = new ViewHolder();
            holder.view = convertView;
            holder.imageView = convertView.findViewById(R.id.imageLabel);
            holder.clip = convertView.findViewById(R.id.clip);
            holder.textView = convertView.findViewById(R.id.textLabel);
            holder.moreButton = convertView.findViewById(R.id.moreButton);
            holder.killButton = convertView.findViewById(R.id.killButton);
            holder.app = currentApp;

            // Set clipToOutline to true on imageView
            convertView.findViewById(R.id.clip).setClipToOutline(true);
            convertView.setTag(holder);
            if (position == 0) launcherActivity.updateGridViews();

        } else holder = (ViewHolder) convertView.getTag();

        // set value into textview
        if (showTextLabels) {
            String name = SettingsManager.getAppLabel(currentApp);
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(name);
            holder.textView.setTextColor(Color.parseColor(launcherActivity.darkMode ? "#FFFFFF" : "#000000"));
            holder.textView.setShadowLayer(6, 0, 0, Color.parseColor(launcherActivity.darkMode ? "#000000" : "#20FFFFFF"));
        } else holder.textView.setVisibility(View.GONE);

        new LoadIconTask().execute(this, currentApp, launcherActivity, holder.imageView, holder.imageViewBg);
        holder.view.post(() -> updateView(holder));

        getViewCache().put(Icon.cacheName(currentApp.packageName), holder);
        return convertView;
    }
    public void clearViewCache() {
        getViewCache().clear();
    }

    private void updateView(ViewHolder holder) {
        holder.view.setOnClickListener(view -> {
            if (getEditMode()) {
                boolean selected = launcherActivity.selectApp(holder.app.packageName);
                ObjectAnimator an = ObjectAnimator.ofFloat(holder.view, "alpha", selected ? 0.5F : 1.0F);
                an.setDuration(150);
                an.start();
            } else {
                if (Launch.launchApp(launcherActivity, holder.app)) animateOpen(holder);

                boolean isWeb = App.isWebsite(holder.app);
                if (isWeb) holder.killButton.setVisibility(View.VISIBLE);
                shouldAnimateClose = isWeb || Platform.isTv(launcherActivity);
            }
        });
        holder.view.setOnLongClickListener(view -> {
            if (getEditMode() || !launcherActivity.canEdit() || launcherActivity.sharedPreferences
                    .getBoolean(Settings.KEY_DETAILS_LONG_PRESS, Settings.DEFAULT_DETAILS_LONG_PRESS)) {
                if (holder.killButton.getVisibility() == View.VISIBLE) holder.killButton.callOnClick();
                else showAppDetails(holder.app);
            } else {
                launcherActivity.setEditMode(true);
                launcherActivity.selectApp(holder.app.packageName);
            }
            return true;
        });

        // A list of selected apps and background-running websites is stored in the launcher activity,
        // then periodically checked by each app's view here.
        Runnable periodicUpdate = new Runnable() {
            @Override
            public void run() {
                holder.moreButton.setVisibility(holder.view.isHovered() || holder.moreButton.isHovered() ? View.VISIBLE : View.GONE);
                boolean selected = launcherActivity.isSelected(holder.app.packageName);
                if (selected != holder.view.getAlpha() < 0.9) {
                    ObjectAnimator an = ObjectAnimator.ofFloat(holder.view, "alpha", selected ? 0.5F : 1.0F);
                    an.setDuration(150);
                    an.start();
                }
                holder.killButton.setVisibility(SettingsManager.getRunning(holder.app.packageName) ? View.VISIBLE : View.GONE);
                // Top search result
                if (launcherActivity.currentTopSearchResult != null &&
                        Objects.equals(
                            Icon.cacheName(launcherActivity.currentTopSearchResult.packageName),
                            Icon.cacheName(holder.app.packageName))) {
                        updateHover(holder, true);
                } else if (launcherActivity.clearFocusPackageNames.contains(Icon.cacheName(holder.app.packageName))) {
                    updateHover(holder, false);
                    launcherActivity.clearFocusPackageNames.remove(Icon.cacheName(holder.app.packageName));
                }
                // Post self again
                holder.view.postDelayed(this, 250);
            }
        };
        periodicUpdate.run();

        View.OnHoverListener hoverListener = (view, event) -> {
            boolean hovered;
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) hovered = true;
            else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                if ((view != holder.moreButton && holder.moreButton.isHovered())
                        || (view != holder.killButton && holder.killButton.isHovered())) {
                    return false;
                } else hovered = false;
            } else return false;
            updateHover(holder, hovered);
            return false;
        };

        holder.view.setOnHoverListener(hoverListener);
        holder.view.setOnFocusChangeListener((view, hasFocus) -> updateHover(holder, hasFocus));

        holder.moreButton.setOnHoverListener(hoverListener);
        holder.killButton.setOnHoverListener(hoverListener);

        holder.moreButton.setOnClickListener(view -> showAppDetails(holder.app));

        holder.killButton.setOnClickListener(view -> {
            SettingsManager.stopRunning(holder.app.packageName);
            view.setVisibility(View.GONE);
        });
    }
    public void updateHover(ViewHolder holder, boolean hovered) {
        holder.killButton.setBackgroundResource(hovered ? R.drawable.ic_circ_running_kb : R.drawable.ic_running_ns);

        final float newScaleInner = hovered ? 1.055f : 1.005f;
        final float newScaleOuter = hovered ? 1.055f : 1.000f;
        final float newElevation = hovered ? 15f : 4f;
        ObjectAnimator aXi = ObjectAnimator.ofFloat(holder.imageView, "scaleX", newScaleInner);
        ObjectAnimator aXv = ObjectAnimator.ofFloat(holder.view, "scaleX", newScaleOuter);
        ObjectAnimator aYi = ObjectAnimator.ofFloat(holder.imageView, "scaleY", newScaleInner);
        ObjectAnimator aYv = ObjectAnimator.ofFloat(holder.view, "scaleY", newScaleOuter);
        ObjectAnimator aAm = ObjectAnimator.ofFloat(holder.moreButton, "alpha", hovered ? 1f : 0f);
        ObjectAnimator aAe = ObjectAnimator.ofFloat(holder.clip, "elevation", newElevation);

        final ObjectAnimator[] animators = new ObjectAnimator[] {aXi, aXv, aYi, aYv, aAm, aAe};
        for (ObjectAnimator animator:animators) animator.setInterpolator(new OvershootInterpolator());
        for (ObjectAnimator animator:animators) animator.setDuration(250);
        for (ObjectAnimator animator:animators) animator.start();

        // Force correct state, even if interrupted
        holder.view.postDelayed(() -> {
            holder.imageView.setScaleX(newScaleInner);
            holder.imageView.setScaleY(newScaleInner);
            holder.view.setScaleX(newScaleOuter);
            holder.view.setScaleY(newScaleOuter);
            holder.clip.setElevation(newElevation);
        }, 250);
    }

    public void onImageSelected(String path, ImageView selectedImageView) {
        Compat.clearIconCache(launcherActivity);
        if (path != null) {
            Bitmap bitmap = ImageLib.bitmapFromFile(launcherActivity, new File(path));
            if (bitmap == null) return;
            bitmap = ImageLib.getResizedBitmap(bitmap, 450);
            ImageLib.saveBitmap(bitmap, iconFile);
            selectedImageView.setImageBitmap(bitmap);
        } else {
            selectedImageView.setImageDrawable(iconDrawable);
            Icon.updateIcon(iconFile, packageName, null);
            // No longer sets icon here but that should be fine
        }
    }
    private void showAppDetails(ApplicationInfo currentApp) {
        // Set View
        AlertDialog dialog = Dialog.build(launcherActivity, R.layout.dialog_app_details);
        // Package Name
        ((TextView) dialog.findViewById(R.id.packageName)).setText(currentApp.packageName);
        // Info Action
        dialog.findViewById(R.id.info).setOnClickListener(view -> App.openInfo(launcherActivity, currentApp.packageName));
        dialog.findViewById(R.id.uninstall).setOnClickListener(view -> {
            App.uninstall(launcherActivity, currentApp.packageName); dialog.dismiss();});


        dialog.findViewById(R.id.kill).setVisibility(
                SettingsManager.getRunning(currentApp.packageName) ? View.VISIBLE : View.GONE);
        dialog.findViewById(R.id.kill).setOnClickListener((view) -> {
            SettingsManager.stopRunning(currentApp.packageName);
            view.setVisibility(View.GONE);
        });


        // Launch Mode Toggle
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        final Switch launchModeSwitch = dialog.findViewById(R.id.launchModeSwitch);
        final View launchOutButton = dialog.findViewById(R.id.launchOut);
        final View launchModeSection = dialog.findViewById(R.id.launchModeSection);
        final View refreshIconButton = dialog.findViewById(R.id.refreshIconButton);
        final boolean appIsVr = App.isVirtualReality(currentApp, launcherActivity);
        final boolean appIsTv = App.isAndroidTv(currentApp, launcherActivity);
        final boolean appIsWeb = App.isWebsite(currentApp);

        // Load Icon
        PackageManager packageManager = launcherActivity.getPackageManager();
        ImageView iconImageView = dialog.findViewById(R.id.appIcon);
        iconImageView.setImageDrawable(Icon.loadIcon(launcherActivity, currentApp, null));

        iconImageView.setClipToOutline(true);
        if (App.isBanner(currentApp, launcherActivity)) iconImageView.getLayoutParams().width = launcherActivity.dp(150);

        iconImageView.setOnClickListener(iconPickerView -> {
            iconDrawable = currentApp.loadIcon(packageManager);
            packageName = currentApp.packageName;

            iconFile = Icon.iconFileForPackage(launcherActivity, currentApp.packageName);
            if (iconFile.exists()) //noinspection ResultOfMethodCallIgnored
                iconFile.delete();
            launcherActivity.setSelectedIconImage(iconImageView, currentApp.packageName);
            ImageLib.showImagePicker(launcherActivity, Settings.PICK_ICON_CODE);
        });

        dialog.findViewById(R.id.info).setVisibility(appIsWeb ? View.GONE : View.VISIBLE);
        if (appIsVr || Platform.isTv(launcherActivity)) {
            // VR apps MUST launch out, so just hide the option and replace it with another
            // Websites could theoretically support this option, but it is currently way too buggy
            launchModeSection.setVisibility(View.GONE);
            refreshIconButton.setVisibility(View.VISIBLE);
            launchOutButton.setVisibility(View.GONE);

            refreshIconButton.setOnClickListener(view -> Icon.reloadIcon(launcherActivity, currentApp, iconImageView));
        } else {
            launchModeSection.setVisibility(View.VISIBLE);
            refreshIconButton.setVisibility(View.GONE);

            launchOutButton.setVisibility(View.VISIBLE);
            launchOutButton.setOnClickListener((view) -> {
                final boolean prevLaunchOut = SettingsManager.getAppLaunchOut(currentApp.packageName);
                SettingsManager.setAppLaunchOut(currentApp.packageName, true);
                Launch.launchApp(launcherActivity, currentApp);
                SettingsManager.setAppLaunchOut(currentApp.packageName, prevLaunchOut);
            });

            launchModeSwitch.setChecked(SettingsManager.getAppLaunchOut(currentApp.packageName));
            launchModeSwitch.setOnCheckedChangeListener((sw, value) -> {
                SettingsManager.setAppLaunchOut(currentApp.packageName, value);

                if (!launcherActivity.sharedPreferences.getBoolean(Settings.KEY_SEEN_LAUNCH_OUT_POPUP, false)) {
                    AlertDialog subDialog = Dialog.build(launcherActivity, R.layout.dialog_launch_out_info);
                    subDialog.findViewById(R.id.confirm).setOnClickListener(view -> {
                        launcherActivity.sharedPreferenceEditor
                                .putBoolean(Settings.KEY_SEEN_LAUNCH_OUT_POPUP, true).apply();
                        subDialog.dismiss();
                    });
                }
            });
        }

        // Show/hide button
        final View showButton = dialog.findViewById(R.id.show);
        final View hideButton = dialog.findViewById(R.id.hide);
        String unhideGroup = SettingsManager.getAppGroupMap().get(currentApp.packageName);
        if (Objects.equals(unhideGroup, Settings.HIDDEN_GROUP))
            unhideGroup = SettingsManager.getDefaultGroup(appIsVr, appIsTv, appIsWeb);
        if (Objects.equals(unhideGroup, Settings.HIDDEN_GROUP))
            try {
                unhideGroup = (String) SettingsManager.getAppGroups().toArray()[0];
            } catch (AssertionError | IndexOutOfBoundsException ignored) {
                unhideGroup = Settings.HIDDEN_GROUP;
                Dialog.toast("Could not find a group to unhide app to!", "", true);
            }
        String finalUnhideGroup = unhideGroup;

        boolean amHidden = Objects.equals(SettingsManager.getAppGroupMap()
                .get(currentApp.packageName), Settings.HIDDEN_GROUP);
        showButton.setVisibility( amHidden ? View.VISIBLE : View.GONE);
        hideButton.setVisibility(!amHidden ? View.VISIBLE : View.GONE);
        showButton.setOnClickListener(v -> {
            launcherActivity.settingsManager.setAppGroup(currentApp.packageName,
                    finalUnhideGroup);
            boolean nowHidden = Objects.equals(SettingsManager.getAppGroupMap()
                    .get(currentApp.packageName), Settings.HIDDEN_GROUP);
            showButton.setVisibility( nowHidden ? View.VISIBLE : View.GONE);
            hideButton.setVisibility(!nowHidden ? View.VISIBLE : View.GONE);
            Dialog.toast(launcherActivity.getString(R.string.moved_shown), finalUnhideGroup, false);
        });
        hideButton.setOnClickListener(v -> {
            launcherActivity.settingsManager.setAppGroup(currentApp.packageName,
                    Settings.HIDDEN_GROUP);
            boolean nowHidden = Objects.equals(SettingsManager.getAppGroupMap()
                    .get(currentApp.packageName), Settings.HIDDEN_GROUP);
            showButton.setVisibility( nowHidden ? View.VISIBLE : View.GONE);
            hideButton.setVisibility(!nowHidden ? View.VISIBLE : View.GONE);
            Dialog.toast(launcherActivity.getString(R.string.moved_hidden),
                    launcherActivity.getString(R.string.moved_hidden_bold), false);

        });

        // Set Label (don't show star)
        String label = SettingsManager.getAppLabel(currentApp);
        final EditText appNameEditText = dialog.findViewById(R.id.appLabel);
        appNameEditText.setText(StringLib.withoutStar(label));
        // Star (actually changes label)
        final ImageView starButton = dialog.findViewById(R.id.star);
        final boolean[] isStarred = {StringLib.hasStar(label)};
        starButton.setImageResource(isStarred[0] ? R.drawable.ic_star_on : R.drawable.ic_star_off);
        starButton.setOnClickListener((view) -> {
            isStarred[0] = !isStarred[0];
            starButton.setImageResource(isStarred[0] ? R.drawable.ic_star_on : R.drawable.ic_star_off);
        });
        // Save Label & Reload on Confirm
        dialog.findViewById(R.id.confirm).setOnClickListener(view -> {
            SettingsManager.setAppLabel(currentApp, StringLib.setStarred(appNameEditText.getText().toString(), isStarred[0]));
            clearViewCache();
            launcherActivity.refreshInterfaceAll();
            dialog.dismiss();
        });
    }


    // Animation

    private void animateOpen(ViewHolder holder) {

        int[] l = new int[2];
        View clip = holder.view.findViewById(R.id.clip);
        clip.getLocationInWindow(l);
        int w = clip.getWidth();
        int h = clip.getHeight();

        View openAnim = launcherActivity.rootView.findViewById(R.id.openAnim);
        openAnim.setX(l[0]);
        openAnim.setY(l[1]);
        ViewGroup.LayoutParams layoutParams = new FrameLayout.LayoutParams(w, h);
        openAnim.setLayoutParams(layoutParams);

        openAnim.setVisibility(View.VISIBLE);
        openAnim.setAlpha(1F);
        openAnim.setClipToOutline(true);
        openAnim.setScaleX(1.08F);
        openAnim.setScaleY(1.08F);

        ImageView animIcon = openAnim.findViewById(R.id.openIcon);
        ImageView animIconBg = openAnim.findViewById(R.id.openIconBg);
        animIcon.setImageDrawable(holder.imageView.getDrawable());
        animIconBg.setImageDrawable(holder.imageView.getDrawable());
        animIconBg.setAlpha(1f);

        View openProgress = launcherActivity.rootView.findViewById(R.id.openProgress);
        openProgress.setVisibility(View.VISIBLE);
        openProgress.setAlpha(0f);

        ObjectAnimator aX = ObjectAnimator.ofFloat(openAnim, "ScaleX", 100f);
        ObjectAnimator aY = ObjectAnimator.ofFloat(openAnim, "ScaleY", 100f);
        ObjectAnimator aA = ObjectAnimator.ofFloat(animIcon, "Alpha", 0f);
        ObjectAnimator aP = ObjectAnimator.ofFloat(openProgress, "Alpha", 0.8f);
        ObjectAnimator aPo = ObjectAnimator.ofFloat(openProgress, "Alpha", 0.0f);
        aX.setDuration(1000);
        aY.setDuration(1000);
        aA.setDuration(500);
        aP.setDuration(500);
        aPo.setDuration(3000);
        aP.setStartDelay(1000);
        aPo.setStartDelay(7000);
        aX.start();
        aY.start();
        aA.start();
        aP.start();
        aPo.start();
    }
    public static boolean shouldAnimateClose = false;
    public static boolean animateClose(LauncherActivity launcherActivity) {
        View openAnim = launcherActivity.rootView.findViewById(R.id.openAnim);
        ImageView animIcon = openAnim.findViewById(R.id.openIcon);

        View openProgress = launcherActivity.rootView.findViewById(R.id.openProgress);
        openProgress.setVisibility(View.INVISIBLE);

        final boolean rv = (openAnim.getVisibility() == View.VISIBLE);
        if (!AppsAdapter.shouldAnimateClose) {
            openAnim.setVisibility(View.INVISIBLE);
            openAnim.setScaleX(1);
            openAnim.setScaleY(1);
            animIcon.setAlpha(1.0f);
            openAnim.setVisibility(View.INVISIBLE);
        } else {
            // This animation assumes the app already animated open, and therefore does not set icon
            ImageView animIconBg = openAnim.findViewById(R.id.openIconBg);

            openAnim.setScaleX(3f);
            openAnim.setScaleY(3f);
            animIcon.setAlpha(1f);

            ObjectAnimator aX = ObjectAnimator.ofFloat(openAnim, "ScaleX", 1.08f);
            ObjectAnimator aY = ObjectAnimator.ofFloat(openAnim, "ScaleY", 1.08f);
            ObjectAnimator aA = ObjectAnimator.ofFloat(animIconBg, "Alpha", 0f);

            aX.setDuration(100);
            aY.setDuration(100);
            aA.setDuration(50);
            aA.setStartDelay(50);
            aA.start();
            aX.start();
            aY.start();

            openAnim.postDelayed(() -> openAnim.setVisibility(View.INVISIBLE), 100);
            shouldAnimateClose = false;
        }
        return rv;
    }
}
