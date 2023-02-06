package com.kokoconnect.android.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TimelineRecyclerView: RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun scrollTo(x: Int, y: Int) {
        scrollBy(x - currentScrollX, 0);
    }

    var isTouchable: Boolean = true
    private var currentScrollX: Int= 0

    private val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            updateChildVisibleArea()
        }
    }

    private fun updateChildVisibleArea() {
        for (i in 0 until childCount) {
            val child = getChildAt(i) as ShiftableOnScroll
            child.updateVisibleArea()
        }
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        return if (isTouchable) super.onTouchEvent(e) else false
    }

    override fun onScrolled(dx: Int, dy: Int) {
        viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        super.onScrolled(dx, dy)
        currentScrollX += dx
        updateChildVisibleArea()
    }

    fun resetScroll(position: Int, offset: Int) {
//        println("resetScroll position = [${position}], offset = [${offset}]")
        //viewTreeObserver is null when fragment containing this view is destroyed
        viewTreeObserver ?: return
        (layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(position, -offset)
        // Workaround to b/31598505. When a program's duration is too long,
        // RecyclerView.onScrolled() will not be called after scrollToPositionWithOffset().
        // Therefore we have to update children's visible areas by ourselves in this case.
        // Since scrollToPositionWithOffset() will call requestLayout(), we can listen to this
        // behavior to ensure program items' visible areas are correctly updated after layouts
        // are adjusted, i.e., scrolling is over.
        viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

}

interface ShiftableOnScroll {
    fun updateVisibleArea()
}