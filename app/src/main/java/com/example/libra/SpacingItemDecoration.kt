package com.example.libra

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // We're using the margin in the CardView, so we don't need additional spacing
        // This class exists in case we want to adjust spacing dynamically in the future
    }
}