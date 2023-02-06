package com.kokoconnect.android.adapter.vod

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.util.diffutli.SeriesRowDiffUtilCallback
import com.kokoconnect.android.vh.BannerViewHolder
import com.kokoconnect.android.vh.LoadingProgressViewHolder
import com.kokoconnect.android.vh.vod.SeriesEpisodeViewHolder
import com.kokoconnect.android.vh.vod.SeriesHeaderViewHolder
import com.kokoconnect.android.vh.vod.SeriesViewHolderBuilder


class SeriesAdapter(
    val adsObjectsProvider: AdsObjectsProvider?
) : PagingDataAdapter<SeriesRow, RecyclerView.ViewHolder>(SeriesRowDiffUtilCallback()) {

    companion object {
        const val POSTER_VERT_WIDTH = 450
        const val POSTER_VERT_HEIGHT = 600
        const val POSTER_HORZ_WIDTH = 600
        const val POSTER_HORZ_HEIGHT = 450
    }

    var listener: Listener? = null

    var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (this.recyclerView == recyclerView) {
            this.recyclerView = null
        }
        super.onDetachedFromRecyclerView(recyclerView)
    }

    var headerEnabled: Boolean = true
        set(value) {
            field = value
            if (itemCount > 0) {
                notifyDataSetChanged()
            }
        }

    var bannersEnabled: Boolean = true
        set(value) {
            field = value
            if (itemCount > 0) {
                notifyDataSetChanged()
            }
        }

    var progressVisible: Boolean = true
        set(value) {
            field = value
            notifyItemChanged(itemCount - 1)
        }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.rowType?.ordinal ?: SeriesRowTypes.EPISODE.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val rowType = SeriesRowTypes.values().find { it.ordinal == viewType }
        val viewHolder = SeriesViewHolderBuilder.build(parent, rowType, adsObjectsProvider)
        if (viewHolder is SeriesEpisodeViewHolder) {
            viewHolder.seriesListener = listener
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position !in (0 until itemCount)) return
        getItem(position)?.let {
            when {
                holder is SeriesHeaderViewHolder && it is SeriesHeaderRow -> {
                    holder.bind(it, position, headerEnabled)
                }
                holder is SeriesEpisodeViewHolder && it is SeriesEpisodeRow -> {
                    holder.bind(it, position)
                }
                holder is LoadingProgressViewHolder -> {
                    holder.setVisible(progressVisible)
                }
                holder is BannerViewHolder && it is SeriesBannerRow -> {
                    holder.setVisible(bannersEnabled)
                }
                else -> {
                }
            }
        }
    }

    interface Listener {
        fun onEpisodeClick(episode: Episode?, position: Int)
    }
}
