package com.google.firebase.codelab.nthuchat

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet

//fix blinking when load fragment
class CustomLinearLayoutManager : LinearLayoutManager {

    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(context, orientation, reverseLayout) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {}

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        //override this method and implement code as below
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}