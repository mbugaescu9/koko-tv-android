package com.kokoconnect.android.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import org.jetbrains.anko.displayMetrics

class PagerRecyclerView : RecyclerView {
    var currentPagePosition: Int = RecyclerView.NO_POSITION
    private val snapHelper = LinearSnapHelper() //PagerSnapHelper()
    private var itemMargin: Float = 0f
    private var onPageChangedListener: OnPageChangedListener? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs){
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val displayMetrics = context.displayMetrics
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PagerRecyclerView, 0, 0)
            val value = typedArray.getDimension(R.styleable.PagerRecyclerView_itemMargin, 0f)
            itemMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics)
            typedArray.recycle()
        }

        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        snapHelper.attachToRecyclerView(this)
    }

    fun setOnPageChangedListener(listener: OnPageChangedListener) {
        onPageChangedListener = listener
    }

    fun snapToPosition() {
        layoutManager
    }

    override fun onScrolled(dx: Int, dy: Int) {
        val snapPosition = getSnapPosition()
        val snapPositionChanged = this.currentPagePosition != snapPosition
        if (snapPositionChanged) {
            onPageChangedListener?.invoke(snapPosition)
            this.currentPagePosition = snapPosition
        }
        super.onScrolled(dx, dy)
    }

    private fun getSnapPosition(): Int {
        val layoutManager = layoutManager ?: return RecyclerView.NO_POSITION
        val snapView = snapHelper.findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
        return layoutManager.getPosition(snapView)
    }

    fun setPagerAdapter(adapter: Adapter<ViewHolder>) {
        val pagerAdapter = adapter as? PagerRecyclerViewAdapter

        if (pagerAdapter != null) {
            pagerAdapter.setItemMargin(itemMargin.toInt())
            pagerAdapter.updateDisplayMetrics()
        } else {
            throw IllegalArgumentException("Only PagerRecyclerViewAdapter is allowed here")
        }
        super.setAdapter(adapter)
    }

    abstract class PagerRecyclerViewAdapter<VH : PagerViewHolder> : RecyclerView.Adapter<VH> {

        private var metrics: DisplayMetrics
        private var itemMargin = 0
        private var itemWidth = 0

        constructor(metrics: DisplayMetrics) {
            this.metrics = metrics
        }

        fun setItemMargin(itemMargin: Int) {
            this.itemMargin = itemMargin
        }

        fun updateDisplayMetrics() {
            itemWidth = metrics.widthPixels - itemMargin * 2
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            var currentItemWidth = itemWidth

            if (position == 0) {
                currentItemWidth += itemMargin
                holder.rootLayout.setPadding(itemMargin, 0, 0, 0)
            } else if (position == itemCount - 1) {
                currentItemWidth += itemMargin
                holder.rootLayout.setPadding(0, 0, itemMargin, 0)
            }

            val height = holder.rootLayout.layoutParams.height
            holder.rootLayout.layoutParams = ViewGroup.LayoutParams(currentItemWidth, height)
        }
    }

    abstract class PagerViewHolder : RecyclerView.ViewHolder {
        val rootLayout: View

        constructor(itemView: View) : super(itemView) {
            rootLayout = itemView
        }
    }
}

typealias OnPageChangedListener = ((pageNumber: Int)->Unit)?