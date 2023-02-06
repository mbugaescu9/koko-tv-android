package com.kokoconnect.android.model.response

import com.kokoconnect.android.model.vod.Episode
import com.kokoconnect.android.model.vod.Movie
import com.kokoconnect.android.model.vod.Season
import com.kokoconnect.android.model.vod.Series

class ContentResponse {
    var season: Season? = null
    var seasons: List<Season>? = null
    var series: Series? = null
    var movie: Movie? = null
    var episode: Episode? = null

}