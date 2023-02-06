package com.kokoconnect.android.model.ads.video

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.kokoconnect.android.model.ads.AdsStatus
import com.kokoconnect.android.model.player.VideoAdLoader
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.model.event.AmsEventsManager
import com.kokoconnect.android.vm.vod.VideoAdVodContentInfo
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class VodVideoAdsManager(
    context: Context,
    amsEventsManager: AmsEventsManager,
    airyRepo: AiryRepository,
    onNeedShowVideoAd: MutableLiveData<VideoAdLoader?>,
    onAdNotLoaded: MutableLiveData<VideoAdTrigger?>,
    onCurrentAdNumber: MutableLiveData<VideoAdNumber?>,
    val onReturnStream: MutableLiveData<Boolean>
) : VideoAdsManager(
    context,
    amsEventsManager,
    airyRepo,
    onNeedShowVideoAd,
    onAdNotLoaded,
    onCurrentAdNumber
) {
    private val imaUrlPreparer = VodImaUrlPreparer()
    private var adsStatus: AdsStatus? = null
    private val videoAdExecutor = Executors.newSingleThreadScheduledExecutor()
    private var videoAdTimerFuture: ScheduledFuture<*>? = null

    fun init(
        activity: FragmentActivity,
        contentInfo: VideoAdVodContentInfo?
    ) {
        imaUrlPreparer.initialize(activity, contentInfo, amsEventsManager)
    }

    fun setAds(adsStatus: AdsStatus?) {
        this.adsStatus = adsStatus
        rotationVideoAds = RotationVideoAds(
            context,
            adsStatus?.ima,
            adsStatus?.infomercial
        )
        onNeedRunTimer()
    }

    private fun onNeedRunTimer() {
        val adsStatus = adsStatus ?: return
        val reason = VideoAdTrigger.OnVodTimer
        val setup = adsStatus.getSetupVideoAd(reason)
        val delay = setup.imaVast().repeatIntervalSec() ?: 0
        if (delay > 0 && isAnyAdsEnabled(reason)) {
            Timber.d("onNeedRunTimer() start delay == $delay")
            videoAdTimerFuture = videoAdExecutor.scheduleWithFixedDelay({
                Timber.d("onNeedRunTimer() run scheduleWithFixedDelay")
                try {
                    showVideoAds(setup, reason) {
                        onReturnStream.postValue(true)
                    }
                } catch (e: Exception) {
                    Timber.e("onNeedRunTimer() errors ${e.message}")
                    e.printStackTrace()
                }
            }, delay, delay, TimeUnit.SECONDS)
        }
    }

    fun stopTimer() {
        videoAdTimerFuture?.let {
            if (!it.isCancelled) {
                it.cancel(false)
            }
        }
        videoAdTimerFuture = null
    }

    private fun isAnyAdsEnabled(videoAdTrigger: VideoAdTrigger): Boolean {
        return adsStatus?.getSetupVideoAd(videoAdTrigger)?.isAnyEnabled() ?: false
    }

    override fun prepareVastUrl(inputUrl: String?): String? {
        return imaUrlPreparer.prepareImaUrl(inputUrl)
    }
}