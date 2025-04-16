package com.threethan.launcher.activity.adapter;

import android.animation.ObjectAnimator;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BaseInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.threethan.launcher.R;
import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launcher.activity.dialog.AppDetailsDialog;
import com.threethan.launcher.activity.support.SettingsManager;
import com.threethan.launcher.data.Settings;
import com.threethan.launcher.helper.LaunchExt;
import com.threethan.launcher.helper.PlaytimeHelper;
import com.threethan.launchercore.adapter.AppsAdapter;
import com.threethan.launchercore.lib.StringLib;
import com.threethan.launchercore.util.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *     The adapter for the main app grid.
 * <p>
 *     Only app views matching the current search term or group filter will be shown.
 *     When a view is requested for an app, it gets cached, and will be reused if displayed again.
 *     This massively speeds up group-switching and makes search possible without immense lag.
 * <p>
 *     This class also handles clicking and long clicking apps, including the app settings dialog.
 *     It also handles displaying/updating the views of an app (hover interactions, background website)
 */
public class LauncherAppsAdapter extends AppsAdapter<LauncherAppsAdapter.AppViewHolderExt> {
    private LauncherActivity launcherActivity;
    private Set<ApplicationInfo> fullAppSet;
    protected ApplicationInfo topSearchResult = null;

    private boolean getEditMode() {
        return launcherActivity.isEditing();
    }
    public LauncherAppsAdapter(LauncherActivity activity) {
        super(R.layout.item_app, null);
        launcherActivity = activity;
    }

    public void setFullAppSet(Set<ApplicationInfo> myApps) {
        fullAppSet = myApps;
    }
    public synchronized void setAppList(LauncherActivity activity) {
        SettingsManager settingsManager = SettingsManager.getInstance(activity);
        launcherActivity = activity;

        topSearchResult = null;
        prevFilterText = "";
        setFullItems(Collections.unmodifiableList(settingsManager
                .getVisibleAppsSorted(settingsManager.getAppGroupsSorted(true), fullAppSet)));
    }

    private static String prevFilterText = "";
    public synchronized void filterBy(String text) {
        if (text.isEmpty()) {
            prevFilterText = "";
            refresh();
            return;
        }

        SettingsManager settingsManager = SettingsManager.getInstance(launcherActivity);

        boolean showHidden = !text.isEmpty() && launcherActivity.dataStoreEditor.getBoolean(
                Settings.KEY_SEARCH_HIDDEN, Settings.DEFAULT_SEARCH_HIDDEN);

        boolean reList = !text.startsWith(prevFilterText);
        prevFilterText = text;

        final List<ApplicationInfo> newItems = reList ?
                settingsManager.getVisibleAppsSorted(
                        settingsManager.getAppGroupsSorted(false),
                        fullAppSet) : new ArrayList<>(getCurrentList());

        newItems.removeIf(item -> !
                SettingsManager.getAppLabel(item).toLowerCase().contains(text.toLowerCase()));


        if (!showHidden) {
            Set<String> hg = SettingsManager.getGroupAppsMap().get(Settings.HIDDEN_GROUP);
            if (hg != null) newItems.removeIf(ai -> hg.contains(ai.packageName));
        }

        boolean showWeb = !text.isEmpty() && launcherActivity.dataStoreEditor
                .getBoolean(Settings.KEY_SEARCH_WEB, Settings.DEFAULT_SEARCH_WEB);

        // Add search queries
        if (showWeb && !launcherActivity.isEditing()) {
            final ApplicationInfo googleProxy = new ApplicationInfo();
            googleProxy.packageName = StringLib.googleSearchForUrl(text);
            newItems.add(googleProxy);

            final ApplicationInfo youTubeProxy = new ApplicationInfo();
            youTubeProxy.packageName = StringLib.youTubeSearchForUrl(text);
            newItems.add(youTubeProxy);

            final ApplicationInfo apkPureProxy = new ApplicationInfo();
            apkPureProxy.packageName = StringLib.apkPureSearchForUrl(text);
            newItems.add(apkPureProxy);

            final ApplicationInfo apkMirrorProxy = new ApplicationInfo();
            apkMirrorProxy.packageName = StringLib.apkMirrorSearchForUrl(text);
            newItems.add(apkMirrorProxy);
        }

        topSearchResult = newItems.isEmpty() ? null : newItems.get(0);
        submitList(newItems, () -> notifyItemChanged(topSearchResult));
    }
    public void setLauncherActivity(LauncherActivity val) {
        launcherActivity = val;
    }

    public ApplicationInfo getTopSearchResult() {
        return topSearchResult;
    }

    protected static class AppViewHolderExt extends AppsAdapter.AppViewHolder {
        Button moreButton;
        Button playtimeButton;
        boolean hovered = false;
        public AppViewHolderExt(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    protected AppViewHolderExt newViewHolder(View itemView) {
        return new AppViewHolderExt(itemView);
    }

    protected void setupViewHolder(AppViewHolderExt holder) {
        holder.moreButton = holder.view.findViewById(R.id.moreButton);
        holder.playtimeButton = holder.view.findViewById(R.id.playtimeButton);
        holder.imageView.setClipToOutline(true);

        // Launch app on click
        holder.view.setOnClickListener(view -> {
            if (holder.app == null || holder.app.packageName == null) return;
            if (getEditMode()) {
                boolean selected = launcherActivity.selectApp(holder.app.packageName);
                holder.view.animate().alpha(selected ? 0.5f : 1).setDuration(150).start();
            } else {
                if (LaunchExt.launchApp(launcherActivity, holder.app)) animateOpen(holder);
            }
        });
        holder.view.setOnLongClickListener(view -> {
            if (holder.app == null || holder.app.packageName == null) return false;
            if (getEditMode() || !launcherActivity.canEdit() || launcherActivity.dataStoreEditor
                    .getBoolean(Settings.KEY_DETAILS_LONG_PRESS, Settings.
                            DEFAULT_DETAILS_LONG_PRESS)) {
                new AppDetailsDialog(launcherActivity, holder.app).show();
            } else {
                launcherActivity.setEditMode(true);
                launcherActivity.selectApp(holder.app.packageName);
                holder.view.animate().alpha(0.5f).setDuration(300).start();
            }
            return true;
        });

        // Hover
        View.OnHoverListener hoverListener = (view, event) -> {
            boolean hovered;
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) hovered = true;
            else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                for (View subView : new View[] {holder.playtimeButton, holder.moreButton})
                    if (view != subView && subView != null && subView.isHovered()) return false;
                hovered = false;
            } else return false;
            updateAppFocus(holder, hovered, FocusSource.CURSOR);
            return false;
        };

        holder.view.setOnHoverListener(hoverListener);
        holder.view.setOnFocusChangeListener((view, hasFocus) -> updateAppFocus(holder, hasFocus, FocusSource.CURSOR));

        holder.view.findViewById(R.id.moreButton).setOnClickListener(
                view -> new AppDetailsDialog(launcherActivity, holder.app).show());

        holder.view.findViewById(R.id.playtimeButton).setOnClickListener(v
                -> PlaytimeHelper.openFor(holder.app.packageName));


    }

    @Override
    protected void onViewHolderReady(AppViewHolderExt holder) {
        super.onViewHolderReady(holder);
        if (!Platform.isTv())
            holder.playtimeButton.post(() -> holder.playtimeButton.setText("--:--"));
        holder.view.post(() -> updateSelected(holder));
    }

    @Override
    protected void onIconChanged(AppViewHolderExt holder, Drawable icon) {
        launcherActivity.runOnUiThread(() ->
                Glide.with(holder.imageView.getContext())
                .load(icon)
                .centerCrop()
                .into(holder.imageView));

    }

    public void notifySelectionChange(String packageName) {
        for (int i=0; i<getItemCount(); i++)
            if (Objects.equals(getItem(i).packageName, packageName))
                notifyItemChanged(i, getItem(i));
    }

    private void updateSelected(AppViewHolderExt holder) {
        if (holder.app == null || holder.app.packageName == null) return;
        boolean selected = launcherActivity.isSelected(holder.app.packageName);
        if (selected != holder.view.getAlpha() < 0.9) {
            ObjectAnimator an = ObjectAnimator.ofFloat(holder.view, "alpha",
                    selected ? 0.5F : 1.0F);
            an.setDuration(150);
            an.start();
        }

        // Top search result
        if (topSearchResult != null && holder.app != null
                && Objects.equals(topSearchResult.packageName, holder.app.packageName)) {
            updateAppFocus(holder, true, FocusSource.SEARCH);
        }
    }
    // Only one focused app is allowed per source at a time.
    private enum FocusSource { CURSOR, SEARCH }

    private final Map<FocusSource, AppViewHolderExt> focusedHolderBySource = new HashMap<>();
    /** @noinspection ClassEscapesDefinedScope*/
    public synchronized void updateAppFocus(AppViewHolderExt holder, boolean focused, FocusSource source) {
        // Handle focus sources
        if (focused) {
            AppViewHolderExt prevHolder = focusedHolderBySource.getOrDefault(source, null);
            focusedHolderBySource.put(source, holder);
            // If not focused by another source, remove prev. hover
            if (!focusedHolderBySource.containsValue(prevHolder) && prevHolder != null)
                updateAppFocus(prevHolder, false, source);
        } else if (focusedHolderBySource.containsValue(holder)) {
            focusedHolderBySource.remove(source);
            // Return early if focused by another source
            if (focusedHolderBySource.containsValue(holder)) return;
        }

        if (holder.hovered == focused) return;

        if (!Platform.isTv())
            holder.moreButton.setVisibility(focused ? View.VISIBLE : View.INVISIBLE);

        if (!Platform.isTv() && LauncherActivity.timesBanner) {
            if (focused) {
                // Show and update view holder
                holder.playtimeButton.setVisibility(Boolean.TRUE.equals(holder.banner)
                        && !holder.app.packageName.contains("://")
                        ? View.VISIBLE : View.INVISIBLE);
                if (Boolean.TRUE.equals(holder.banner)
                        && !holder.app.packageName.contains("://")) {
                    PlaytimeHelper.getPlaytime(holder.app.packageName,
                            t -> launcherActivity.runOnUiThread(()
                                    -> holder.playtimeButton.setText(t)));
                }
            } else holder.playtimeButton.setVisibility(View.INVISIBLE);
        }
        final boolean tv = Platform.isTv();
        final float newScaleInner = focused ? (tv ? 1.055f : 1.050f) : 1.005f;
        final float newScaleOuter = focused ? (tv ? 1.270f : 1.085f) : 1.005f;

        final float newElevation = focused ? 20f : 3f;

        final float textScale = 1-(1-(1/newScaleOuter))*0.7f;
        final int duration = tv ? 175 : 250;
        BaseInterpolator interpolator = Platform.isTv() ?
                new LinearInterpolator() : new OvershootInterpolator();

        holder.imageView.animate().scaleX(newScaleInner).scaleY(newScaleInner)
                .setDuration(duration).setInterpolator(interpolator).start();
        holder.view     .animate().scaleX(newScaleOuter).scaleY(newScaleOuter)
                .setDuration(duration).setInterpolator(interpolator).start();
        holder.moreButton.animate().alpha(focused ? 1f : 0f)
                .setDuration(duration).setInterpolator(interpolator).start();
        holder.textView .animate().scaleX(textScale).scaleY(textScale)
                .setDuration(duration).setInterpolator(interpolator).start();

        ObjectAnimator aE = ObjectAnimator.ofFloat(holder.imageView, "elevation",
            newElevation);
        aE.setDuration(duration).start();

        boolean banner = Boolean.TRUE.equals(holder.banner);
        if (banner && !LauncherActivity.namesBanner || !banner && !LauncherActivity.namesSquare)
            holder.textView.setVisibility(focused ? View.VISIBLE : View.INVISIBLE);

        holder.view.setActivated(true);
        // Force correct state, even if interrupted
        holder.view.postDelayed(() -> {
            if (Objects.equals(focusedHolderBySource.get(source), holder)) {
                holder.imageView.setScaleX(newScaleInner);
                holder.imageView.setScaleY(newScaleInner);
                holder.view.setScaleX(newScaleOuter);
                holder.view.setScaleY(newScaleOuter);
                holder.imageView.setElevation(newElevation);
                holder.view.setActivated(false);
            }
        },  tv ? 200 : 300);

        holder.view.setZ(focused ? 2 : 1);
        holder.hovered = focused;
    }

    // Animation

    private void animateOpen(AppViewHolderExt holder) {

        int[] l = new int[2];
        holder.imageView.getLocationInWindow(l);
        int w = holder.imageView.getWidth();
        int h = holder.imageView.getHeight();

        View openAnim = launcherActivity.rootView.findViewById(R.id.openAnim);
        openAnim.setX(l[0] + (Platform.isTv() ? 30 : 3));
        openAnim.setY(l[1] + (Platform.isTv() ? 30 : 3));
        ViewGroup.LayoutParams layoutParams = new FrameLayout.LayoutParams(w, h);
        openAnim.setLayoutParams(layoutParams);

        openAnim.setVisibility(View.VISIBLE);
        openAnim.setAlpha(1F);
        openAnim.setClipToOutline(true);
        openAnim.setScaleX(Platform.isTv() ? 1.40F : 1.15F);
        openAnim.setScaleY(Platform.isTv() ? 1.40F : 1.15F);

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
        ObjectAnimator aPo= ObjectAnimator.ofFloat(openProgress, "Alpha", 0.0f);
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
    public static boolean animateClose(LauncherActivity launcherActivity) {
        View openAnim = launcherActivity.rootView.findViewById(R.id.openAnim);
        ImageView animIcon = openAnim.findViewById(R.id.openIcon);

        View openProgress = launcherActivity.rootView.findViewById(R.id.openProgress);
        openProgress.setVisibility(View.INVISIBLE);

        final boolean rv = (openAnim.getVisibility() == View.VISIBLE);
        {
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
        }
        return rv;
    }
}