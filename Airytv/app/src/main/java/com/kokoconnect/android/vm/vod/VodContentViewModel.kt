package com.kokoconnect.android.vm.vod

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.model.vod.Collection
import org.joda.time.Duration
import org.joda.time.Instant
import timber.log.Timber
import javax.inject.Inject

interface AmsVodContentInfo {
    fun getCurrentVodContent(): Content?
    fun getLastVodContent(): Content?
    fun getLastWatchDuration(): Int
    fun getCurrentWatchDuration(): Int
}

interface VideoAdVodContentInfo {
    fun getContentName(): String?
    fun getContentType(): String?
}

class VodContentViewModel @Inject constructor(
    val app: AiryTvApp
) : ViewModel(), VideoAdVodContentInfo, AmsVodContentInfo {
    companion object {
        const val FULLSCREEN_CONTROLS_DURATION = 5 * 1000L
    }

    private val ams: AmsEventsFacade
        get() {
            return app.ams
        }
    var previousCollection: Collection? = null
    var currentCollection: Collection? = null

    var currentSeries: Series? = null
    var currentSeason: Season? = null
    var lastContent: Content? = null
    var currentContent: Content? = null

    var needShowCollection = MutableLiveData<Boolean>()
    var needShowSeries = MutableLiveData<Boolean>()
    var needShowSeason = MutableLiveData<Boolean>()
    var needShowContent = MutableLiveData<Boolean>()

    val needOpenContent = MutableLiveData<Content?>()
    val currentContentLiveData = MutableLiveData<Content?>()

    private var lastPauseInstant: Instant? = null
    private var lastSwitchTime: Instant? = null
    private var previousWatchDuration: Int = 0
    private var lastPausesDuration: Int = 0

    var needSendWatchEvent = MutableLiveData<Boolean?>()
    var needShowFullscreenDescription = MutableLiveData<Boolean?>()
    var fullscreenControlsHandler = Handler(Looper.getMainLooper())
    var seriesVisible = MutableLiveData<Boolean?>()


    override fun getCurrentVodContent(): Content? {
        return currentContent
    }

    override fun getLastVodContent(): Content? {
        return lastContent
    }

    override fun getLastWatchDuration(): Int = previousWatchDuration

    override fun getCurrentWatchDuration(): Int {
        var currentWatchDuration = 0
        val thisTime = Instant.now()
        lastSwitchTime?.let {
            currentWatchDuration = Duration.millis(thisTime.millis - it.millis).toSeconds()
            currentWatchDuration -= lastPausesDuration
        }
        return currentWatchDuration
    }

    override fun getContentName(): String? {
        return currentContent?.name
    }

    override fun getContentType(): String? {
        return currentContent?.type
    }

    fun setCollection(collection: Collection?, needShow: Boolean = false) {
        previousCollection = currentCollection
        currentCollection = collection
        needShowCollection.postValue(needShow)
    }


    fun openContent(content: Content) {
        when (content) {
            is Series -> {
                currentSeries = content
                needShowSeries.postValue(true)
            }
            is Season -> {
                currentSeason = content
                needShowSeason.postValue(true)
            }
            else -> {
                Timber.d("openContent() ${content.sourceUrl}")

                lastContent = currentContent
                currentContent = content
                currentContentLiveData.postValue(content)
                needOpenContent.postValue(content)

                resetWatchDuration()
                needSendWatchEvent.postValue(true)
                needShowContent.postValue(true)
            }
        }
    }

    fun getCurrentContentType(): ContentType {
        return when (currentContent) {
            is Episode -> {
                ContentType.episode
            }
            is Movie -> {
                ContentType.movie
            }
            else -> ContentType.none
        }
    }

    fun showEpisodes(isVisible: Boolean) {
        seriesVisible.postValue(isVisible)
    }

    fun refresh() {
        needOpenContent.postValue(currentContent)
        currentContentLiveData.postValue(currentContent)
    }

    fun requestFullscreenControls(durationToShow: Long = FULLSCREEN_CONTROLS_DURATION) {
        needShowFullscreenDescription.value = true
        fullscreenControlsHandler.removeCallbacksAndMessages(null)
        fullscreenControlsHandler.postDelayed({
            needShowFullscreenDescription.postValue(false)
        }, durationToShow)
    }

    fun resetWatchDuration() {
        val thisTime = Instant.now()
        lastSwitchTime?.let {
            previousWatchDuration = Duration.millis(thisTime.millis - it.millis).toSeconds()
            previousWatchDuration -= lastPausesDuration
        }
        lastPausesDuration = 0
        lastSwitchTime = thisTime
    }

    fun setPaused(isPaused: Boolean) {
        if (isPaused) {
            lastPauseInstant = Instant.now()
        } else {
            lastPauseInstant?.let {
                val thisTime = Instant.now()
                lastPausesDuration += Duration.millis(thisTime.millis - it.millis).toSeconds()
            }
        }
    }

    private fun sendWatchEvent() {
        if (currentContent != null) {
            val currentUrl = currentContent?.sourceUrl ?: ""
            ams.sendWatchEvent(currentUrl, false)
            Timber.d("sendWatchEvent() current ${currentUrl}")
        }
    }

    private fun Duration.toSeconds(): Int = this.toStandardSeconds().seconds

    fun reset() {
        needShowContent.postValue(false)
        needShowSeries.postValue(false)
        needShowCollection.postValue(false)

        needOpenContent.postValue(null)
        currentContentLiveData.postValue(null)
        needShowFullscreenDescription.postValue(null)
        needSendWatchEvent.postValue(null)
    }
}