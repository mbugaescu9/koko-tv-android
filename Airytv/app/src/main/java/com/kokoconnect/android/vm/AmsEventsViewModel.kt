package com.kokoconnect.android.vm

import androidx.lifecycle.ViewModel
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.model.AiryContentType
import com.kokoconnect.android.model.event.*
import com.kokoconnect.android.vm.tv.AmsTvGuideInfo
import com.kokoconnect.android.vm.vod.AmsVodContentInfo
import javax.inject.Inject

class AmsEventsViewModel @Inject constructor() : ViewModel(), AmsEventsFacade {

    private fun getAms(): AmsEventsFacade = AiryTvApp.instance.ams

    override fun onCleared() {
        super.onCleared()
        getAms().releaseTimers()
    }

    override fun updateContentType(contentType: AiryContentType?) {
        getAms().updateContentType(contentType)
    }

    override fun connectWithTvGuideInfo(amsTvGuideInfo: AmsTvGuideInfo) {
        getAms().connectWithTvGuideInfo(amsTvGuideInfo)
    }

    override fun connectWithVodContentInfo(amsVodContentInfo: AmsVodContentInfo) {
        getAms().connectWithVodContentInfo(amsVodContentInfo)
    }

    override fun updateUser() {
        getAms().updateUser()
    }

    override fun sendWatchEvent(videoUrl: String?, isCurrent: Boolean) {
        getAms().sendWatchEvent(videoUrl, isCurrent)
    }

    override fun startStaticTimerEvent() {
        getAms().startStaticTimerEvent()
    }

    override fun resumeStaticTimerEvent() {
        getAms().resumeStaticTimerEvent()
    }

    override fun pauseStaticTimerEvent() {
        getAms().pauseStaticTimerEvent()
    }

    override fun sendBrowserEvent(
        currentUrl: String?,
        durationSec: Int?
    ) {
        getAms().sendBrowserEvent(currentUrl, durationSec)
    }

    override fun sendStaticTimerEvent(data: StaticTimerEventData?) {
        getAms().sendStaticTimerEvent(data)
    }

    override fun sendLandingEvent(landing_url: String?, duration: Long?) {
        getAms().sendLandingEvent(landing_url, duration)
    }

    override fun sendRatingEventStars() {
        getAms().sendRatingEventStars()
    }

    override fun sendRatingEventStars(stars: Float) {
        getAms().sendRatingEventStars(stars)
    }

    override fun sendRatingEventStarsCancel() {
        getAms().sendRatingEventStarsCancel()
    }

    override fun sendRatingEventGooglePlay() {
        getAms().sendRatingEventGooglePlay()
    }

    override fun sendRatingEventGooglePlayCancel() {
        getAms().sendRatingEventGooglePlayCancel()
    }

    override fun sendRatingEventMessage(message: String) {
        getAms().sendRatingEventMessage(message)
    }

    override fun sendRatingEventMessageCancel() {
        getAms().sendRatingEventMessageCancel()
    }

    override fun sendAdEventInit(adEventParams: AdEventParams) {
        getAms().sendAdEventInit(adEventParams)
    }

    override fun sendAdEventReinit(adEventParams: AdEventParams) {
        getAms().sendAdEventReinit(adEventParams)
    }

    override fun sendAdEventClicked(adEventParams: AdEventParams) {
        getAms().sendAdEventClicked(adEventParams)
    }

    override fun sendAdEventLoaded(adEventParams: AdEventParams) {
        getAms().sendAdEventLoaded(adEventParams)
    }

    override fun sendAdEventLoadFail(adEventParams: AdEventParams) {
        getAms().sendAdEventLoadFail(adEventParams)
    }

    override fun sendAdEventShow(adEventParams: AdEventParams) {
        getAms().sendAdEventShow(adEventParams)
    }

    override fun sendGiveawaysEventOpenList() {
        getAms().sendGiveawaysEventOpenList();
    }

    override fun sendGiveawaysEventClickedEnter(payload: String) {
        getAms().sendGiveawaysEventClickedEnter(payload)
    }

    override fun getExternalIp(): String? {
        return getAms().getExternalIp()
    }

    override fun releaseTimers() {
        // ignore because called in onCleared()
    }

}