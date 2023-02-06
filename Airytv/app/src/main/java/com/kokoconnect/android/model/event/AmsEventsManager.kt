package com.kokoconnect.android.model.event

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.*
import androidx.lifecycle.ProcessLifecycleOwner
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.model.AiryContentType
import com.kokoconnect.android.service.AmsService
import com.kokoconnect.android.model.tv.ProgramDescription
import com.kokoconnect.android.model.vod.Content
import com.kokoconnect.android.util.DOMAIN_YOUTUBE
import com.kokoconnect.android.util.NetworkUtils
import com.kokoconnect.android.vm.tv.AmsTvGuideInfo
import com.kokoconnect.android.vm.vod.AmsVodContentInfo
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.min

class AmsEventsManager(application: Application) : AmsEventsFacade, LifecycleObserver {
    companion object {
        private val staticTimerDelayValues = arrayOf<Long>(10)
    }

    private var context = application
    var mService: AmsService? = null
    private var mBound = false
    val mConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as AmsService.AmsBinder
            mService = binder.service
            onBindService()
            mBound = true
            // AmsService binded, in application class we cannot catch onDestroy and unbind
            // but android system will destroy service
            if (inForeground) {
                //if lifecycle callback triggered before service was bound, send the event again
                sendForegroundEvent()
            }
            Timber.d("AmsEventsViewModel: ServiceConnection.onServiceConnected() ${mService}")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.d("AmsEventsViewModel: ServiceConnection.onServiceDisconnected()")
            mBound = false
        }
    }

    private var dataStack: Queue<EventData> = LinkedList<EventData>()

    var inForeground = false
    private var appStarted = false
    private var firstDataLoaded = false

    private var lastContentType: AiryContentType? = null
    private var currentContentType: AiryContentType? = null

    var amsTvGuideInfo: AmsTvGuideInfo? = null
    var amsVodContentInfo: AmsVodContentInfo? = null

    private var browseParentUrl: String? = null

    private var lastStaticTimerEventData: StaticTimerEventData? = null
    private var executorTimerEvent = Executors.newSingleThreadScheduledExecutor()
    private var lastScheduledStaticTimerFuture: ScheduledFuture<*>? = null
    private var staticTimerDelayIndex = 0
    private var currentStaticTimerDelay: Long = staticTimerDelayValues.get(staticTimerDelayIndex)
    private var staticTimerDelayPaused = -1L
    private val runnableStaticTimerEvent = Runnable {
        lastStaticTimerEventData = StaticTimerEventData(
            content_name = getContentName(true),
            is_active_player = true,
            current_url = getUrl(true)
        )
        sendStaticTimerEvent(lastStaticTimerEventData)
        staticTimerDelayIndex = min(staticTimerDelayIndex + 1, staticTimerDelayValues.size - 1)
        if (!executorTimerEvent.isShutdown) {
            currentStaticTimerDelay = staticTimerDelayValues[staticTimerDelayIndex]
            setExecutorStaticTimer()
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun setExecutorStaticTimer() {
        lastScheduledStaticTimerFuture = executorTimerEvent.schedule(
            runnableStaticTimerEvent,
            currentStaticTimerDelay,
            TimeUnit.SECONDS
        )
    }

    private fun stopExecutorStaticTimer() {
        lastScheduledStaticTimerFuture?.let {
            if (!it.isCancelled) {
                it.cancel(false)
            }
        }
        lastScheduledStaticTimerFuture = null
    }

    private fun onBindService() {
        val arrayForSend = ArrayList<EventData>(dataStack)
        dataStack.removeAll(arrayForSend)
        for (data in arrayForSend) {
            when (data) {
                is AdvertisementEventData -> mService?.sendAdvertisementEvent(data)
                is BrowseEventData -> mService?.sendBrowserEvent(data)
                is WatchEventData -> mService?.sendWatchEvent(data)
                is LandingEventData -> mService?.sendLandingEvent(data)
                is RatingEventData -> mService?.sendRatingEvent(data)
                is GiveawaysEventData -> mService?.sendGiveawaysEvent(data)
                is StaticTimerEventData -> mService?.sendStaticTimerEvent(data)
            }
        }
    }

    private fun checkService(data: EventData): Boolean {
        val result = if (mService == null) {
            dataStack.offer(data)
            false
        } else {
            true
        }
        Timber.v("AmsEventsViewModel: checkService() == $result")
        return result
    }

    override fun releaseTimers() {
        Timber.d("AmsEventsViewModel: releaseTimers()")
        dataStack.clear()
        stopExecutorStaticTimer()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppInForeground() {
        inForeground = true
        Timber.d("onAppInForeground")
        sendForegroundEvent()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppInBackground() {
        inForeground = false
        Timber.d("onAppInBackground")
        sendBackgroundEvent()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        Timber.d("AmsEventsViewModel: onStart()")
        val intent = Intent(context, AmsService::class.java)
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Timber.d("AmsEventsViewModel: onStop()")
        if (mBound) {
            context.unbindService(mConnection)
            mBound = false
        }
    }

    override fun connectWithTvGuideInfo(amsTvGuideInfo: AmsTvGuideInfo) {
        this.amsTvGuideInfo = amsTvGuideInfo
    }

    override fun connectWithVodContentInfo(amsVodContentInfo: AmsVodContentInfo) {
        this.amsVodContentInfo = amsVodContentInfo
    }

    override fun updateContentType(contentType: AiryContentType?) {
        lastContentType = currentContentType
        currentContentType = contentType
    }

    override fun updateUser() {
        mService?.updateUser()
    }

    private fun getWatchDuration(isCurrent: Boolean): Int? {
        return when (
            if (isCurrent) currentContentType else lastContentType
        ) {
            AiryContentType.TV -> {
                if (isCurrent) {
                    amsTvGuideInfo?.getCurrentWatchDuration()
                } else {
                    amsTvGuideInfo?.getLastWatchDuration()
                }
            }
            AiryContentType.VOD -> {
                if (isCurrent) {
                    amsVodContentInfo?.getCurrentWatchDuration()
                } else {
                    amsVodContentInfo?.getLastWatchDuration()
                }
            }
            else -> {
                null
            }
        }
    }

    private fun getContentType(isCurrent: Boolean = true): AiryContentType? {
        return if (isCurrent) currentContentType else lastContentType
    }

    private fun getChannelNumber(isCurrent: Boolean = true): Int? {
        return when (getContentType(isCurrent)) {
            AiryContentType.TV -> {
                if (isCurrent) {
                    amsTvGuideInfo?.getCurrentDescription()?.channelNumber
                } else {
                    amsTvGuideInfo?.getLastDescription()?.channelNumber
                }
            }
            AiryContentType.VOD -> {
                null
            }
            else -> {
                null
            }
        }
    }

    private fun getChannelName(isCurrent: Boolean = true): String? {
        return when (getContentType(isCurrent)) {
            AiryContentType.TV -> {
                if (isCurrent) {
                    amsTvGuideInfo?.getCurrentDescription()?.channelName
                } else {
                    amsTvGuideInfo?.getLastDescription()?.channelName
                }
            }
            AiryContentType.VOD -> {
                null
            }
            else -> {
                null
            }
        }
    }

    private fun getContentId(isCurrent: Boolean = true): Long? {
        return when (getContentType(isCurrent)) {
            AiryContentType.TV -> {
                if (isCurrent) {
                    amsTvGuideInfo?.getCurrentDescription()?.channelId?.toLong()
                } else {
                    amsTvGuideInfo?.getLastDescription()?.channelId?.toLong()
                }
            }
            AiryContentType.VOD -> {
                if (isCurrent) {
                    amsVodContentInfo?.getCurrentVodContent()?.id
                } else {
                    amsVodContentInfo?.getLastVodContent()?.id
                }
            }
            else -> {
                null
            }
        }
    }

    private fun getContentName(isCurrent: Boolean = true): String? {
        return when (getContentType(isCurrent)) {
            AiryContentType.TV -> {
                if (isCurrent) {
                    amsTvGuideInfo?.getCurrentDescription()?.programName
                } else {
                    amsTvGuideInfo?.getLastDescription()?.programName
                }
            }
            AiryContentType.VOD -> {
                if (isCurrent) {
                    amsVodContentInfo?.getCurrentVodContent()?.name
                } else {
                    amsVodContentInfo?.getLastVodContent()?.name
                }
            }
            else -> {
                null
            }
        }
    }

    private fun getUrl(programDescription: ProgramDescription?): String? {
        return programDescription?.let {
            "airy://${programDescription.category}/" +
                    "${programDescription.channelNumber}_" +
                    programDescription.programName.replace(" ", "_")
        }
    }

    private fun getUrl(content: Content?): String? {
        return content?.let {
            "airy://${content.type}/" +
                    "${content.name?.replace(" ", "_")}"
        }
    }

    private fun getUrl(isCurrent: Boolean = true): String? {
        return when (getContentType(isCurrent)) {
            AiryContentType.TV -> {
                val programDescription = if (isCurrent) {
                    amsTvGuideInfo?.getCurrentDescription()
                } else {
                    amsTvGuideInfo?.getLastDescription()
                }
                getUrl(programDescription)
            }
            AiryContentType.VOD -> {
                val content = if (isCurrent) {
                    amsVodContentInfo?.getCurrentVodContent()
                } else {
                    amsVodContentInfo?.getLastVodContent()
                }
                getUrl(content)
            }
            else -> {
                null
            }
        }
    }


    override fun sendWatchEvent(videoUrl: String?, isCurrent: Boolean) {
        val watchDuration = getWatchDuration(isCurrent)
        if (watchDuration == null || watchDuration == 0 ) {
            Timber.d("AmsEventsManager: sendWatchEvent() watchDuration == 0, ignore event")
        } else {
            val data = WatchEventData(
                    duration = watchDuration,
                    content_type = getContentType(isCurrent)?.typeName,
                    content_name = getContentName(isCurrent),
                    content_id = getContentId(isCurrent),
                    content_source = NetworkUtils.getDomainName(videoUrl) ?: DOMAIN_YOUTUBE,
                    channel_num = getChannelNumber(isCurrent),
                    channel_name = getChannelName(isCurrent),
                    current_url = getUrl(isCurrent)
            )
            if (checkService(data)) {
                mService?.sendWatchEvent(data)
            }
        }
    }

    override fun startStaticTimerEvent() {
        stopExecutorStaticTimer()
        staticTimerDelayIndex = 0
        currentStaticTimerDelay = staticTimerDelayValues.firstOrNull() ?: 0
        setExecutorStaticTimer()
    }

    override fun resumeStaticTimerEvent() {
        if (staticTimerDelayPaused > 0) {
            currentStaticTimerDelay = staticTimerDelayPaused
            stopExecutorStaticTimer()
            setExecutorStaticTimer()
        }
    }

    override fun pauseStaticTimerEvent() {
        staticTimerDelayPaused = lastScheduledStaticTimerFuture?.getDelay(TimeUnit.SECONDS) ?: -1
        stopExecutorStaticTimer()
    }

    override fun sendBrowserEvent(
        currentUrl: String?,
        durationSec: Int?
    ) {
        val data = BrowseEventData(
            content_name = null,
            current_url = currentUrl,
            content_description = null,
            parent_url = browseParentUrl,
            duration = durationSec
        )
        browseParentUrl = data.current_url
        if (checkService(data)) {
            mService?.sendBrowserEvent(data)
        }
    }

    override fun sendStaticTimerEvent(data: StaticTimerEventData?) {
        data?.copy()?.let {
            if (checkService(it)) {
                mService?.sendStaticTimerEvent(it)
            }
        }
    }

    override fun sendLandingEvent(landing_url: String?, duration: Long?) {
        if (landing_url == LandingEvent.URL_START && !appStarted) {
            appStarted = true
        } else if (landing_url == LandingEvent.URL_START && appStarted) {
            return
        }
        if (landing_url == LandingEvent.URL_LOADED && !firstDataLoaded) {
            firstDataLoaded = true
        } else if (landing_url == LandingEvent.URL_LOADED && firstDataLoaded) {
            return
        }

        val loadingDuration = duration ?: AiryTvApp.instance.getDurationFromStart()
        val data = LandingEventData(
            landing_url = landing_url,
            loading_time = loadingDuration
        )
        if (checkService(data)) {
            mService?.sendLandingEvent(data)
        }
    }

    private fun sendRatingEvent(
        title: String?,
        description: String?
    ) {
        val data = RatingEventData(
            content_name = title,
            payload = description
        )
        if (checkService(data)) {
            mService?.sendRatingEvent(data)
        }
    }

    override fun sendRatingEventStars() {
        sendRatingEvent("Stars", "dialog")
    }

    override fun sendRatingEventStars(stars: Float) {
        sendRatingEvent("Stars", stars.toString())
    }

    override fun sendRatingEventStarsCancel() {
        sendRatingEvent("Stars", "cancel")
    }

    override fun sendRatingEventGooglePlay() {
        sendRatingEvent("GooglePlay", "dialog")
    }

    override fun sendRatingEventGooglePlayCancel() {
        sendRatingEvent("GooglePlay", "cancel")
    }

    override fun sendRatingEventMessage(message: String) {
        sendRatingEvent("Message", message)
    }

    override fun sendRatingEventMessageCancel() {
        sendRatingEvent("Message", "cancel")
    }

    private fun sendAdvertisementEvent(adEventParams: AdEventParams) {
        val data = AdvertisementEventData(
            ad_title = adEventParams.adTitle,
            ad_banner_type = adEventParams.adKey,
            ad_trigger = adEventParams.adTrigger,
            ad_description = adEventParams.adDescription,
            click = adEventParams.adClick,
            ad_type = adEventParams.adType,

            content_type = getContentType()?.typeName,
            content_name = getContentName(),
            content_id = getContentId(),
            channel_num = getChannelNumber(),
            channel_name = getChannelName()
        )
        if (checkService(data)) {
            mService?.sendAdvertisementEvent(data)
        }
    }

    override fun sendAdEventInit(adEventParams: AdEventParams) {
        adEventParams.adTitle = "Init"
        sendAdvertisementEvent(adEventParams)
    }

    override fun sendAdEventLoaded(adEventParams: AdEventParams) {
        adEventParams.adTitle = "Loaded"
        sendAdvertisementEvent(adEventParams)
    }

    override fun sendAdEventLoadFail(adEventParams: AdEventParams) {
        adEventParams.adTitle = "LoadFail"
        sendAdvertisementEvent(adEventParams)
    }

    override fun sendAdEventShow(adEventParams: AdEventParams) {
        adEventParams.adTitle = "Show"
        sendAdvertisementEvent(adEventParams)
    }

    override fun sendAdEventClicked(adEventParams: AdEventParams) {
        adEventParams.adTitle = "Clicked"
        sendAdvertisementEvent(adEventParams)
    }

    override fun sendAdEventReinit(adEventParams: AdEventParams) {
        adEventParams.adTitle = "Reinit"
        sendAdvertisementEvent(adEventParams)
    }

    private fun sendGiveawaysEvent(
        action: String?,
        payload: String?
    ) {
        val data = GiveawaysEventData(
            content_name = null,
            action_name = action,
            payload = payload
        )
        if (checkService(data)) {
            mService?.sendGiveawaysEvent(data)
        }
    }

    override fun sendGiveawaysEventOpenList() {
        sendGiveawaysEvent("List", "")
    }

    override fun sendGiveawaysEventClickedEnter(payload: String) {
        sendGiveawaysEvent("Enter", payload)
    }

    override fun getExternalIp(): String? = mService?.getExternalIp()

    private fun sendForegroundEvent() {
        val data = BrowseEventData(
            content_name = null,
            content_description = null,
            current_url = "airy://foreground",
            parent_url = null,
            duration = null
        )
        mService?.sendBrowserEvent(data)
    }

    private fun sendBackgroundEvent() {
        val data = BrowseEventData(
            content_name = null,
            content_description = null,
            current_url = "airy://background",
            parent_url = null,
            duration = null
        )
        mService?.sendBrowserEventNow(data)
    }

}