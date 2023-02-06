package com.kokoconnect.android.model.ads.video

import android.annotation.SuppressLint
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.kokoconnect.android.model.ads.AdsStatus
import com.kokoconnect.android.model.player.VideoAdLoader
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.model.event.AmsEventsManager
import com.kokoconnect.android.util.*
import com.kokoconnect.android.vm.tv.VideoAdTvGuideInfo
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class TvVideoAdsManager(
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
    private  val imaUrlPreparer = TvImaUrlPreparer()
    private var adsStatus: AdsStatus? = null
    private var imaProgramAds: ImaProgramAds? = null
    private var lastDetectedCueTonesTime: Date? = null
    private var countChannelSwitches = 0
    private val videoAdExecutor = Executors.newSingleThreadScheduledExecutor()
    private var videoAdTimerFuture: ScheduledFuture<*>? = null

    fun init(
        activity: FragmentActivity,
        videoAdTvGuideInfo: VideoAdTvGuideInfo?
    ) {
        imaUrlPreparer.initialize(activity, videoAdTvGuideInfo, amsEventsManager)
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

    fun setProgramAds(imaProgramAds: ImaProgramAds?) {
        this.imaProgramAds = imaProgramAds
    }

    fun onFirstTune() {
        val reason = VideoAdTrigger.OnTvFirstTune
        val setup = adsStatus?.getSetupVideoAd(reason) ?: return
        if (isAnyAdsEnabled(reason)) {
            showVideoAds(setup, reason) {
                onReturnStream.postValue(true)
            }
        }
    }

    fun onChannelChange() {
        countChannelSwitches++
        Timber.d(" onChannelsChange() countSwitches = ${countChannelSwitches}")
        val reason = VideoAdTrigger.OnTvChannelChange
        val adsStatus = adsStatus ?: return
        val setup = adsStatus.getSetupVideoAd(reason) ?: return
        if (isAnyAdsEnabled(reason)) {
            if (countChannelSwitches >= setup.imaVast().numberOfChannels()) {
                Timber.d("onChannelsChange() showVideoAds")
                countChannelSwitches = 0
                showVideoAds(setup, VideoAdTrigger.OnTvChannelChange) {
                    onReturnStream.postValue(true)
                }
            }
        }
    }

    fun onDetectedCueTones(tag: String, channelId: Int?) {
        val adsStatus = adsStatus ?: return
        if (!adRunning && (channelId == null || adsStatus.isCueTonesEnabled(channelId))) {
            val duration: Int = CueTonesParser.parseDuration(tag) ?: return
            Timber.d("onDetectedCueTones() duration = ${duration}")
            if (lastDetectedCueTonesTime == null || Date().after(lastDetectedCueTonesTime)) {
                Timber.d("onDetectedCueTones() duration = ${duration} showVideoAds lastDetectedCueTones ${lastDetectedCueTonesTime}")
                lastDetectedCueTonesTime = Date(Date().time + (duration * 1000))
                val reason = VideoAdTrigger.OnTvCueTones(duration)
                val setup = adsStatus.getSetupVideoAd(reason)
                if (isAnyAdsEnabled(reason)) {
                    Timber.d("onDetectedCueTones() duration = ${duration} showVideoAds")
                    showVideoAds(setup, reason) {
                        onReturnStream.postValue(true)
                    }
                }
            } else {
                Timber.v("onDetectedCueTones() not run because lastDetectedCueTonesTime is later current time")
            }
        }
    }

    fun releaseCueTonesAd() {
        lastDetectedCueTonesTime = null
    }

    fun onProgramChange() {
        Timber.d("onProgramChange()")
        val adsStatus = adsStatus ?: return
        val reason = VideoAdTrigger.OnTvProgramChange
        val setup = adsStatus.getSetupVideoAd(reason)
        if (isAnyAdsEnabled(reason)) {
            showVideoAds(setup, reason) {
                onReturnStream.postValue(true)
            }
        }
    }

    private fun onNeedRunTimer() {
        val adsStatus = adsStatus ?: return
        val reason = VideoAdTrigger.OnTvTimer
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

    @SuppressLint("BinaryOperationInTimber")
    fun onCurrentPositionChanged(positionMs: Long) {
        val currentAdsBlock = imaProgramAds?.getCurrentAdBlock(positionMs)
        val currentAds = imaProgramAds?.getCurrentAds(currentAdsBlock)
        if (currentAds == null || currentAds.isEmpty()) return
        val reason = VideoAdTrigger.OnTvProgramTimestamp
        val videoAds = currentAds.map { ImaVastVideoAd(it, context) }
        Timber.d("onCurrentPositionChanged() " +
                    "position ${positionMs} " +
                    "currentAds ${currentAds.joinToString { it.name.toString() }} " +
                    "currentBlock ${currentAdsBlock?.title}"
        )
        showVideoAds(videoAds, reason) {
            onReturnStream.postValue(true)
        }
    }

    private fun isAnyAdsEnabled(videoAdTrigger: VideoAdTrigger): Boolean {
        return if (imaProgramAds != null) {
            // if current ads is ima program (block) ads
            imaProgramAds?.getSetupVideoAd(videoAdTrigger)?.isAnyEnabled() ?: false
        } else {
            // if current ads is ordinal video ads
            adsStatus?.getSetupVideoAd(videoAdTrigger)?.isAnyEnabled() ?: false
        }
    }

    override fun prepareVastUrl(inputUrl: String?): String? {
        return imaUrlPreparer.prepareImaUrl(inputUrl)
    }
}