package com.krittish.dailytracker.ui.common;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/** Swipe a row to the left to delete it (the screen then shows a Snackbar with Undo). */
public final class SwipeToDelete {
    private SwipeToDelete() {}

    public interface OnSwiped { void onSwiped(int position); }

    public static void attach(RecyclerView rv, OnSwiped callback) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder a, @NonNull RecyclerView.ViewHolder b) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) callback.onSwiped(pos);
            }
        }).attachToRecyclerView(rv);
    }
}
