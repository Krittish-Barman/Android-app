package com.krittish.dailytracker.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

/**
 * One reusable ListAdapter + DiffUtil for every simple list in the app.
 * You give it an item layout, a way to get a stable id, and a function that fills the row.
 */
public class GenericAdapter<T> extends ListAdapter<T, GenericAdapter.VH> {

    public interface Ids<T> { long id(T item); }
    public interface Binder<T> { void bind(View itemView, T item); }

    public static class VH extends RecyclerView.ViewHolder {
        public VH(@NonNull View v) { super(v); }
    }

    private final int layout;
    private final Binder<T> binder;

    public GenericAdapter(int layout, Ids<T> ids, Binder<T> binder) {
        super(new DiffUtil.ItemCallback<T>() {
            @Override public boolean areItemsTheSame(@NonNull T a, @NonNull T b) { return ids.id(a) == ids.id(b); }
            @Override public boolean areContentsTheSame(@NonNull T a, @NonNull T b) { return false; }   // always re-bind
        });
        this.layout = layout;
        this.binder = binder;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        binder.bind(holder.itemView, getItem(position));
    }

    public T itemAt(int position) { return getItem(position); }

    /** Sets layout manager + adapter and turns off the cross-fade that would flicker on every update. */
    public GenericAdapter<T> attachTo(RecyclerView rv) {
        rv.setLayoutManager(new LinearLayoutManager(rv.getContext()));
        rv.setAdapter(this);
        RecyclerView.ItemAnimator a = rv.getItemAnimator();
        if (a instanceof SimpleItemAnimator) ((SimpleItemAnimator) a).setSupportsChangeAnimations(false);
        return this;
    }
}
