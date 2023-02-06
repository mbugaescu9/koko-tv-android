package com.kokoconnect.android.util.diffutli

import androidx.recyclerview.widget.DiffUtil
import com.kokoconnect.android.model.vod.*

class SeriesRowDiffUtilCallback() : DiffUtil.ItemCallback<SeriesRow>() {
    override fun areItemsTheSame(oldItem: SeriesRow, newItem: SeriesRow): Boolean {
        return when {
            oldItem is SeriesHeaderRow && newItem is SeriesHeaderRow -> {
                oldItem.series?.id == newItem.series?.id
            }
            oldItem is SeriesEpisodeRow && newItem is SeriesEpisodeRow -> {
                oldItem.episode?.id == newItem.episode?.id
            }
            oldItem is SeriesBannerRow && newItem is SeriesBannerRow -> {
                true
            }
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: SeriesRow, newItem: SeriesRow): Boolean {
        return when {
            oldItem is SeriesHeaderRow && newItem is SeriesHeaderRow -> {
                oldItem.series?.name == newItem.series?.name
                        && oldItem.series?.description == newItem.series?.description
                        && oldItem.series?.poster?.getUrl() == newItem.series?.poster?.getUrl()
            }
            oldItem is SeriesEpisodeRow && newItem is SeriesEpisodeRow -> {
                oldItem.episode?.number == newItem.episode?.number
                        && oldItem.episode?.name == newItem.episode?.name
                        && oldItem.episode?.season?.number == newItem.episode?.season?.number
                        && oldItem.episode?.publishedAt == newItem.episode?.publishedAt
                        && oldItem.episode?.poster?.getUrl() == newItem.episode?.poster?.getUrl()
                        && oldItem.episode?.duration == newItem.episode?.duration
            }
            oldItem is SeriesBannerRow && newItem is SeriesBannerRow -> {
                false
            }
            else -> false
        }
    }

}