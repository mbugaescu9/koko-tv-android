package com.kokoconnect.android.adapter.vod

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.model.vod.Collection
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.util.diffutli.CollectionsRowDiffUtilCallback
import com.kokoconnect.android.vh.BannerViewHolder
import com.kokoconnect.android.vh.LoadingProgressViewHolder
import com.kokoconnect.android.vh.vod.CollectionDataViewHolder
import com.kokoconnect.android.vh.vod.CollectionsListHorizontalViewHolder
import com.kokoconnect.android.vh.vod.CollectionsViewHolderBuilder
import timber.log.Timber

class CollectionsPagedAdapter(
    var adsObjectsProvider: AdsObjectsProvider?,
    var airyRepo: AiryRepository,
    var listener: Listener? = null
) : PagingDataAdapter<CollectionsRow, RecyclerView.ViewHolder>(CollectionsRowDiffUtilCallback()) {
    companion object {
        const val POSTER_VERT_WIDTH = 360
        const val POSTER_VERT_HEIGHT = 540
        const val POSTER_HORZ_WIDTH = 540
        const val POSTER_HORZ_HEIGHT = 360
    }


    var bannersEnabled = true
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var progressVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyItemChanged(itemCount-1)
            }
        }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.rowType?.ordinal ?: CollectionsRowTypes.COLLECTION_GRID.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val rowType = CollectionsRowTypes.values().find { it.ordinal == viewType } ?: CollectionsRowTypes.COLLECTION_GRID
        Timber.d("CollectionsPagedAdapter: onCreateViewHolder() rowType == $rowType")
        val viewHolder = CollectionsViewHolderBuilder.build(parent, rowType, adsObjectsProvider)
        if (viewHolder is CollectionsListHorizontalViewHolder) {
            viewHolder.airyRepo = airyRepo
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position in (0 until itemCount)) {
            getItem(position)?.let {
                when {
                    holder is CollectionDataViewHolder && it is CollectionRow -> {
                        holder.listener = listener
                        holder.bind(it, position)
                    }
                    holder is LoadingProgressViewHolder -> {
                        holder.setVisible(progressVisible)
                    }
                    holder is BannerViewHolder && it is CollectionBannerRow -> {
                        holder.setVisible(bannersEnabled)
                    }
                    else -> { }
                }
            }
        }
    }

    interface Listener {
        fun onShowMore(collection: Collection, position: Int)
        fun onContentClick(content: Content, collection: Collection, position: Int)
    }
}