package com.kokoconnect.android.vm.tv

import android.app.Application
import android.os.SystemClock
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.model.ads.video.ImaProgramAds
import com.kokoconnect.android.model.ads.video.ImaProgramParams
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.model.player.*
import com.kokoconnect.android.model.response.ApiError
import com.kokoconnect.android.model.tv.ProgramDescription
import com.kokoconnect.android.model.player.VideoOpeningReason
import com.kokoconnect.android.model.tv.Category
import com.kokoconnect.android.model.tv.Channel
import com.kokoconnect.android.model.tv.ChannelsResponse
import com.kokoconnect.android.ui.fragment.tv.drop
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.util.ContentUrlPreparer
import com.kokoconnect.android.util.DateUtils
import com.kokoconnect.android.util.UpdateTimer
import com.kokoconnect.android.vm.AdsViewModel
import org.joda.time.Duration
import org.joda.time.Instant
import timber.log.Timber
import java.util.*
import javax.inject.Inject

interface AmsTvGuideInfo {
    fun getCurrentDescription(): ProgramDescription?
    fun getLastWatchDuration(): Int
    fun getLastDescription(): ProgramDescription?
    fun getCurrentWatchDuration(): Int
}

interface VideoAdTvGuideInfo {
    fun getShowName(): String
    fun getChannelName(): String
}

private const val TICK_INTERVAL = 5000L

class TvGuideViewModel @Inject constructor(
    private val airyRepo: AiryRepository,
    private val app: AiryTvApp
) : AndroidViewModel(app), VideoAdTvGuideInfo, AmsTvGuideInfo {

    private var context = getApplication<Application>()
    private val ams: AmsEventsFacade
        get() {
            return app.ams
        }
    private var contentUrlPreparer = ContentUrlPreparer()


    val guideLiveData: MutableLiveData<ChannelsResponse?> = MutableLiveData()
    val channelLiveData: MutableLiveData<Channel?> = MutableLiveData()

    private var lastProgramDescription: ProgramDescription? = null
    private var currentProgramDescription: ProgramDescription? = null
    private var channelNumberToOpen: Int? = null
    var currentDescriptionLiveData = MutableLiveData<ProgramDescription?>()
    var channelNumberLiveData = MutableLiveData<Int>()
    var programProgress = MutableLiveData<Pair<Int, Int>>()
    var needOpenProgram = MutableLiveData<ProgramDescription?>()

    private var timerForNextProgramOrPart: Timer? = null
    private var timerForUpdateProgramProgress: Timer? = null
    private var lastPauseInstant: Instant? = null
    private var lastSwitchTime: Instant? = null
    private var lastWatchDuration: Int = 0
    private var lastPausesDuration: Int = 0

    val timeTickLiveData = MutableLiveData<Long>()
    val errorLiveData = MutableLiveData<ApiError>().apply { ApiError.NONE }
    var isPopupDescriptionVisible = MutableLiveData<Boolean>()
    private val needUpdateGuide = MutableLiveData<Boolean>().apply { value = false }
    private val updateTimer: UpdateTimer = UpdateTimer(needUpdateGuide)

    var lastStream: PlayerObject? = null
    var currentStream: PlayerObject? = null

    // only for use in setCurrentChannel and previousChannel.
    // for get current channel use getChannel().
    private var currentChannel: Channel? = null
    private var previousChannel: Channel? = null

    init {
        updateTimer.start()
        needUpdateGuide.observeForever {
            if (it == true) {
                needUpdateGuide.postValue(false)
                requestGuideFromServer()
            }
        }
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                timeTickLiveData.postValue(
                    System.currentTimeMillis().drop(TICK_INTERVAL)
                )
            }
        }, SystemClock.currentThreadTimeMillis().drop(TICK_INTERVAL) + TICK_INTERVAL, TICK_INTERVAL)
    }

    fun init(activity: FragmentActivity) {
        contentUrlPreparer.initialize(activity)
    }

    fun reopenCurrentChannel() {
        channelLiveData.value = channelLiveData.value
    }

    fun requestGuideFromServer() {
        Timber.d("requestGuideFromServer()")
        airyRepo.getGuide(guideLiveData, errorLiveData)
    }

    fun isGuideNotEmpty(): Boolean = guideLiveData.value?.categories?.isNotEmpty() ?: false
    fun isGuideLoaded(): Boolean = guideLiveData.value != null
    fun getGuide(): List<Category>? = guideLiveData.value?.categories
    fun getChannel(): Channel? = currentChannel
    fun getFirstChannel(): Channel? = getGuide()?.firstOrNull()?.channels()?.firstOrNull()
    fun getAdsParams(): Map<String, ImaProgramParams>? = guideLiveData.value?.advertisements

    fun isProgramInitialized() = getChannel() != null

    fun openChannelIfNecessary() {
        Timber.d("openChannelIfNecessary() channel number to open ${channelNumberToOpen}")
        val guide = guideLiveData.value?.categories ?: return
        val channelNumber =
            channelNumberToOpen ?: getChannel()?.number ?: getFirstChannel()?.number ?: return
        Timber.d("openChannelIfNecessary() channel number to open ${channelNumber}")
        val channel =
            getChannelFromGuideByNumber(channelNumber, guide) ?: getFirstChannel() ?: return
        channel.openingReason = if (getChannel() == null) {
            VideoOpeningReason.ON_TV_FIRST_TUNE
        } else {
            VideoOpeningReason.ON_TV_CHANNEL_CHANGE
        }
        channelLiveData.postValue(channel)
    }

    fun openChannel(channelNumber: Int) {
        Timber.d("openChannel() channel number ${channelNumber}")
        val guide = guideLiveData.value?.categories
        if (guide == null) {
            channelNumberToOpen = channelNumber
            return
        }
        val channel = getChannelFromGuideByNumber(channelNumber, guide) ?: return
        channel.openingReason = if (getChannel() == null) {
            VideoOpeningReason.ON_TV_FIRST_TUNE
        } else {
            VideoOpeningReason.ON_TV_CHANNEL_CHANGE
        }
        channelLiveData.postValue(channel)
    }

    // only to call from channelLiveData observer
    // for open channel use channelLiveData.postValue(channel)
    fun openChannel(newChannel: Channel, adsViewModel: AdsViewModel, ignoreNeedOpenChannelCheck: Boolean = false) {
        Timber.d("openChannel() channel name = ${newChannel.name} private = ${newChannel.private}")
        val currentChannel = getChannel()
        val needOpenChannel = if (ignoreNeedOpenChannelCheck) {
            true
        } else {
            currentChannel == null || (newChannel.id != currentChannel.id)
        }
        Timber.d("openChannel() needOpenChannel = $needOpenChannel")

        lastProgramDescription = currentProgramDescription
        lastStream = currentStream
        Preferences(context).Guide().setChannelId(newChannel.id)

        if (needOpenChannel) {
            updateWatchDuration()
            setCurrentChannel(newChannel)
            channelNumberLiveData.postValue(newChannel.number)
            val currentContent = newChannel.switchToCurrentContent()
            Timber.d("openChannel() channel = ${newChannel.name}, program = ${currentContent?.programName}")
            currentProgramDescription = currentContent
            currentStream = currentContent?.video
            currentProgramDescription?.videoOpeningReason = newChannel.openingReason
            Timber.d("Who called requestOpenProgram(): openChannel()")
            requestOpenProgram(currentContent)
            adsViewModel.openChannel()
        }
    }

    fun openPreviousChannel() {
        channelLiveData.postValue(previousChannel)
    }

    fun getCurrentChannelVideo() {
        updateWatchDuration()
        val currentContent = getChannel()?.switchToCurrentContent() ?: return
        currentProgramDescription = currentContent
        currentStream = currentContent.video
        currentContent?.videoOpeningReason = VideoOpeningReason.ON_RESUME
        Timber.d("Who called requestOpenProgram(): getCurrentChannelVideo()")
        requestOpenProgram(currentContent)
    }

    fun getNextChannelVideo() {
        val nextContent = getChannel()?.switchToNextContent() ?: return
        currentProgramDescription = nextContent
        currentStream = nextContent.video
        Timber.d("Who called requestOpenProgram(): getNextChannelVideo()")
        requestOpenProgram(nextContent)
    }

    // for ams

    fun updateWatchDuration() {
        val thisTime = Instant.now()
        lastSwitchTime?.let {
            lastWatchDuration = Duration.millis(thisTime.millis - it.millis).toSeconds()
            lastWatchDuration -= lastPausesDuration
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

    override fun getCurrentDescription() = currentDescriptionLiveData.value
    override fun getLastWatchDuration(): Int {
        val lastWatchDuration = lastWatchDuration
        return lastWatchDuration
    }

    override fun getLastDescription(): ProgramDescription? = lastProgramDescription
    override fun getCurrentWatchDuration(): Int {
        var currentWatchDuration = 0
        val thisTime = Instant.now()
        lastSwitchTime?.let {
            currentWatchDuration = Duration.millis(thisTime.millis - it.millis).toSeconds()
        }
        return currentWatchDuration
    }

    private fun setCurrentChannel(channel: Channel) {
        currentChannel?.close()
        previousChannel = currentChannel
        currentChannel = channel
    }


    private fun prepareProgram(description: ProgramDescription?) {
        description?.video?.prepareUrl(contentUrlPreparer)
    }

    private fun requestOpenProgram(programDescription: ProgramDescription?) {
        prepareProgram(programDescription)
        currentDescriptionLiveData.postValue(programDescription)
        needOpenProgram.postValue(programDescription)
        if (programDescription?.video?.isStream() == true) {
            getChannel()?.parseNextProgram()?.let { nextProgram ->
                startSwitchTimer(nextProgram.realStartAtIso)
            }
        } else {
            // channel with programs
            stopSwitchTimer()
        }
        startProgramProgressTimer()
    }

    fun getCurrentImaProgramAds(): ImaProgramAds? {
        val imaProgramAds = getChannel()?.getCurrentProgram()?.getImaAds() ?: return null
        val adsParams = getAdsParams() ?: return null
        imaProgramAds.setParams(adsParams)
        return imaProgramAds
    }

    private fun startProgramProgressTimer(delay: Long = 1000 * 60) {
        sendProgressPosition(currentProgramDescription)
        currentProgramDescription?.let {
            stopProgressTimer()
            timerForUpdateProgramProgress = Timer()
            val task = object : TimerTask() {
                override fun run() {
                    val position = sendProgressPosition(currentProgramDescription)
                    if (position != null && position <= it.programDurationSecs) {
                        startProgramProgressTimer(delay)
                    }
                }
            }
            timerForUpdateProgramProgress?.schedule(task, delay)
        }
    }

    private fun stopProgressTimer() {
        if (timerForUpdateProgramProgress != null) {
            try {
                timerForUpdateProgramProgress?.cancel()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                timerForUpdateProgramProgress = null
            }
        }
    }

    private fun getChannelFromGuideById(id: Int, guide: List<Category>): Channel? {
        var channel: Channel? = null
        for (item in guide) {
            channel = item.channels().find { it.id == id }
            if (channel != null) {
                break
            }
        }
        return channel
    }

    private fun getChannelFromGuideByNumber(number: Int, guide: List<Category>): Channel? {
        var channel: Channel? = null
        for (item in guide) {
            channel = item.channels().find { it.number == number }
            if (channel != null) {
                break
            }
        }
        return channel
    }

    fun setPopupDescriptionVisible(isVisible: Boolean) {
        isPopupDescriptionVisible.postValue(isVisible)
    }

    fun switchPopupDescription() {
        if (isPopupDescriptionVisible.value == true) {
            isPopupDescriptionVisible.postValue(false)
        } else {
            isPopupDescriptionVisible.postValue(true)
        }
    }

    private fun startSwitchTimer(startAtIso: String) {
        if (timerForNextProgramOrPart != null) {
            timerForNextProgramOrPart?.cancel()
        }
        timerForNextProgramOrPart = Timer()
        val task = object : TimerTask() {
            override fun run() {
                Timber.d("GuideViewModel: startSwitchTimer() run()")
                val currentContent = getChannel()?.switchToCurrentContent()
                currentContent?.videoOpeningReason = VideoOpeningReason.ON_TV_PROGRAM_CHANGE
                Timber.d("Who called requestOpenProgram(): startSwitchTimer()")
                requestOpenProgram(currentContent)
            }
        }
        var delay = DateUtils.parseIsoDate(startAtIso) - DateUtils.getCurrentDate()
        if (delay < 0) {
            //looks like day time overflow and next program starts tomorrow.
            //so add 24 hr.
            delay += 24 * 60 * 60 * 1000L
        }
        timerForNextProgramOrPart?.schedule(task, delay)
    }

    private fun stopSwitchTimer() {
        if (timerForNextProgramOrPart != null) {
            try {
                timerForNextProgramOrPart?.cancel()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                timerForNextProgramOrPart = null
            }
        }
    }

    private fun sendProgressPosition(currentProgramDescription: ProgramDescription?): Int? {
        if (currentProgramDescription == null) {
            return 0
        } else {
            currentProgramDescription.let {
                val position: Int =
                    (DateUtils.getCurrentTimeInSecs() - it.programStartSecs).toInt()
                programProgress.postValue(Pair(position, it.realProgramDuration))
                return position
            }
        }
    }

    private fun Duration.toSeconds(): Int = this.toStandardSeconds().seconds

    override fun getShowName(): String {
        return currentProgramDescription?.programName.toString()
    }

    override fun getChannelName(): String {
        return currentProgramDescription?.channelName.toString()
    }

}