package com.kokoconnect.android.model.event

import com.kokoconnect.android.model.vod.Content
import com.kokoconnect.android.model.vod.Episode
import com.kokoconnect.android.model.vod.Movie
import com.kokoconnect.android.model.vod.Collection

class WatchEvent(var data: WatchEventData) : AmsEvent("watch_event") {
    companion object {
        const val BEGIN = "begin"
        const val END = "end"
    }
}

class WatchEventData (
    var content_id: Long?,
    var content_name: String?,
    var content_type: String?,
    var content_source: String?,

    var channel_num: Int?,
    var channel_name: String?,

    var current_url: String?,
    var duration: Int?
) : EventData() {

}

class WatchEventDescription {
    var description: String = ""
        private set
    var collectionName: String = ""
        private set

    constructor(content: Content?, collection: Collection?) {
        when(content) {
            is Movie -> {
                setMovie(content, collection)
            }
            is Episode -> {
                setEpisode(content, collection)
            }
        }
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