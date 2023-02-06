package com.kokoconnect.android.model.event

import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.model.vod.Collection

class BrowseEvent(var data: BrowseEventData) : AmsEvent("browse_event")

class BrowseEventData (
    var content_name: String?,
    var current_url: String?,
    var content_description: String?,
    var parent_url: String?,
    var duration: Int?
) : EventData()


class BrowseEventDescription {
    var description: String = ""
    private set
    var collectionName: String = ""
    private set

    constructor(content: Content?, collection: Collection?) {
        when(content) {
            is Movie -> {
                setMovie(content, collection)
            }
            is Series -> {
                setSeries(content, collection)
            }
            is Episode -> {
                setEpisode(content, collection)
            }
        }
    }

    fun setSeries(series: Series?, collection: Collection?) {
        val collectionName: String = collection?.name ?: ""
        val seriesName: String = series?.name ?: ""
        description = "${seriesName}"
        this.collectionName = collectionName
    }

    fun setMovie(movie: Movie?, collection: Collection?) {
        val collectionName: String = collection?.name ?: ""
        val movieName: String = movie?.name ?: ""
        description = "${movieName}"
        this.collectionName = collectionName
    }

    fun setEpisode(episode: Episode?, collection: Collection?) {
        val collectionName: String = collection?.name ?: ""
        val seriesName: String = episode?.season?.series?.name?:""
        val episodeName: String = episode?.name?:""
        val seasonEpisode: String = "${episode?.getSeasonNumber()}:${episode?.getEpisodeNumber()}"
        description = "${seriesName}/${episodeName}/${seasonEpisode}"
        this.collectionName = collectionName
    }
}