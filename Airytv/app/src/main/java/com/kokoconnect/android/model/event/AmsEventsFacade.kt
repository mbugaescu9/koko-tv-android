package com.kokoconnect.android.model.event

import com.kokoconnect.android.model.AiryContentType
import com.kokoconnect.android.util.DOMAIN_YOUTUBE
import com.kokoconnect.android.vm.tv.AmsTvGuideInfo
import com.kokoconnect.android.vm.vod.AmsVodContentInfo

interface AmsEventsFacade {
    fun connectWithTvGuideInfo(amsTvGuideInfo: AmsTvGuideInfo)
    fun connectWithVodContentInfo(amsVodContentInfo: AmsVodContentInfo)
    fun updateContentType(contentType: AiryContentType?)
    fun updateUser()
    fun sendWatchEvent(videoUrl: String? = DOMAIN_YOUTUBE, isCurrent: Boolean = true)
    fun startStaticTimerEvent()
    fun resumeStaticTimerEvent()
    fun pauseStaticTimerEvent()
    fun sendBrowserEvent(currentUrl: String?, durationSec: Int? = null)
    fun sendStaticTimerEvent(data: StaticTimerEventData?)
    fun sendLandingEvent(landing_url: String?, duration: Long? = null)
    fun sendRatingEventStars()
    fun sendRatingEventStars(stars: Float)
    fun sendRatingEventStarsCancel()
    fun sendRatingEventGooglePlay()
    fun sendRatingEventGooglePlayCancel()
    fun sendRatingEventMessage(message: String)
    fun sendRatingEventMessageCancel()
    fun sendAdEventInit(adEventParams: AdEventParams)
    fun sendAdEventReinit(adEventParams: AdEventParams)
    fun sendAdEventLoaded(adEventParams: AdEventParams)
    fun sendAdEventLoadFail(adEventParams: AdEventParams)
    fun sendAdEventShow(adEventParams: AdEventParams)
    fun sendAdEventClicked(adEventParams: AdEventParams)
    fun sendGiveawaysEventOpenList()
    fun sendGiveawaysEventClickedEnter(payload: String)
    fun getExternalIp(): String?
    fun releaseTimers()
}