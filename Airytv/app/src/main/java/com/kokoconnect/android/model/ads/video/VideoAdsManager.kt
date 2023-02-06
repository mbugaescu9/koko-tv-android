package com.kokoconnect.android.model.ads.video

import android.content.Context
import androidx.lifecycle.*
import com.kokoconnect.android.model.event.AdEventParams
import com.kokoconnect.android.model.player.VideoAdLoader
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.model.event.AmsEventsManager
import com.kokoconnect.android.util.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

enum class VastDownloadError {
    NONE, NETWORK_PROBLEM, SERVER_ERROR, EMPTY
}

abstract class VideoAdsManager(
    val context: Context,
    val amsEventsManager: AmsEventsManager,
    val airyRepo: AiryRepository,
    val onNeedShowVideoAd: MutableLiveData<VideoAdLoader?>,
    val onAdNotLoaded: MutableLiveData<VideoAdTrigger?>,
    val onCurrentAdNumber: MutableLiveData<VideoAdNumber?>
) : CoroutineScope {
    protected var showVideoAdsJob: Job? = null
    protected var supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + supervisorJob
    val adRunning: Boolean
        get() {
            return showVideoAdsJob != null && showVideoAdsJob?.isActive == true
        }
    protected var lastAd: VideoAd? = null
    lateinit var rotationVideoAds: RotationVideoAds

    fun hideCurrentAdNumber() {
        onCurrentAdNumber.postValue(null)
    }

    fun cancelShowVideoAds() {
        Timber.d("VideoAdsManager: cancelShowVideoAds()")
        coroutineContext.cancelChildren()
        onCurrentAdNumber.postValue(null)
    }

    fun showVideoAds(
        setupVideoAd: SetupVideoAd,
        reason: VideoAdTrigger,
        onFinished: (() -> Unit)? = null
    ) {
        if (!adRunning) {
            Timber.d("VideoAdsManager: showVideoAds() run, reason == $reason")
            val videoAds: List<VideoAd> = createVideoAdsList(setupVideoAd, reason)
            Timber.d(
                "VideoAdsManager: showVideoAds() created ${videoAds.size} ads: ${
                    videoAds.map { it.type() + " " + it.name() }
                        .joinToString()
                }")
            showVideoAds(videoAds, reason, onFinished)
        } else {
            Timber.d("VideoAdsManager: showVideoAds() not run because alreadyRunAd() == true")
        }
    }


    fun showVideoAds(
        videoAds: List<VideoAd>,
        reason: VideoAdTrigger,
        onFinished: (() -> Unit)?
    ) {
        launch() {
            showVideoAds(videoAds, reason)
            onCurrentAdNumber.postValue(null)
            onFinished?.invoke()
        }
    }

    private suspend fun showVideoAds(
        videoAds: List<VideoAd>,
        reason: VideoAdTrigger
    ) = suspendCancellableCoroutine<Boolean> { continuation ->

        showVideoAdsJob = launch(coroutineContext) {
            val adsStack: Queue<VideoAd> = LinkedList<VideoAd>(videoAds)
            val videoAdsCount = videoAds.size
            var videoAdNumber = 0

            var executorXCueDuration: ScheduledExecutorService? = null

            if (reason is VideoAdTrigger.OnTvCueTones) {
                executorXCueDuration = Executors.newSingleThreadScheduledExecutor()
                executorXCueDuration.schedule({
                    Timber.d("VideoAdsManager: showVideoAds() executorXCueDuration body")
                    showVideoAdsJob?.cancel()
                    continuation.resumeWithSafe(Result.success(true))
                }, reason.duration.toLong(), TimeUnit.SECONDS)
            }

            val listener: VideoAdListener = object : VideoAdListener {
                override fun onEvent(ad: VideoAd, event: String) {
                    FirebaseLogger.logImaAdEvent(
                        adKey = ad.name(),
                        adEvent = event,
                        adPayload = ad.url()
                    )
                }

                override fun onError(ad: VideoAd, description: String) {
                    Timber.d(
                        "VideoAdsManager: showVideoAds() onError() " +
                                "listener ad.name() == ${ad.name()} " +
                                "description == ${description}"
                    )
                    FirebaseLogger.logImaAdEvent(
                        adKey = ad.name(),
                        adEvent = "ERROR",
                        adPayload = ad.url(),
                        adDescription = description
                    )

                    amsEventsManager.sendAdEventLoadFail(
                        AdEventParams(
                            adKey = ad.name(),
                            adDescription = description,
                            adClick = ad.extra(),
                            adTrigger = ad.extra(),
                            adType = ad.type()
                        )
                    )
                }

                override fun onNeedLoadNextAd() {
//                        viewModelScope.launch {
//                            adsStack.peek()?.load(airyRepo)
//                        }
                }

                override fun onStarted(ad: VideoAd, description: String) {
                    Timber.d("VideoAdsManager: showVideoAds() listener ad.name() == ${ad.name()} onStarted()")
                    amsEventsManager.sendAdEventShow(
                        AdEventParams(
                            adKey = ad.name(),
                            adDescription = description,
                            adClick = ad.extra(),
                            adTrigger = ad.extra(),
                            adType = ad.type()
                        )
                    )
                }

                override fun onLoaded(ad: VideoAd, description: String) {
                    Timber.d("VideoAdsManager: showVideoAds() listener ad.name() == ${ad.name()} onLoaded()")
                    onCurrentAdNumber.postValue(
                        VideoAdNumber(videoAdNumber, videoAdsCount)
                    )
                    amsEventsManager.sendAdEventLoaded(
                        AdEventParams(
                            adKey = ad.name(),
                            adDescription = description,
                            adClick = ad.extra(),
                            adTrigger = ad.extra(),
                            adType = ad.type()
                        )
                    )
                }

                override fun onRun(ad: VideoAd, description: String) {
                    Timber.d("VideoAdsManager: showVideoAds() listener ad.name() == ${ad.name()} onRun()")
                    amsEventsManager.sendAdEventInit(AdEventParams(
                        adKey = ad.name(),
                        adDescription = description,
                        adClick = ad.extra(),
                        adTrigger = ad.extra(),
                        adType = ad.type()
                    )
                    )
                }
            }

            // 1. prepare ads.
            for (ad in videoAds) {
                if (!isActive) {
                    break
                }
                ad.prepare(this@VideoAdsManager, listener) { loader ->
                    onNeedShowVideoAd.postValue(loader)
                }
            }

            // 2. loading and show ads.
            while (adsStack.isNotEmpty()) {
                Timber.d("VideoAdsManager: showVideoAds() next ad in while loop")
                if (!isActive) {
                    Timber.d("VideoAdsManager: showVideoAds() showVideoAdsJob?.isCancelled == true")
                    break
                }

                try {
                    val ad = adsStack.poll()
                    Timber.d("VideoAdsManager: showVideoAds() ad ${ad?.name()} with priority ${ad?.priority()} is loaded ${ad?.isLoaded()}")
                    lastAd = ad
                    Timber.d("VideoAdsManager: showVideoAds() ad loaded")
                    ad.load(airyRepo)
                    Timber.d("VideoAdsManager: showVideoAds() ad load completed")
                    videoAdNumber++
                    withContext(Dispatchers.Main) {
                        Timber.d("VideoAdsManager: showVideoAds() ad startAndWait")
                        ad.startAndWait()
                        Timber.d("VideoAdsManager: showVideoAds() ad startAndWait completed ${ad.isSuccessful()}")
                        if (!ad.isSuccessful()) {
                            onAdNotLoaded.postValue(reason)
                        }
                    }
                } catch (e: CancellationException) {
                    Timber.d("VideoAdsManager: showVideoAds() CancellationException")
                } finally {
                    Timber.d("VideoAdsManager: showVideoAds() finally")
                    lastAd?.release()
                }
            }

            Timber.d("VideoAdsManager: showVideoAds() all ads completed")
            continuation.resumeWithSafe(Result.success(true))
            executorXCueDuration?.shutdownNow()
        }
        showVideoAdsJob?.start()
    }

    private fun createVideoAdsList(
        isEnableIma: Boolean,
        imaNumberAds: Int,
        isEnableInfomercial: Boolean,
        infomercialNumberAds: Int
    ): List<VideoAd> {
        val list = ArrayList<VideoAd>()
        if (isEnableIma && !rotationVideoAds.isEmptyIma()) {
            for (index in 1..imaNumberAds) {
                rotationVideoAds.getNextIma()?.let {
                    list.add(it)
                }
            }
        }
        if (isEnableInfomercial && !rotationVideoAds.isEmptyInfomercial()) {
            for (index in 1..infomercialNumberAds) {
                rotationVideoAds.getNextInfomercial()?.let {
                    list.add(it)
                }
            }
        }
        return list.sortedByDescending { it.priority() }
    }

    private fun createVideoAdsList(
        setupVideoAd: SetupVideoAd,
        reason: VideoAdTrigger
    ): List<VideoAd> {
        val isEnableIma = setupVideoAd.imaVast().enable()
        val imaNumberAds = setupVideoAd.imaVast().numberOfAdsToServe()

        val isEnableInfomercial = setupVideoAd.infomercial().enable()
        val infomercialNumberAds = setupVideoAd.infomercial().numberOfAdsToServe()

        val videoAdsList = createVideoAdsList(
            isEnableIma, imaNumberAds,
            isEnableInfomercial, infomercialNumberAds
        )
        videoAdsList.forEach {
            it.setExtra(reason.reasonName)
        }
        return videoAdsList
    }

    open fun prepareVastUrl(inputUrl: String?): String? {
        return inputUrl
    }
}