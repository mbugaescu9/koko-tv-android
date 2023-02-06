package com.kokoconnect.android.adapter.vod

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.ui.fragment.vod.dp
import com.kokoconnect.android.util.diffutli.ContentRowDiffUtilCallback
import com.kokoconnect.android.vh.BannerViewHolder
import com.kokoconnect.android.vh.LoadingProgressViewHolder
import com.kokoconnect.android.vh.vod.ContentDataGridViewHolder
import com.kokoconnect.android.vh.vod.ContentDataListHorizontalViewHolder
import com.kokoconnect.android.vh.vod.ContentViewHolderBuilder
import com.kokoconnect.android.vh.vod.OnContentClickListener

class ContentGridPagedAdapter(
    val adsObjectsProvider: AdsObjectsProvider?
) : PagingDataAdapter<ContentRow, RecyclerView.ViewHolder>(ContentRowDiffUtilCallback()) {
//    companion object {
//        const val POSTER_VERT_WIDTH = 200
//        const val POSTER_VERT_HEIGHT = 600
//        const val POSTER_HORZ_WIDTH = 600
//        const val POSTER_HORZ_HEIGHT = 450
//    }

    companion object {
        const val GRID_SPAN_COUNT = 3
        const val BANNER_SPAN_SIZE = 3
        const val PROGRESS_SPAN_SIZE = 3
        const val DEFAULT_SPAN_SIZE = 1
        const val MAX_ITEMS = Int.MAX_VALUE
        val SPACING_RECT = Rect(4.dp, 6.dp, 4.dp, 6.dp)
    }

    var contentClickListener: OnContentClickListener? = null

    var bannersEnabled: Boolean = true
        set(value) {
            if (field != value) {
                notifyDataSetChanged()
            }
            field = value
        }

    var progressVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyItemChanged(itemCount - 1)
            }
        }
    var spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when (getItem(position)?.rowType) {
                ContentRowTypes.BANNER_AD -> {
                    ContentGridAdapter.BANNER_SPAN_SIZE
                }
                ContentRowTypes.PROGRESS -> {
                    ContentGridAdapter.PROGRESS_SPAN_SIZE
                }
                else -> ContentGridAdapter.DEFAULT_SPAN_SIZE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val rowType =
            ContentRowTypes.values().find { it.ordinal == viewType } ?: ContentRowTypes.CONTENT_GRID
        val viewHolder = ContentViewHolderBuilder.build(parent, rowType, adsObjectsProvider)
        when {
            viewHolder is ContentDataGridViewHolder -> {
                viewHolder.onContentClickListener = contentClickListener
            }
            viewHolder is ContentDataListHorizontalViewHolder -> {
                viewHolder.onContentClickListener = contentClickListener
            }
            viewHolder is BannerViewHolder -> {
                viewHolder.adsObjectsProvider = adsObjectsProvider
            }
        }
        return viewHolder
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.rowType?.ordinal ?: ContentRowTypes.CONTENT_GRID.ordinal
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let {
            when {
                holder is ContentDataGridViewHolder && it is ContentDataGridRow -> {
                    holder.bind(it)
                }
                holder is ContentDataListHorizontalViewHolder && it is ContentDataListHorizontalRow -> {
                    holder.bind(it)
                }
                holder is LoadingProgressViewHolder -> {
                    holder.setVisible(progressVisible)
                }
                holder is BannerViewHolder && it is ContentBannerRow -> {
                    holder.setVisible(bannersEnabled)
                }
            }
        }
    }

    fun getCurrentItems(): List<ContentRow> {
        return snapshot().mapNotNull { it }.toList()
    }

    inner class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Rect,
        private val includeEdge: Boolean,
        private val headerNum: Int
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view) - headerNum // item position
            val items = getCurrentItems()
            val item = items.getOrNull(position)
            val dataItems  = items.filter { it is ContentDataRow }
            if (item is ContentDataGridRow) {
                val dataPosition = dataItems.indexOf(item)
                val column = dataPosition % spanCount // item column
                if (includeEdge) {
                    outRect.left =
                        spacing.left - column * spacing.left / spanCount // spacing - column * ((1f / spanCount) * spacing)
                    outRect.right =
                        (column + 1) * spacing.right / spanCount // (column + 1) * ((1f / spanCount) * spacing)

                    if (dataPosition < spanCount) { // top edge
                        outRect.top = spacing.top
                    }
                    outRect.bottom = spacing.bottom // item bottom
                } else {
                    outRect.left =
                        column * spacing.left / spanCount // column * ((1f / spanCount) * spacing)
                    outRect.right =
                        spacing.right - (column + 1) * spacing.right / spanCount // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                    if (dataPosition >= spanCount) {
                        outRect.top = spacing.top // item top
                    }
                }
            } else {
                outRect.left = 0
                outRect.right = 0
                outRect.top = 0
                outRect.bottom = 0
            }
        }
    }

}