package com.kokoconnect.android.adapter.vod

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.vh.BannerViewHolder
import com.kokoconnect.android.vh.LoadingProgressViewHolder
import com.kokoconnect.android.vh.vod.ContentDataListHorizontalViewHolder
import com.kokoconnect.android.vh.vod.ContentViewHolderBuilder
import com.kokoconnect.android.vh.vod.OnContentClickListener

class ContentListHorizontalAdapter() : PagingDataAdapter<ContentDataListHorizontalRow, ContentDataListHorizontalViewHolder>(REPO_COMPARATOR) {

    companion object {
        private val REPO_COMPARATOR = object : DiffUtil.ItemCallback<ContentDataListHorizontalRow>() {
            override fun areItemsTheSame(oldItem: ContentDataListHorizontalRow, newItem: ContentDataListHorizontalRow): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: ContentDataListHorizontalRow, newItem: ContentDataListHorizontalRow): Boolean =
                oldItem.content.id == newItem.content.id
        }
    }

    var contentClickListener: OnContentClickListener? = null

    override fun onBindViewHolder(holder: ContentDataListHorizontalViewHolder, position: Int) {
        getItem(position)?.let { item ->
            holder.bind(item)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContentDataListHorizontalViewHolder {
        val viewHolder = ContentViewHolderBuilder.build(
            parent,
            ContentRowTypes.CONTENT_LIST_HORIZONTAL
        ) as ContentDataListHorizontalViewHolder
        viewHolder.onContentClickListener = contentClickListener
        return viewHolder
    }

}

class ContentListHorizontalAdapterLegacy(
    val adsObjectsProvider: AdsObjectsProvider?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//    companion object {
//        const val POSTER_VERT_WIDTH = 200
//        const val POSTER_VERT_HEIGHT = 600
//        const val POSTER_HORZ_WIDTH = 600
//        const val POSTER_HORZ_HEIGHT = 450
//    }

    var contentClickListener: OnContentClickListener? = null

    var bannersEnabled: Boolean = true
        set(value) {
            if (field != value) {
                notifyDataSetChanged()
            }
            field = value
        }

    var items: MutableList<ContentRow> = mutableListOf(ContentLoadingProgressRow())
        set(value) {
            value.add(ContentLoadingProgressRow())
            field = value
            notifyDataSetChanged()
        }
    var itemCountToShow: Int = ContentGridAdapter.MAX_ITEMS
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

    override fun getItemViewType(position: Int): Int {
        return items.getOrNull(position)?.rowType?.ordinal
            ?: ContentRowTypes.CONTENT_LIST_HORIZONTAL.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val rowType = ContentRowTypes.values().find { it.ordinal == viewType }
            ?: ContentRowTypes.CONTENT_LIST_HORIZONTAL
        val viewHolder = ContentViewHolderBuilder.build(parent, rowType)
        when {
            viewHolder is ContentDataListHorizontalViewHolder -> {
                viewHolder.onContentClickListener = contentClickListener
            }
            viewHolder is BannerViewHolder -> {
                viewHolder.adsObjectsProvider = adsObjectsProvider
            }
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        items.getOrNull(position)?.let {
            when {
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
}