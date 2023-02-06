package com.kokoconnect.android.vm.tv

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.model.player.Language
import com.kokoconnect.android.model.tv.ProgramDescription
import com.kokoconnect.android.model.player.VideoOpeningReason
import com.kokoconnect.android.model.ads.AdsStatus
import com.kokoconnect.android.model.ads.video.*
import com.kokoconnect.android.model.error.PlayerError
import com.kokoconnect.android.model.error.YouTubePlayerError
import com.kokoconnect.android.model.player.*
import com.kokoconnect.android.model.player.proxy.*
import com.kokoconnect.android.model.player.proxy.chromecast.ChromecastConnectionState
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.vm.PlayerViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

class TvPlayersViewModel @Inject constructor(
    val app: AiryTvApp,
    private val airyRepo: AiryRepository
) : ViewModel(), LifecycleObserver {
    private var context = app
    private var playerViewModel: PlayerViewModel? = null
    private var isCurrent: Boolean = false
    private val ams: AmsEventsFacade
        get() {
            return app.ams
        }

    private val playersManagerListener = object : PlayersManager.Listener {
        override fun onPlayerSwitched(playerProxy: InnerPlayerProxy) {
            needSwitchPlayer.postValue(playerProxy.getType())
        }

        override fun onNeedRequestNextVideo() {
            needOpenNextContent.postValue(true)
        }

        override fun onNeedReloadVideo() {
            Timber.d("onNeedReloadVideo()")
            playersManager.reopenContent()
        }

        override fun onNeedSwitchFullscreen() {
            playerViewModel?.switchFullscreen()
        }

        override fun onBufferingProgressEnabled(isEnabled: Boolean) {
            needShowBufferingProgress.postValue(isEnabled)
        }

        override fun onDetectedCueTones(cueTones: String) {
            videoAdsManager.onDetectedCueTones(cueTones, currentTvProgramDescription?.channelId)
        }

        override fun onError(error: PlayerError) {
            when {
                error is YouTubePlayerError -> {
                    error.prepareException(context, currentTvProgramDescription)
                    error.exception?.let {
                        FirebaseCrashlytics.getInstance().recordException(it)
                    }
                }
                else -> {
                    needShowError.postValue(error)
                }
            }
        }

        override fun onPositionUpdated(positionMs: Long) {}

        override fun onAvailableLanguagesUpdated(languages: List<Language>) {}

        override fun onChromecastConnectionUpdated(chromecastConnectionState: ChromecastConnectionState) {
            if (AppParams.isChromecastTvEnabled) {
                this@TvPlayersViewModel.chromecastConnectionState.value = chromecastConnectionState
                when (chromecastConnectionState) {
                    ChromecastConnectionState.DISCONNECTED -> {
                        Preferences(context).Guide().clearChromecastData()
                        needReopenCurrentContent?.postValue(true)
                    }
                    ChromecastConnectionState.CONNECTED -> {
                        stop()
                        if (isCurrent) {
                            reopenContent(VideoOpeningReason.ON_CHROMECAST_CONNECTED)
                            ams.sendBrowserEvent("airy://chromecast")
                        }
                    }
                }
            }
        }
    }
    private val playersManager: PlayersManager = PlayersManager(app, playersManagerListener)

    private var lastTvProgramDescription: ProgramDescription? = null
    private var currentTvProgramDescription: ProgramDescription? = null
    private var lastContent: PlayerObject? = null
    private var currentContent: PlayerObject? = null

    val currentPositionMs: LiveData<Long> = playersManager.currentPositionMs

    val needSwitchPlayer: MutableLiveData<PlayerType> = MutableLiveData<PlayerType>()
    val needOpenNextContent: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    val needReopenCurrentContent: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    val needShowError = MutableLiveData<PlayerError>()
    var needShowBufferingProgress = MutableLiveData<Boolean>().apply { postValue(false) }
    var availableSubtitlesLanguages = playersManager.availableSubtitlesLanguages
    val currentSubtitlesLanguage = playersManager.currentSubtitlesLanguage
    var subtitlesEnabled = playersManager.subtitlesEnabled
    var subtitlesExists = playersManager.subtitlesExists

    val chromecastConnectionState =
        MutableLiveData<ChromecastConnectionState>(playersManager.chromecastConnectionState)
    val isChromecastConnected = chromecastConnectionState.map {
        it == ChromecastConnectionState.CONNECTED && AppParams.isChromecastTvEnabled
    }

    private var onNeedShowVideoAd = MutableLiveData<VideoAdLoader?>()
    private var onReturnStream = MutableLiveData<Boolean>()
    var onCurrentAdNumber = MutableLiveData<VideoAdNumber?>()
    var onAdNotLoaded = MutableLiveData<VideoAdTrigger?>()
    val videoAdsManager = TvVideoAdsManager(
        app,
        app.ams,
        airyRepo,
        onNeedShowVideoAd,
        onAdNotLoaded,
        onCurrentAdNumber,
        onReturnStream
    )

    init {
        chromecastConnectionState.observeForever {
            Timber.d("chromecastConnectionState = ${chromecastConnectionState.value?.name}")
        }
        currentPositionMs.observeForever {
            Timber.d("currentPositionMs = ${it}")
            videoAdsManager.onCurrentPositionChanged(it)
        }
    }

    fun init(
        playerViewModel: PlayerViewModel
    ) {
        this.playerViewModel = playerViewModel
        this.playerViewModel?.tvPlayersViewModel = this
    }

    fun initVideoAd(
        activity: FragmentActivity,
        playerLifecycleOwner: LifecycleOwner,
        videoAdTvGuideInfo: VideoAdTvGuideInfo?
    ) {
        videoAdsManager.init(activity, videoAdTvGuideInfo)
        onNeedShowVideoAd.observe(playerLifecycleOwner, Observer {
            if (it != null) {
                Timber.d("onNeedShowVideoAd.observe")
                openContent(it, VideoOpeningReason.ON_NEED_SHOW_VIDEO_AD)
                onNeedShowVideoAd.postValue(null)
            }
        })
        onReturnStream.observe(playerLifecycleOwner, Observer {
            if (it != null && it != false) {
                Timber.d("onReturnStream.observe $currentContent")
                reopenContent(VideoOpeningReason.ON_AFTER_VIDEO_AD)
                onReturnStream.postValue(false)
            }
        })
    }

    fun setupPlayers(
        youtubeProxyParams: YouTubeProxy.Params?,
        exoProxyParams: ExoPlayerProxy.Params?,
        dailymotionProxyParams: DailymotionProxy.Params?,
        webPlayerProxyParams: WebPlayerProxy.Params?,
        adsProxyParams: AdsProxy.Params?,
        lifecycle: Lifecycle
    ) {
        Timber.d("setupPlayers()")
        playersManager.setup(
            youtubeProxyParams,
            exoProxyParams,
            dailymotionProxyParams,
            webPlayerProxyParams,
            adsProxyParams,
            lifecycle
        )
        if (currentContent != null && currentTvProgramDescription != null) {
            // screen rotation for example
            reopenContent(VideoOpeningReason.ON_RECREATE_ACTIVITY)
        }
    }

    fun setCurrent(isCurrent: Boolean) {
        this.isCurrent = isCurrent
    }

    fun setAds(adsStatus: AdsStatus?) {
        videoAdsManager.setAds(adsStatus)
    }

    fun setProgramAds(imaProgramAds: ImaProgramAds?) {
        videoAdsManager.setProgramAds(imaProgramAds)
    }

    fun initChromecast(builder: ChromecastProxyBuilder) {
        playersManager.initChromecast(builder)
    }

    fun isChromecastConnected(): Boolean {
        return isChromecastConnected.value ?: false
    }

    fun reopenContent(openingReason: VideoOpeningReason = VideoOpeningReason.ON_RESUME) {
        openContent(
            currentContent,
            openingReason,
            currentTvProgramDescription
        )
    }

    fun openContent(
        playerObj: PlayerObject?,
        videoOpeningReason: VideoOpeningReason,
        videoProgramDescription: ProgramDescription? = null
    ) {
        val playerObject: PlayerObject = playerObj ?: return
        Timber.d(
            "openContent(), " +
                    "opening reason: ${videoOpeningReason}  " +
                    "chromecastListener = ${this}" +
                    "playerObj == ${playerObj}" +
                    "playerObj.position == ${playerObj.getStartPosition()}"
        )

        var isNeedUpdateContent = !arrayOf(
            VideoOpeningReason.ON_RECREATE_ACTIVITY,
            VideoOpeningReason.ON_NEED_SHOW_VIDEO_AD,
            VideoOpeningReason.ON_AFTER_VIDEO_AD
        ).contains(videoOpeningReason)
        isNeedUpdateContent = isNeedUpdateContent && !playerObject.isAd()

        Timber.d("openContent() isNeedUpdateGuideContent == $isNeedUpdateContent")
        if (isNeedUpdateContent) {
            videoAdsManager.cancelShowVideoAds()
            lastTvProgramDescription = currentTvProgramDescription
            lastContent = currentContent
            currentContent = playerObject
            currentTvProgramDescription = videoProgramDescription
        }

        val isNeedReopenVideo = videoOpeningReason == VideoOpeningReason.ON_RECREATE_ACTIVITY
                || videoOpeningReason == VideoOpeningReason.ON_RESUME
        val isContentChanged = videoOpeningReason == VideoOpeningReason.ON_TV_CHANNEL_CHANGE
                || videoOpeningReason == VideoOpeningReason.ON_TV_FIRST_TUNE
                || videoOpeningReason == VideoOpeningReason.ON_TV_PROGRAM_CHANGE
                || videoOpeningReason == VideoOpeningReason.ON_TV_PROGRAM_PART_CHANGE


        if (!isChromecastConnected()) {
            if (isNeedReopenVideo) {
                videoProgramDescription?.videoOpeningReason = VideoOpeningReason.ON_RESUME
            }
            if (isContentChanged || isNeedReopenVideo) {
                //release cue tones timestamp
                videoAdsManager.releaseCueTonesAd()
            }
            Timber.d("openContent() isNeedReopenVideo == $isNeedReopenVideo")
            playersManager.openContent(playerObject, videoOpeningReason)
            when (videoOpeningReason) {
                VideoOpeningReason.ON_TV_FIRST_TUNE -> {
                    videoProgramDescription?.videoOpeningReason = VideoOpeningReason.ON_RESUME
                    videoAdsManager.onFirstTune()
                    sendTvWatchEvent()
                }
                VideoOpeningReason.ON_TV_CHANNEL_CHANGE -> {
                    videoProgramDescription?.videoOpeningReason = VideoOpeningReason.ON_RESUME
                    videoAdsManager.onChannelChange()
                    sendTvWatchEvent()
                }
                VideoOpeningReason.ON_TV_PROGRAM_CHANGE -> {
                    if (videoProgramDescription?.isNotStream() == true) {
                        videoProgramDescription.videoOpeningReason = VideoOpeningReason.ON_RESUME
                        videoAdsManager.onProgramChange()
                    }
                }
            }
        } else {
            stop()
            Timber.d("openContent() сhromecast is active, open channel")
            videoProgramDescription?.let { playersManager.openChromecastContent(it) }
        }
    }

    private fun sendTvWatchEvent() {
        if (currentContent?.isAd() == true
            || currentContent?.getUrl()?.isEmpty() == true
            || currentContent?.getUrl() == lastContent?.getUrl()
        ) {
            return
        }

        val lastVideoUrl = lastContent?.getUrl()
        val currentVideoUrl = currentContent?.getUrl()
        if (lastVideoUrl != null) {
            // send last opened video url (end)
            Timber.d("sendWatchEvent() for lastDescription")
            ams.sendWatchEvent(lastVideoUrl, false)
        } else if (currentVideoUrl != null) {
            // send current opened video url (start)
            Timber.d("sendWatchEvent() for currentDescription")
            ams.sendWatchEvent(currentVideoUrl, true)
        }
    }

    fun setSecurityData(securityData: SecurityData?) {
        playersManager.setSecurityData(securityData)
    }

    fun pause() {
        playersManager.pause()
    }

    fun play() {
        playersManager.play()
    }

    fun stop() {
        playersManager.stop()
        videoAdsManager.cancelShowVideoAds()
    }

    fun setSubtitilesEnabled(enabled: Boolean) {
        playersManager.setSubtitilesEnabled(enabled)
    }

    fun isSubtitlesEnabled(): Boolean {
        return playersManager.isSubtitlesEnabled()
    }

    fun isSubtitlesExists(): Boolean {
        return playersManager.isSubtitlesExists()
    }

    fun setSubtitlesLanguage(language: Language?) {
        playersManager.setSubtitlesLanguage(language)
    }

    fun switchSubtitles() {
        playersManager.switchSubtitles()
    }

    override fun onCleared() {
        Timber.d("onCleared()")
        videoAdsManager.cancelShowVideoAds()
        videoAdsManager.stopTimer()
        super.onCleared()
    }

}