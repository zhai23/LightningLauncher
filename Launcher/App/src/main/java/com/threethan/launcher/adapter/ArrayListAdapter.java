package com.threethan.launcher.adapter;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** @noinspection unused*/
public abstract class ArrayListAdapter<T, H extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<H> {
    protected List<T> items = Collections.synchronizedList(new ArrayList<>());
    public void setItems(List<T> newItems) {
//        for(T item : new ArrayList<>(items)) {
//            if (newItems.contains(item)) {
//                int i = items.indexOf(item);
//                int j = newItems.indexOf(item);
//                notifyItemMoved(i, j);
//            }
//        }
        for(T item : new ArrayList<>(items)) {
            if (!newItems.contains(item)) {
                int i = items.indexOf(item);
                items.remove(i);
                notifyItemRemoved(i);
            }
        }
        for(T item : newItems) {
            if (!items.contains(item)) {
                int i = newItems.indexOf(item);
                items.add(i, item);
                notifyItemInserted(i);
            }
        }
    }
    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItem(T item) {
        notifyItemChanged(items.indexOf(item));
    }
    public void removeItem(T item) {
        int index = items.indexOf(item);
        items.remove(item);
        notifyItemRemoved(index);
    }
    public void addItem(T item) {
        items.add(item);
        notifyItemInserted(items.indexOf(item));
    }
}
