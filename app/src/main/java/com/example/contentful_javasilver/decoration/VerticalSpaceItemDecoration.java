package com.example.contentful_javasilver.decoration;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {

    private final int verticalSpaceHeight;

    public VerticalSpaceItemDecoration(int verticalSpaceHeight) {
        this.verticalSpaceHeight = verticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        // Add top margin only for the first item to avoid double space between items
        // parent.getChildAdapterPosition(view) == 0 -> outRect.top = verticalSpaceHeight;
        
        // Add bottom margin to all items except the last one
        if (parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1) {
             outRect.bottom = verticalSpaceHeight;
        }
        // Add top margin to all items (alternative approach)
        // outRect.top = verticalSpaceHeight;
    }
} 