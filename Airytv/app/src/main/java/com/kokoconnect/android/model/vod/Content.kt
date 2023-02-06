package com.kokoconnect.android.model.vod

import com.kokoconnect.android.model.player.*
import com.kokoconnect.android.util.DOMAIN_ARCHIVE
import com.kokoconnect.android.util.DateUtils
import com.kokoconnect.android.util.NetworkUtils
import com.kokoconnect.android.util.RuntimeTypeAdapterFactory
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.annotations.SerializedName


open class Content {
    companion object {
        val childClasses = listOf(
            Movie::class.java,
            Series::class.java,
            Season::class.java,
            Episode::class.java
        )
        val TYPE_ADAPTER_FACTORY = RuntimeTypeAdapterFactory.of(Content::class.java, "type")
            .registerSubtype(Movie::class.java, ContentType.movie.type)
            .registerSubtype(Series::class.java, ContentType.series.type)
            .registerSubtype(Season::class.java, ContentType.season.type)
            .registerSubtype(Episode::class.java, ContentType.episode.type)
        val EXCLUSION_STRATEGY: ExclusionStrategy = object : ExclusionStrategy {
            override fun shouldSkipField(field: FieldAttributes): Boolean {
                return when {
                    (field.declaringClass == Content::class.java || childClasses.contains(field.declaringClass))
                            && field.name == "other" -> {
                        true
                    }
                    else -> false
                }
            }

            override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                return false
            }
        }
    }

    var id: Long? = null
    var name: String? = null
    var description: String? = null

    @SerializedName("posters")
    var poster: Poster? = null

    @SerializedName("sourceUrl")
    var sourceUrl: String? = null
    var duration: Int? = null
    var type: String? = null
    var mediaType: String? = null

    fun getPlayerObject(): PlayerObject? {
        val contentUrl = sourceUrl
        val startTimeMs = DateUtils.getCurrentDate()
        val seekToTime = 0L

        return if (contentUrl != null) {
            when (mediaType) {
                ContentMediaType.MP4.type -> {
                    val isSecured = NetworkUtils.isSameDomainName(contentUrl, DOMAIN_ARCHIVE)
                    Mpeg4Video(
                        videoUrl = contentUrl,
                        seekToTime = seekToTime,
                        startTimeMsEpoch = startTimeMs,
                        isSecured = isSecured
                    )
                }
                ContentMediaType.HLS.type -> {
                    HlsStream(
                        videoUrl = contentUrl,
                        startTimeMsEpoch = startTimeMs
                    )
                }
                ContentMediaType.DAILYMOTION.type -> {
                    DailymotionVideo(
                        videoUrl = contentUrl,
                        seekToTime = seekToTime,
                        startTimeMsEpoch = startTimeMs
                    )
                }
                ContentMediaType.YOUTUBE_STREAM.type -> {
                    YouTubeStream(
                        videoUrl = contentUrl,
                        startTimeMsEpoch = startTimeMs
                    )
                }
                ContentMediaType.YOUTUBE.type -> {
                    YouTubeVideo(
                        videoUrl = contentUrl,
                        seekToTime = seekToTime,
                        startTimeMsEpoch = startTimeMs
                    )
                }
                else -> {
                    WebVideo(
                        videoUrl = contentUrl,
                        startTimeMsEpoch = startTimeMs
                    )
                }
            }
        } else {
            null
        }
    }
}

enum class ContentType(val type: String?) {
    movie("movie"),
    series("series"),
    season("season"),
    episode("episode"),
    none(null)
}

enum class ContentMediaType(val type: String?) {
    MP4("mp4"),
    HLS("hls"),
    YOUTUBE("youtube"),
    YOUTUBE_STREAM("youtubeStream"),
    DAILYMOTION("dailymotion"),
    NONE("none")
}

class Movie() : Content() {
    constructor(newName: String, newImageUrl: String) : this() {
        name = newName
        poster = Poster().apply {
            this.setUrl(newImageUrl)
        }
    }
}

class SeriesResponse : Series() {
    val series: Series? = null

    var total: Int = 0
    var count: Int = 0
    var page: Int = 0
    var limit: Int = 0

    @SerializedName("total_pages")
    var totalPages: Int = 0
}

open class Series : Content() {
    var seasons: MutableList<Season> = mutableListOf()

    fun prepare() {
        for (season in seasons) {
            season.series = this
            for (episode in season.episodes) {
                episode.season = season
            }
        }
    }

    fun prepareEpisodes() {
        for (season in seasons) {
            for (episode in season.episodes) {
                episode.season = season
            }
        }
    }

    fun addEpisodesFrom(newSeasons: List<Season>) {
        for (newSeason in newSeasons) {
            val idx = seasons.indexOfFirst { it.id == newSeason.id }
            if (idx in seasons.indices) {
                seasons[idx].addEpisodesFrom(newSeason)
            } else {
                seasons.add(newSeason)
            }
        }
    }

    fun addEpisodesFrom(series: Series) {
        addEpisodesFrom(series.seasons)
    }

    fun getAllEpisodes(): List<Episode> {
        val episodes = mutableListOf<Episode>()
        for (season in seasons.sortedBy { it.number }) {
            episodes.addAll(season.episodes.sortedBy { it.number })
        }
        return seasons.flatMap { it.episodes }
    }

    fun clear() {
        seasons.clear()
    }
}

class Season : Content() {
    var number: Int? = null
    var episodes: MutableList<Episode> = mutableListOf()
    var seriesId: Long? = null
    var series: Series? = null

    fun addEpisodesFrom(season: Season) {
        for (newEpisode in season.episodes) {
            val idx = episodes.indexOfFirst { it.id == newEpisode.id }
            if (idx !in episodes.indices) {
                episodes.add(newEpisode)
            }
        }
    }
}

class Episode : Content() {
    var number: Int? = null
    var season: Season? = null
    var seriesId: Long? = null

    @SerializedName("published_at_iso")
    var publishedAt: String? = null

    fun getSeasonNumber(): String {
        val seasonNumber = season?.number ?: 1
        val seasonsDigitsCount = season?.series?.seasons?.size?.getDigitsCount() ?: 2
        return String.format("S%0${seasonsDigitsCount}d", seasonNumber)
    }

    fun getEpisodeNumber(): String {
        val episodeNumber = number ?: 1
        val episodesDigitsCount = season?.episodes?.size?.getDigitsCount() ?: 2
        return String.format("E%0${episodesDigitsCount}d", episodeNumber)
    }
}

class Poster {
    private var mobile: String? = null
    private var desktop: String? = null
    private var tablet: String? = null

    fun setUrl(posterUrl: String?) {
        mobile = posterUrl
    }

    fun getUrl(): String? {
        //switch posters for tv, tablet and mobile
        return when {
            mobile != null -> mobile
            tablet != null -> tablet
            else -> desktop
        }
    }
}

fun Int.getDigitsCount() = this.toString().length