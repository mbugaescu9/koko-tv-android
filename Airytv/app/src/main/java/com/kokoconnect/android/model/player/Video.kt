package com.kokoconnect.android.model.player

import com.kokoconnect.android.util.ContentUrlPreparer
import timber.log.Timber


abstract class PlayerObject {
    abstract fun getUrl(): String?
    abstract fun setUrl(url: String?)
    abstract fun getPlayerType(): PlayerType
    abstract fun isStream(): Boolean
    abstract fun isAd(): Boolean
    abstract fun getStartPosition(): Long
    abstract fun setStartPosition(position: Long)
    abstract fun getStartTimeMs(): Long

    fun prepareUrl(contentUrlPreparer: ContentUrlPreparer) {
        val contentUrl = getUrl()
        val preparedUrl = contentUrlPreparer.prepareContentUrl(contentUrl)
        setUrl(preparedUrl)
        Timber.d("prepareUrl() contentUrl = ${contentUrl} preparedUrl = ${preparedUrl}")
    }
}

interface Video {}

interface Stream {}

abstract class VideoAdLoader : PlayerObject() {
    override fun getPlayerType(): PlayerType = PlayerType.AD
    override fun isStream(): Boolean = false
    override fun isAd(): Boolean = true
    override fun getStartPosition(): Long = -1
    override fun setStartPosition(position: Long) {
        // ignore
    }
    override fun getStartTimeMs(): Long = -1L
}

abstract class YouTube : PlayerObject(), Video {
    fun getCue() = getUrl()?.replace("https://www.youtube.com/watch?v=", "")
    override fun getPlayerType(): PlayerType = PlayerType.YOUTUBE
    override fun isAd(): Boolean = false
}

class YouTubeVideo(
    var videoUrl: String?,
    var seekToTime: Long,
    var startTimeMsEpoch: Long
) : YouTube(), Video {
    override fun getUrl(): String? = videoUrl
    override fun setUrl(url: String?) {
        videoUrl = url
    }
    override fun isStream(): Boolean = false
    override fun getStartPosition(): Long = seekToTime
    override fun setStartPosition(position: Long) {
        seekToTime = position
    }
    override fun getStartTimeMs(): Long = startTimeMsEpoch
}

class YouTubeStream(var videoUrl: String?, var startTimeMsEpoch: Long) : YouTube(), Stream {
    override fun getUrl(): String? = videoUrl
    override fun setUrl(url: String?) {
        videoUrl = url
    }
    override fun getStartTimeMs(): Long = startTimeMsEpoch

    override fun isStream(): Boolean = true
    override fun getStartPosition(): Long = -1
    override fun setStartPosition(position: Long) {
        // ignore
    }
}

abstract class Exoplayer : PlayerObject() {
    override fun getPlayerType(): PlayerType = PlayerType.EXOPLAYER
    override fun isAd(): Boolean = false
}

class HlsStream(var videoUrl: String?, var startTimeMsEpoch: Long) : Exoplayer(), Stream {
    override fun getUrl(): String? = videoUrl
    override fun setUrl(url: String?) {
        videoUrl = url
    }
    override fun getStartTimeMs(): Long = startTimeMsEpoch

    override fun isStream(): Boolean = true
    override fun getStartPosition(): Long = -1
    override fun setStartPosition(position: Long) {
        // ignore
    }
}

class Mpeg4Video(
    var videoUrl: String?,
    var seekToTime: Long,
    var startTimeMsEpoch: Long,
    val isSecured: Boolean = false
) : Exoplayer(), Video {
    override fun getUrl(): String? = videoUrl
    override fun setUrl(url: String?) {
        videoUrl = url
    }
    override fun isStream(): Boolean = false
    override fun getStartPosition(): Long = seekToTime
    override fun setStartPosition(position: Long) {
        seekToTime = position
    }

    override fun getStartTimeMs(): Long = startTimeMsEpoch
}

class DailymotionVideo(
    var videoUrl: String?,
    var seekToTime: Long,
    var startTimeMsEpoch: Long
) : PlayerObject(), Video {
    fun getCue() = getUrl()?.replace("https://www.dailymotion.com/video/", "")
    override fun getUrl(): String? = videoUrl
    override fun setUrl(url: String?) {
        videoUrl = url
    }
    override fun isStream(): Boolean = false
    override fun isAd(): Boolean = false
    override fun getPlayerType(): PlayerType = PlayerType.DAILYMOTION
    override fun getStartPosition(): Long = seekToTime
    override fun setStartPosition(position: Long) {
        seekToTime = position
    }
    override fun getStartTimeMs(): Long = startTimeMsEpoch
}

class WebVideo(
    var videoUrl: String?,
    var startTimeMsEpoch: Long
) : PlayerObject(), Video {
    override fun getUrl(): String? = videoUrl
    override fun setUrl(url: String?) {
        videoUrl = url
    }
    override fun getPlayerType(): PlayerType = PlayerType.WEB
    override fun isStream(): Boolean = false
    override fun isAd(): Boolean = false
    override fun getStartPosition(): Long = 0L
    override fun setStartPosition(position: Long) {}
    override fun getStartTimeMs(): Long = startTimeMsEpoch
}

data class SecurityData(
    var authKey: ArchiveApiKey = ArchiveApiKey(),
    var cookie: String = ""
)

data class ArchiveApiKey(var header: String = "")