package com.kokoconnect.android.vm.vod

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.model.player.Language
import com.kokoconnect.android.model.player.VideoOpeningReason
import com.kokoconnect.android.model.ads.AdsStatus
import com.kokoconnect.android.model.ads.video.VideoAdTrigger
import com.kokoconnect.android.model.ads.video.VideoAdNumber
import com.kokoconnect.android.model.ads.video.VodVideoAdsManager
import com.kokoconnect.android.model.error.EmptyPlayerError
import com.kokoconnect.android.model.error.PlayerError
import com.kokoconnect.android.model.error.YouTubePlayerError
import com.kokoconnect.android.model.player.*
import com.kokoconnect.android.model.player.proxy.*
import com.kokoconnect.android.model.player.proxy.chromecast.ChromecastConnectionState
import com.kokoconnect.android.model.vod.Content
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.vm.PlayerViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

class VodPlayersViewModel @Inject constructor(
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

        override fun onDetectedCueTones(cueTones: String) {}

        override fun onError(error: PlayerError) {
            when {
                error is YouTubePlayerError -> {
                    error.prepareException(context, currentVodContent)
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
            if (AppParams.isChromecastVodEnabled) {
                this@VodPlayersViewModel.chromecastConnectionState.value = chromecastConnectionState
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

    private var lastVodContent: Content? = null
    private var currentVodContent: Content? = null
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

    val chromecastConnectionState = MutableLiveData<ChromecastConnectionState>(playersManager.chromecastConnectionState)
    val isChromecastConnected = chromecastConnectionState.map {
        it == ChromecastConnectionState.CONNECTED && AppParams.isChromecastVodEnabled
    }

    private var onNeedShowVideoAd = MutableLiveData<VideoAdLoader?>()
    private var onReturnStream = MutableLiveData<Boolean>()
    var onCurrentAdNumber = MutableLiveData<VideoAdNumber?>()
    var onAdNotLoaded = MutableLiveData<VideoAdTrigger?>()
    private val videoAdsManager = VodVideoAdsManager(
        app,
        app.ams,
        airyRepo,
        onNeedShowVideoAd,
        onAdNotLoaded,
        onCurrentAdNumber,
        onReturnStream
    )

    init {
        currentPositionMs.observeForever {
            Timber.d("currentPositionMs = ${it}")
        }
    }

    fun init(playerViewModel: PlayerViewModel) {
        this.playerViewModel = playerViewModel
        this.playerViewModel?.vodPlayersViewModel = this
    }

    fun initVideoAd(
        activity: FragmentActivity,
        playerLifecycleOwner: LifecycleOwner,
        videoAdContentInfo: VideoAdVodContentInfo
    ) {
        videoAdsManager.init(activity, videoAdContentInfo)
        onNeedShowVideoAd.observe(playerLifecycleOwner, Observer {
            if (it != null) {
                Timber.d("onNeedShowVideoAd.observe")
                openContent(it, VideoOpeningReason.ON_NEED_SHOW_VIDEO_AD, null)
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
        if (currentContent != null) {
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

    fun initChromecast(builder: ChromecastProxyBuilder) {
        playersManager.initChromecast(builder)
    }

    fun isChromecastConnected(): Boolean {
        return isChromecastConnected.value ?: false
    }

    fun reopenContent(videoOpeningReason: VideoOpeningReason = VideoOpeningReason.ON_RESUME) {
        openContent(currentContent, videoOpeningReason, currentVodContent)
    }

    fun openContent(
        playerObject: PlayerObject?,
        videoOpeningReason: VideoOpeningReason,
        content: Content?
    ) {
        playerObject ?: return
        Timber.d(
            "openContent(), " +
                    "opening reason: ${videoOpeningReason}  " +
                    "playerObj == ${playerObject}" +
                    "playerObj.position == ${playerObject.getStartPosition()}"
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
            lastContent = currentContent
            currentContent = playerObject
            lastVodContent = currentVodContent
            currentVodContent = content
        }
        //release cue tones timestamp
        if (!isChromecastConnected()) {
            playersManager.openContent(playerObject, videoOpeningReason)
        } else {
            stop()
            content?.let {
                playersManager.openChromecastContent(content)
            }
        }
        needShowError.postValue(EmptyPlayerError())
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
        videoAdsManager.cancelShowVideoAds()
        videoAdsManager.stopTimer()
        super.onCleared()
    }
}