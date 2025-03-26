package com.threethan.launcher.activity.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BaseInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.threethan.launcher.R;
import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launcher.activity.dialog.AppDetailsDialog;
import com.threethan.launcher.activity.support.SettingsManager;
import com.threethan.launcher.data.Settings;
import com.threethan.launcher.helper.LaunchExt;
import com.threethan.launcher.helper.PlatformExt;
import com.threethan.launcher.helper.PlaytimeHelper;
import com.threethan.launchercore.adapter.ArrayListAdapter;
import com.threethan.launchercore.lib.ImageLib;
import com.threethan.launchercore.lib.StringLib;
import com.threethan.launchercore.metadata.IconLoader;
import com.threethan.launchercore.util.App;
import com.threethan.launchercore.util.Platform;

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
public class LauncherAppsAdapter extends ArrayListAdapter<ApplicationInfo, LauncherAppsAdapter.AppViewHolder> {
    private LauncherActivity launcherActivity;
    private Set<ApplicationInfo> fullAppSet;
    private boolean getEditMode() {
        return launcherActivity.isEditing();
    }
    public LauncherAppsAdapter(LauncherActivity activity) {
        setHasStableIds(true);
        launcherActivity = activity;
    }

    public void setFullAppSet(Set<ApplicationInfo> myApps) {
        fullAppSet = myApps;
    }
    public synchronized void setAppList(LauncherActivity activity) {
        SettingsManager settingsManager = SettingsManager.getInstance(activity);
        launcherActivity = activity;
        setItems(Collections.unmodifiableList(settingsManager
                .getVisibleApps(settingsManager.getAppGroupsSorted(true), fullAppSet)));
    }
    public synchronized void filterBy(String text) {
        boolean showHidden = !text.isEmpty() && launcherActivity.dataStoreEditor.getBoolean(
                Settings.KEY_SEARCH_HIDDEN, Settings.DEFAULT_SEARCH_HIDDEN);

        SettingsManager settingsManager = SettingsManager.getInstance(launcherActivity);
        final List<ApplicationInfo> newItems =
                settingsManager.getVisibleApps(settingsManager.getAppGroupsSorted(false),
                        fullAppSet);

        newItems.removeIf(item -> !StringLib.forSort(SettingsManager.getAppLabel(item))
                .contains(StringLib.forSort(text)));
        if (!showHidden)
            newItems.removeIf(item -> Objects.equals(SettingsManager.getAppGroupMap()
                    .get(item.packageName), Settings.HIDDEN_GROUP));

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

        setItems(newItems);
    }
    public void setLauncherActivity(LauncherActivity val) {
        launcherActivity = val;
        layoutInflater =
                (LayoutInflater) launcherActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    protected static class AppViewHolder extends RecyclerView.ViewHolder {
        View view;
        ImageView imageView;
        View clip;
        TextView textView;
        Button moreButton;
        Button playtimeButton;
        ApplicationInfo app;
        @Nullable Boolean banner = null;
        @Nullable Boolean darkMode = null;
        @Nullable Boolean showName = null;
        boolean hovered = false;
        @Nullable Runnable iconRunnable = null;
        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AppViewHolder that)) return false;
            return Objects.equals(app.packageName, that.app.packageName);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(app.packageName);
        }
    }

    public ApplicationInfo getItem(int position) { return items.get(position); }
    private static LayoutInflater layoutInflater;


    /** @noinspection ClassEscapesDefinedScope*/
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = layoutInflater.inflate(R.layout.item_app, parent, false);

        AppViewHolder holder =  new AppViewHolder(itemView);
        itemView.findViewById(R.id.clip).setClipToOutline(true);

        holder.view = itemView;
        holder.imageView = itemView.findViewById(R.id.itemIcon);
        holder.clip = itemView.findViewById(R.id.clip);
        holder.textView = itemView.findViewById(R.id.itemLabel);
        holder.moreButton = itemView.findViewById(R.id.moreButton);
        holder.playtimeButton = itemView.findViewById(R.id.playtimeButton);

        setActions(holder);
        return holder;
    }
    private void setActions(AppViewHolder holder) {
        // Launch app on click
        holder.view.setOnClickListener(view -> {
            if (getEditMode()) {
                boolean selected = launcherActivity.selectApp(holder.app.packageName);
                holder.view.animate().alpha(selected ? 0.5f : 1).setDuration(150).start();
            } else {
                if (LaunchExt.launchApp(launcherActivity, holder.app)) animateOpen(holder);

                boolean isWeb = App.isWebsite(holder.app.packageName);
                shouldAnimateClose = isWeb || Platform.isTv();
            }
        });
        holder.view.setOnLongClickListener(view -> {
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

        holder.moreButton.setOnClickListener(view
                -> new AppDetailsDialog(launcherActivity, holder.app).show());
        holder.playtimeButton.setOnClickListener(v
                -> PlaytimeHelper.openFor(holder.app.packageName));
    }

    /** @noinspection ClassEscapesDefinedScope*/
    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        final ApplicationInfo app = getItem(position);

        //noinspection WrapperTypeMayBePrimitive
        final Boolean banner = App.isBanner(app);
        if (banner != holder.banner) {
            ConstraintLayout.LayoutParams ilp
                    = (ConstraintLayout.LayoutParams) holder.imageView.getLayoutParams();

            ilp.dimensionRatio = banner ? "16:9" : "1:1";
            holder.banner = banner;
        }
        if (LauncherActivity.darkMode != holder.darkMode) {
            holder.textView.setTextColor(Color.parseColor(
                    LauncherActivity.darkMode ? "#FFFFFF" : "#000000"));
            holder.textView.setShadowLayer(6, 0, 0, Color.parseColor(
                    LauncherActivity.darkMode ? "#000000" : "#FFFFFF"));
            final Drawable fg = ResourcesCompat.getDrawable(launcherActivity.getResources(),
                    LauncherActivity.darkMode ? R.drawable.fg_app_dm : R.drawable.fg_app_lm,
                    null);
            holder.clip.setForeground(fg);
            holder.darkMode = LauncherActivity.darkMode;
        }

        //noinspection WrapperTypeMayBePrimitive
        final Boolean showName = banner && LauncherActivity.namesBanner
                || !banner && LauncherActivity.namesSquare;
        if (showName != holder.showName) {
            holder.textView.setVisibility(showName ? View.VISIBLE : View.INVISIBLE);
            ((ViewGroup.MarginLayoutParams) holder.view.getLayoutParams())
                    .setMargins(0, 0, 0, showName ? 0 : -15);
            holder.showName = showName;
        }

        if (!Platform.isTv() && banner) holder.playtimeButton.setText("--:--");

        // set value into textview
        holder.app = app;

        // Load Icon
        IconLoader.loadIcon(holder.app, drawable
                -> {
            if (holder.iconRunnable != null) holder.imageView.removeCallbacks(holder.iconRunnable);
            if (drawable == null) return;
            if (holder.app == app) {
                if (drawable instanceof BitmapDrawable bitmapDrawable) {
//                    Bitmap bitmap = ImageLib.getResizedBitmap(
//                            bitmapDrawable.getBitmap(),
//                            128 * getItemViewType(position));

                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    // It's java's fault for allowing this...
                    if (holder.imageView.getDrawable() instanceof BitmapDrawable currentBd &&
                            ImageLib.isIdenticalFast(currentBd.getBitmap(), bitmap)) return;

                    holder.iconRunnable = () -> holder.imageView.setImageBitmap(bitmap);
                } else holder.iconRunnable = () -> holder.imageView.setImageDrawable(drawable);

                holder.imageView.post(holder.iconRunnable);
            }
            else launcherActivity.launcherService.forEachActivity(a -> {
                    LauncherAppsAdapter adapter = launcherActivity.getAppAdapter();
                    if (adapter != null) adapter.notifyItemChanged(app);
            });
        });

        // Load label
        SettingsManager.getAppLabel(app, label -> {
            if (holder.app == app) holder.textView.post(() -> holder.textView.setText(label));
            else launcherActivity.launcherService.forEachActivity(a -> {
                LauncherAppsAdapter adapter = launcherActivity.getAppAdapter();
                if (adapter != null) adapter.notifyItemChanged(app);
            });
        });

        updateSelected(holder);
    }

    public void notifySelectionChange(String packageName) {
        for (int i=0; i<items.size(); i++)
            if (Objects.equals(items.get(i).packageName, packageName))
                notifyItemChanged(i, items.get(i));
    }

    @Override
    public long getItemId(int position) {
        return Objects.hashCode(getItem(position).packageName); // Assuming this is unique!
    }

    private void updateSelected(AppViewHolder holder) {
        boolean selected = launcherActivity.isSelected(holder.app.packageName);
        if (selected != holder.view.getAlpha() < 0.9) {
            ObjectAnimator an = ObjectAnimator.ofFloat(holder.view, "alpha",
                    selected ? 0.5F : 1.0F);
            an.setDuration(150);
            an.start();
        }
        // Top search result
        String cname = IconLoader.cacheName(holder.app);
        if (launcherActivity.currentTopSearchResultName != null
                && Objects.equals(launcherActivity.currentTopSearchResultName, cname)) {
            updateAppFocus(holder, true, FocusSource.SEARCH);
            launcherActivity.currentTopSearchResultName = null;
        } else if (launcherActivity.prevTopSearchResultNames.contains(cname)) {
            updateAppFocus(holder, false, FocusSource.SEARCH);
            launcherActivity.prevTopSearchResultNames.remove(cname);
        }
    }
    // Only one focused app is allowed per source at a time.
    private enum FocusSource { CURSOR, SEARCH }

    private final Map<FocusSource, AppViewHolder> focusedHolderBySource = new HashMap<>();
    /** @noinspection ClassEscapesDefinedScope*/
    public synchronized void updateAppFocus(AppViewHolder holder, boolean focused, FocusSource source) {
        // Handle focus sources
        if (focused) {
            AppViewHolder prevHolder = focusedHolderBySource.getOrDefault(source, null);
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
        final float newElevation = focused ? (tv ? 15f : 20f) : 3f;
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

        ObjectAnimator aE = ObjectAnimator.ofFloat(holder.clip, "elevation",
                newElevation);
        aE.setDuration(duration).start();

        boolean banner = Boolean.TRUE.equals(holder.banner);
        if (banner && !LauncherActivity.namesBanner || !banner && !LauncherActivity.namesSquare)
            holder.textView.setVisibility(focused ? View.VISIBLE : View.INVISIBLE);

        holder.view.setActivated(true);
        // Force correct state, even if interrupted
        holder.view.postDelayed(() -> {
            if (holder.hovered == focused) {
                holder.imageView.setScaleX(newScaleInner);
                holder.imageView.setScaleY(newScaleInner);
                holder.view.setScaleX(newScaleOuter);
                holder.view.setScaleY(newScaleOuter);
                holder.clip.setElevation(newElevation);
                holder.view.setActivated(false);
            }
        },  tv ? 200 : 300);

        holder.view.setZ(focused ? 2 : 1);
        holder.hovered = focused;
    }
    @Override
    public int getItemViewType(int position) {
        return App.isBanner(items.get(position)) ? 2 : 1;
    }

    // Animation

    private void animateOpen(AppViewHolder holder) {

        int[] l = new int[2];
        View clip = holder.view.findViewById(R.id.clip);
        clip.getLocationInWindow(l);
        int w = clip.getWidth();
        int h = clip.getHeight();

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
    public static boolean shouldAnimateClose = false;
    public static boolean animateClose(LauncherActivity launcherActivity) {
        View openAnim = launcherActivity.rootView.findViewById(R.id.openAnim);
        ImageView animIcon = openAnim.findViewById(R.id.openIcon);

        View openProgress = launcherActivity.rootView.findViewById(R.id.openProgress);
        openProgress.setVisibility(View.INVISIBLE);

        final boolean rv = (openAnim.getVisibility() == View.VISIBLE);
        if (!LauncherAppsAdapter.shouldAnimateClose) {
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