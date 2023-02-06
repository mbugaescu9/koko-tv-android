package com.kokoconnect.android.vh.vod

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.vod.*
import com.kokoconnect.android.databinding.ItemLoadingProgressBinding
import com.kokoconnect.android.databinding.ItemSeriesEpisodeBinding
import com.kokoconnect.android.databinding.ItemSeriesHeaderBinding
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.util.*
import com.kokoconnect.android.vh.BannerViewHolder
import com.kokoconnect.android.vh.LoadingProgressViewHolder
import com.bumptech.glide.Glide

import timber.log.Timber

object SeriesViewHolderBuilder {
    fun build(
        parent: ViewGroup,
        rowType: SeriesRowTypes?,
        adsObjectsProvider: AdsObjectsProvider? = null
    ): RecyclerView.ViewHolder {
        return when (rowType) {
            SeriesRowTypes.BANNER_AD -> buildBannerRow(rowType, parent, adsObjectsProvider)
            else -> buildSeriesRow(rowType, parent)  //CollectionBannerRow
        }
    }

    private fun buildSeriesRow(
        rowType: SeriesRowTypes?,
        parent: ViewGroup
    ): RecyclerView.ViewHolder {
        val layoutId = rowType?.layoutId ?: CollectionsRowTypes.COLLECTION_GRID.layoutId
        val view =
            LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return when (rowType) {
            SeriesRowTypes.HEADER -> {
                val binding = ItemSeriesHeaderBinding.bind(view)
                SeriesHeaderViewHolder(binding)
            }
            SeriesRowTypes.EPISODE -> {
                val binding = ItemSeriesEpisodeBinding.bind(view)
                SeriesEpisodeViewHolder(binding)
            }
            else -> {
                val binding = ItemLoadingProgressBinding.bind(view)
                LoadingProgressViewHolder(binding)
            }
        }
    }

    private fun buildBannerRow(
        rowType: SeriesRowTypes,
        parent: ViewGroup,
        adsObjectsProvider: AdsObjectsProvider? = null
    ): BannerViewHolder {
        return BannerViewHolder.buildFor(parent, adsObjectsProvider = adsObjectsProvider)
    }
}

open class SeriesViewHolder(view: View) : RecyclerView.ViewHolder(view) {}

class SeriesHeaderViewHolder(val binding: ItemSeriesHeaderBinding) : SeriesViewHolder(binding.root) {
    fun bind(row: SeriesHeaderRow?, position: Int, enabled: Boolean) {
        if (enabled) {
            binding.root.visibility = View.VISIBLE
            binding.root.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } else {
            binding.root.visibility = View.GONE
            binding.root.layoutParams = RecyclerView.LayoutParams(0, 0)
        }
        val context = binding.root.context
        val series = row?.series
        val posterUrl = series?.poster?.getUrl()

        Glide.with(context)
            .load(posterUrl)
            .placeholder(R.drawable.shape_poster_vertical_placeholder)
            .override(SeriesAdapter.POSTER_VERT_WIDTH, SeriesAdapter.POSTER_VERT_HEIGHT)
            .into(binding.ivPoster)
        binding.tvPosterSource.visibility = View.GONE
//        val domainName = NetworkUtils.getDomainName(posterUrl)
//        if (domainName != null && domainName.isNotEmpty()) {
//            binding.tvPosterSource.visibility = View.VISIBLE
//            binding.tvPosterSource.setText(domainName ?: "")
//        }
        binding.tvDescription.setText(series?.description ?: "")
    }
}

class SeriesEpisodeViewHolder(val binding: ItemSeriesEpisodeBinding) : SeriesViewHolder(binding.root) {

    var seriesListener: SeriesAdapter.Listener? = null

    fun bind(row: SeriesEpisodeRow?, position: Int) {
        val context = binding.root
        val episode = row?.episode
        val number =
            String.format("S%02d EP%02d", episode?.season?.number ?: 1, episode?.number ?: 1)
        Timber.d("bind() ${number} ")
        binding.tvEpisodeNumber.setText(number)
        val description =
            (episode?.publishedAt ?: "") + " " + DateUtils.formatDuration(episode?.duration ?: 0)
        binding.tvEpisodeDescription.setText(description)
        binding.tvEpisodeName.setText(episode?.name ?: "")
        val posterUrl = episode?.poster?.getUrl()
        Glide.with(context)
            .load(posterUrl)
            .placeholder(R.drawable.shape_poster_horizontal_placeholder)
            .override(SeriesAdapter.POSTER_HORZ_WIDTH, SeriesAdapter.POSTER_HORZ_HEIGHT)
            .into(binding.ivPoster)
        binding.tvPosterSource.visibility = View.GONE
//        val domainName = NetworkUtils.getDomainName(posterUrl)
//        if (domainName != null && domainName.isNotEmpty()) {
//            binding.tvPosterSource.visibility = View.VISIBLE
//            binding.tvPosterSource.setText(domainName ?: "")
//        }
        binding.rootLayout.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                seriesListener?.onEpisodeClick(episode, position)
            }
        }
    }
}
