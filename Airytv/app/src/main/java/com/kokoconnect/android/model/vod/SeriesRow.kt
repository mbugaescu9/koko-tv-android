package com.kokoconnect.android.model.vod

import com.kokoconnect.android.R
import com.kokoconnect.android.model.ads.banner.BannerManager

abstract class SeriesRow(val rowType: SeriesRowTypes, var visible: Boolean = true)

class SeriesHeaderRow(
    val series: Series?
) : SeriesRow(SeriesRowTypes.HEADER)

class SeriesEpisodeRow(
    val episode: Episode?
) : SeriesRow(SeriesRowTypes.EPISODE)

class SeriesBannerRow(
    val bannerManager: BannerManager? = null
) : SeriesRow(SeriesRowTypes.BANNER_AD) {}

class SeriesLoadingProgressRow: SeriesRow(SeriesRowTypes.PROGRESS)

enum class SeriesRowTypes(var layoutId: Int) {
    HEADER(R.layout.item_series_header),
    EPISODE(R.layout.item_series_episode),
    BANNER_AD(0),
    PROGRESS(R.layout.item_loading_progress)
}
