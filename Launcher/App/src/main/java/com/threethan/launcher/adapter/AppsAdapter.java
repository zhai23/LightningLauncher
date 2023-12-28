package com.threethan.launcher.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.Icon;
import com.threethan.launcher.helper.Launch;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.helper.Settings;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.lib.StringLib;
import com.threethan.launcher.support.AppDetailsDialog;
import com.threethan.launcher.support.SettingsManager;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.AppViewHolder> {
    private LauncherActivity launcherActivity;
    private List<ApplicationInfo> currentAppList;
    private List<ApplicationInfo> fullAppList;
    private boolean getEditMode() {
        return launcherActivity.isEditing();
    }
    public @Nullable View firstView;
    public AppsAdapter(LauncherActivity activity, List<ApplicationInfo> myApps) {
        launcherActivity = activity;
        SettingsManager settingsManager = SettingsManager.getInstance(launcherActivity);

        firstView = null;
        currentAppList = Collections.synchronizedList(settingsManager
                .getInstalledApps(activity, settingsManager.getAppGroupsSorted(false), myApps));
        fullAppList = myApps;
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
        if (!text.isEmpty() && !launcherActivity.isEditing()) {

            final ApplicationInfo googleProxy = new ApplicationInfo();
            googleProxy.packageName = StringLib.googleSearchForUrl(text);
            currentAppList.add(googleProxy);

            final ApplicationInfo youTubeProxy = new ApplicationInfo();
            youTubeProxy.packageName = StringLib.youTubeSearchForUrl(text);
            currentAppList.add(youTubeProxy);

            final ApplicationInfo apkPureProxy = new ApplicationInfo();
            apkPureProxy.packageName = StringLib.apkPureSearchForUrl(text);
            currentAppList.add(apkPureProxy);

            final ApplicationInfo apkMirrorProxy = new ApplicationInfo();
            apkMirrorProxy.packageName = StringLib.apkMirrorSearchForUrl(text);
            currentAppList.add(apkMirrorProxy);
        }
    }
    public void setLauncherActivity(LauncherActivity val) {
        launcherActivity = val;
    }

    protected static class AppViewHolder extends RecyclerView.ViewHolder {
        View view;
        ImageView imageView;
        ImageView imageViewSquare;
        ImageView imageViewBanner;
        View clip;
        TextView textView;
        Button moreButton;
        Button killButton;
        ApplicationInfo app;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public ApplicationInfo getItem(int position) { return currentAppList.get(position); }
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) launcherActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = layoutInflater.inflate(R.layout.lv_app, parent, false);
        AppViewHolder holder =  new AppViewHolder(itemView);
        itemView.findViewById(R.id.clip).setClipToOutline(true);

        holder.view = itemView;
        holder.imageViewSquare = itemView.findViewById(R.id.imageLabelSquare);
        holder.imageViewBanner = itemView.findViewById(R.id.imageLabelBanner);
        holder.clip = itemView.findViewById(R.id.clip);
        holder.textView = itemView.findViewById(R.id.textLabel);
        holder.moreButton = itemView.findViewById(R.id.moreButton);
        holder.killButton = itemView.findViewById(R.id.killButton);

        setActions(holder);

        return holder;
    }
    private void setActions(AppViewHolder holder) {
        // Sub-buttons
        holder.moreButton.setOnClickListener(view -> AppDetailsDialog.showAppDetails(holder.app, launcherActivity));

        holder.killButton.setOnClickListener(view -> {
            SettingsManager.stopRunning(holder.app.packageName);
            view.setVisibility(View.GONE);
        });

        // Click
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
                if (holder.killButton.getVisibility() == View.VISIBLE)
                    holder.killButton.callOnClick();
                else AppDetailsDialog.showAppDetails(holder.app, launcherActivity);
            } else {
                launcherActivity.setEditMode(true);
                launcherActivity.selectApp(holder.app.packageName);
            }
            return true;
        });

        // Hover
        View.OnHoverListener hoverListener = (view, event) -> {
            boolean hovered;
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) hovered = true;
            else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                for (View subView : new View[] {holder.moreButton, holder.killButton})
                    if (view != subView && subView != null && subView.isHovered()) return false;
                hovered = false;
            } else return false;
            updateHover(holder, hovered);
            return false;
        };

        holder.view.setOnHoverListener(hoverListener);
        holder.view.setOnFocusChangeListener((view, hasFocus) -> updateHover(holder, hasFocus));

        holder.moreButton.setOnHoverListener(hoverListener);
        holder.killButton.setOnHoverListener(hoverListener);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        ApplicationInfo app = getItem(position);

        // Square vs Banner
        final boolean banner = App.isBanner(launcherActivity, app);
        holder.imageViewSquare.setVisibility(banner ? View.GONE : View.VISIBLE);
        holder.imageViewBanner.setVisibility(banner ? View.VISIBLE : View.GONE);
        holder.imageView = banner ? holder.imageViewBanner : holder.imageViewSquare;



        // set value into textview
        if (banner && LauncherActivity.namesBanner || !banner && LauncherActivity.namesSquare) {
            String name = SettingsManager.getAppLabel(app);
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(name);
            holder.textView.setTextColor(Color.parseColor(launcherActivity.darkMode ? "#FFFFFF" : "#000000"));
            holder.textView.setShadowLayer(6, 0, 0, Color.parseColor(launcherActivity.darkMode ? "#000000" : "#20FFFFFF"));
        } else holder.textView.setVisibility(View.GONE);

        holder.app = app;

        //Load Icon
        Drawable appIcon = Icon.loadIcon(launcherActivity, holder.app, holder.imageView);
        launcherActivity.runOnUiThread(() -> holder.imageView.setImageDrawable(appIcon));

        updateSelected(holder);

    }

    public void notifySelectionChange(String packageName) {
        for (int i=0; i<currentAppList.size(); i++)
            if (Objects.equals(currentAppList.get(i).packageName, packageName))
                notifyItemChanged(i, currentAppList.get(i));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return currentAppList.size();
    }
    private void updateSelected(AppViewHolder holder) {
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
    }
    public void updateHover(AppViewHolder holder, boolean hovered) {
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
    @Override
    public int getItemViewType(int position) {
        return App.isBanner(launcherActivity, currentAppList.get(position)) ? 2 : 1;
    }

    // Animation

    private void animateOpen(AppViewHolder holder) {

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
