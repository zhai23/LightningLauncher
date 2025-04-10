package com.threethan.launchercore.adapter;

import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.threethan.launcher.activity.LauncherActivity;
import com.threethan.launchercore.metadata.IconLoader;
import com.threethan.launchercore.util.App;
import com.threethan.launcher.R;
import com.threethan.launchercore.view.LcContainerView;


import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AppsAdapter<VH extends AppsAdapter.AppViewHolder>
        extends ListAdapter<ApplicationInfo, VH> {

    private List<ApplicationInfo> fullAppList;
    @Nullable protected App.Type showOnly;
    protected final int itemLayoutResId;

    private static final Set<AppsAdapter<?>> instances = new HashSet<>();

    private static void runOnEachInstance(Consumer<AppsAdapter<?>> consumer) {
        instances.forEach(consumer);
    }

    /** @noinspection rawtypes*/
    public static WeakReference<AppsAdapter> lastInstanceReference = new WeakReference<>(null);

    private static final DiffUtil.ItemCallback<ApplicationInfo> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull ApplicationInfo oldItem, @NonNull ApplicationInfo newItem) {
            return oldItem.packageName.equals(newItem.packageName);
        }

        @Override
        public boolean areContentsTheSame(@NonNull ApplicationInfo oldItem, @NonNull ApplicationInfo newItem) {
            return oldItem.packageName.equals(newItem.packageName);
        }
    };

    public AppsAdapter(int itemLayoutResId, @Nullable App.Type showOnly) {
        super(DIFF_CALLBACK);
        setHasStableIds(true);
        this.showOnly = showOnly;
        this.itemLayoutResId = itemLayoutResId;
        instances.add(this);
    }
    @Override
    protected void finalize() throws Throwable {
        instances.remove(this);
        super.finalize();
    }

    public void refresh() {
        lastInstanceReference = new WeakReference<>(this);
        if (fullAppList == null) return;

        submitList(fullAppList);
    }

    protected void setFullItems(List<ApplicationInfo> items) {
        if (items != null && fullAppList != null && items.size() == fullAppList.size()) {
            Set<String> pna = items.stream().map(x -> x.packageName).collect(Collectors.toSet());
            Set<String> pnb = fullAppList.stream().map(x -> x.packageName).collect(Collectors.toSet());
            if (pna.equals(pnb)) return;
        }

        fullAppList = items;
        refresh();
    }

    public synchronized void filterBy(String text) {
        if (text.isBlank()) {
            setFullItems(fullAppList);
        } else {
            List<ApplicationInfo> filteredList = fullAppList.stream()
                    .filter(item -> App.getLabel(item).contains(text.strip()))
                    .collect(Collectors.toList());
            submitList(filteredList);
        }
    }

    protected static class AppViewHolder extends RecyclerView.ViewHolder {
        public ViewGroup container;
        public View view;
        public ImageView imageView;
        public TextView textView;
        public ApplicationInfo app;
        @Nullable
        public Boolean banner = null;
        @Nullable
        public Boolean darkMode = null;
        @Nullable Boolean showName = true;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        private final LinkedList<Runnable> onReadyQueue = new LinkedList<>();
        public void whenReady(Runnable runnable) {
            if (view == null) onReadyQueue.add(runnable);
            else runnable.run();
        }

        protected void onReady() {
            while (!onReadyQueue.isEmpty()) onReadyQueue.pop().run();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AppViewHolder that)) return false;
            if (app == null || that.app == null) return false;
            return Objects.equals(app.packageName, that.app.packageName);
        }

        @Override
        public int hashCode() {
            return app == null ? super.hashCode() : Objects.hashCode(app.packageName);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ViewGroup container = new LcContainerView(parent.getContext());
        VH holder = newViewHolder(container);
        holder.container = container;

        AsyncLayoutInflater inflater = new AsyncLayoutInflater(parent.getContext());


        inflater.inflate(itemLayoutResId, parent,
                (view, resId, parent1) -> {
                    holder.view = view;
                    holder.imageView = view.findViewById(R.id.itemImage);

                    holder.textView = view.findViewById(R.id.itemLabel);

                    setupViewHolder(holder);

                    holder.container.post(holder::onReady);
                });

        return holder;
    }

    protected VH newViewHolder(View itemView) {
        //noinspection unchecked
        return (VH) new AppViewHolder(itemView);
    }

    protected void setupViewHolder(VH holder) {}

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ApplicationInfo app = getItem(position);
        holder.app = app;
        holder.whenReady(() -> executorService.submit(() -> {

            final Boolean darkMode = LauncherActivity.darkMode;
            if (!darkMode.equals(holder.darkMode)) {
                holder.textView.setTextColor(darkMode ? Color.WHITE : Color.BLACK);
                holder.textView.setShadowLayer(6, 0, 0,
                        LauncherActivity.darkMode ? Color.BLACK : Color.WHITE);
                holder.darkMode = darkMode;
            }

            //noinspection WrapperTypeMayBePrimitive
            final Boolean banner = App.isBanner(app);
            if (banner != holder.banner) {
                if (holder.imageView.getLayoutParams() instanceof ConstraintLayout.LayoutParams clp) {
                    clp.dimensionRatio = banner ? "16:9" : "1:1";
                    holder.banner = banner;
                }
            }
            //noinspection WrapperTypeMayBePrimitive
            final Boolean showName = banner
                    ? LauncherActivity.namesBanner : LauncherActivity.namesSquare;
            if (showName != holder.showName) {
                holder.textView.setVisibility(showName ? View.VISIBLE : View.GONE);
                holder.showName = showName;
            }

            //Load Icon
            IconLoader.loadIcon(holder.app, drawable -> {
                if (holder.app == app) onIconChanged(holder, drawable);
                else {
                    holder.imageView.post(() ->
                            AppsAdapter.runOnEachInstance(a -> a.notifyItemChanged(position)));
                }
            });

            // Load label
            App.getLabel(app, label
                    -> holder.container.post(() -> {
                if (holder.app == app) holder.textView.setText(label);
                else AppsAdapter.runOnEachInstance(a -> a.notifyItemChanged(position));
            }));

            holder.container.post(() -> holder.container.addView(holder.view));

            onViewHolderReady(holder);
        }));
    }

    protected void onViewHolderReady(VH holder) {}

    private static final ExecutorService executorService = Executors.newWorkStealingPool();
    protected void onIconChanged(VH holder, Drawable icon) {
        // Set the actual image with proper scaling
        if (icon instanceof BitmapDrawable bitmapIcon) {

            executorService.submit(() -> {
                Bitmap bitmap = bitmapIcon.getBitmap();
                holder.imageView.post(() -> holder.imageView.setImageBitmap(bitmap));
            });
        } else {
            holder.imageView.post(() -> holder.imageView.setImageDrawable(icon));
        }
    }

    public void notifyAllChanged() {
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).packageName.hashCode();
    }

    @Override
    public ApplicationInfo getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public int getItemViewType(int position) {
        return App.isBanner(getItem(position)) ? 2 : 1;
    }

    public void notifyItemChanged(ApplicationInfo ai) {
        notifyItemChanged(indexOf(ai));
    }

    public int indexOf(ApplicationInfo ai) {
        return getCurrentList().indexOf(ai);
    }
}