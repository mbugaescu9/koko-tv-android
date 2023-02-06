package com.kokoconnect.android.model.player

import android.content.Context
import androidx.lifecycle.*
import com.kokoconnect.android.model.tv.ProgramDescription
import com.kokoconnect.android.model.error.PlayerError
import com.kokoconnect.android.model.player.proxy.*
import com.kokoconnect.android.model.player.proxy.chromecast.ChromecastConnectionState
import com.kokoconnect.android.model.vod.Content
import com.kokoconnect.android.util.UpdateTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class PlayersManager(
    val context: Context,
    val listener: Listener
) : LifecycleObserver {
    private val internalProxyListener = object : PlayerProxyListener {
        override fun onNeedRequestNextVideo(playerProxy: InnerPlayerProxy) {
            listener.onNeedRequestNextVideo()
        }

        override fun onNeedReloadVideo(playerProxy: InnerPlayerProxy) {
            listener.onNeedReloadVideo()
        }

        override fun onNeedSwitchFullscreen(playerProxy: InnerPlayerProxy) {
            listener.onNeedSwitchFullscreen()
        }

        override fun onBufferingProgressEnabled(playerProxy: InnerPlayerProxy, isEnabled: Boolean) {
            val currentProxy = getPlayerProxy(currentAnyContent)
            if (currentProxy?.getType() == playerProxy.getType()) {
                listener.onBufferingProgressEnabled(isEnabled)
            }
        }

        override fun onDetectedCueTones(playerProxy: InnerPlayerProxy, tag: String) {
            listener.onDetectedCueTones(tag)
        }

        override fun onError(playerProxy: InnerPlayerProxy, error: PlayerError) {
            Timber.w("PlayersViewModel: PlayerProxyListener.onError() proxy == ${playerProxy} error == ${error.type}")
            listener.onError(error)
        }

        override fun onPositionUpdated(playerProxy: InnerPlayerProxy, position: Long) {
            Timber.d("PlayersViewModel: PlayerProxyListener.updatePosition() proxy == ${playerProxy} position == ${position}")
            if (currentContentPositionMs != position) {
                listener?.onBufferingProgressEnabled(false)
            }
            currentContentPositionMs = position
        }

        override fun onAvailableLanguagesUpdated(
            playerProxy: InnerPlayerProxy,
            languages: List<Language>
        ) {
            Timber.d("Available languages ${languages.map { it.code }.joinToString()}")
            availableSubtitlesLanguages.postValue(languages)
            listener.onAvailableLanguagesUpdated(languages)
        }
    }

    private var lastContent: PlayerObject? = null
    private var currentContent: PlayerObject? = null
    private var currentContentPositionMs: Long = -1
    // includes ads video
    private var lastAnyContent: PlayerObject? = null
    private var currentAnyContent: PlayerObject? = null

    private var youtubeProxy = YouTubeProxy(internalProxyListener)
    private var dailymotionProxy = DailymotionProxy(internalProxyListener)
    private var exoPlayerProxy = ExoPlayerProxy(internalProxyListener)
    private var webPlayerProxy = WebPlayerProxy(internalProxyListener)
    private var adsProxy = AdsProxy(internalProxyListener)
    private var chromecastProxy: ChromecastProxy? = null
        set(value) {
            field = value
            if (value == null) {
                chromecastConnectionState = ChromecastConnectionState.DISCONNECTED
            }
        }
    var chromecastConnectionState: ChromecastConnectionState =
        ChromecastConnectionState.DISCONNECTED
    private val chromecastListener = object : ChromecastConnectionListener {
        override fun onChromecastConnecting() {
            Timber.d("PlayersViewModel: chromecastListener.onChromecastConnecting() chromecastListener = ${this}")
            chromecastConnectionState = ChromecastConnectionState.CONNECTING
            listener.onChromecastConnectionUpdated(chromecastConnectionState)
        }

        override fun onChromecastConnected(chromecast: ChromecastProxy) {
            Timber.d("PlayersViewModel: chromecastListener.onChromecastConnected() chromecastListener = ${this}")
            chromecastConnectionState = ChromecastConnectionState.CONNECTED
            chromecastProxy = chromecast
            listener.onChromecastConnectionUpdated(chromecastConnectionState)
        }

        override fun onChromecastDisconnected() {
            Timber.d("PlayersViewModel: chromecastListener.onChromecastDisconnected() chromecastListener = ${this}")
            chromecastConnectionState = ChromecastConnectionState.DISCONNECTED
            chromecastProxy = null
            listener.onChromecastConnectionUpdated(chromecastConnectionState)
        }
    }

    private val needUpdateCurrentPosition = MutableLiveData<Boolean>()
    private var currentPositionTimer = UpdateTimer(needUpdateCurrentPosition)
    val currentPositionMs: LiveData<Long> = needUpdateCurrentPosition.map {
        currentContentPositionMs = getCurrentVideoPosition()
        listener.onPositionUpdated(currentContentPositionMs)
        currentContentPositionMs
    }


    var availableSubtitlesLanguages = MutableLiveData<List<Language>?>()
    val currentSubtitlesLanguage: LiveData<Language?> =
        availableSubtitlesLanguages.map { languages ->
            if (languages != null) {
                val eng = languages.firstOrNull { it.code == "en" }
                if (isSubtitlesEnabled()) {
                    setSubtitlesLanguage(eng)
                }
                eng
            } else {
                null
            }
        }
    var subtitlesEnabled = MutableLiveData<Boolean>(false)
    var subtitlesExists = availableSubtitlesLanguages.map {
        it != null && it.isNotEmpty()
    }

    init {
        subtitlesEnabled.observeForever {
            if (it == true) {
                setSubtitlesLanguage(currentSubtitlesLanguage.value)
            } else {
                setSubtitlesLanguage(null)
            }
        }
        subtitlesExists.observeForever {
            Timber.d("subtitlesExists ${it}")
        }
        currentSubtitlesLanguage.observeForever {
            Timber.d("currentSubtitlesLanguage ${it}")
        }
    }

    fun setup(
        youtubeProxyParams: YouTubeProxy.Params?,
        exoProxyParams: ExoPlayerProxy.Params?,
        dailymotionProxyParams: DailymotionProxy.Params?,
        webPlayerProxyParams: WebPlayerProxy.Params?,
        adsProxyParams: AdsProxy.Params?,
        lifecycle: Lifecycle
    ) {
        youtubeProxy.setup(youtubeProxyParams)
        exoPlayerProxy.setup(exoProxyParams)
        dailymotionProxy.setup(dailymotionProxyParams)
        webPlayerProxy.setup(webPlayerProxyParams)
        adsProxy.setup(adsProxyParams)
        lifecycle.addObserver(this)
        lifecycle.addObserver(youtubeProxy)
        lifecycle.addObserver(exoPlayerProxy)
        lifecycle.addObserver(dailymotionProxy)
        lifecycle.addObserver(webPlayerProxy)
        lifecycle.addObserver(adsProxy)
    }


    fun initChromecast(builder: ChromecastProxyBuilder) {
        builder.initChromecastProxy(chromecastListener)
    }

    fun isChromecastConnected(): Boolean {
        return chromecastProxy != null && chromecastConnectionState == ChromecastConnectionState.CONNECTED
    }

    fun openChromecastContent(programDescription: ProgramDescription) {
        chromecastProxy?.openChannel(context, programDescription)
    }

    fun openChromecastContent(content: Content) {
        chromecastProxy?.openContent(context, content)
    }

    fun reopenContent(videoOpeningReason: VideoOpeningReason = VideoOpeningReason.ON_RESUME) {
        currentContent?.let {
            it.setStartPosition(currentContentPositionMs)
            openContent(it, videoOpeningReason)
        }
    }

    fun openContent(
        content: PlayerObject?,
        videoOpeningReason: VideoOpeningReason
    ) {
        content ?: return
        Timber.d("openVideoInternal() ${content.getUrl()} type ${content.javaClass.canonicalName}")

        lastContent = currentContent
        if (!content.isAd()) {
            currentContent = content
        }
        lastAnyContent = currentAnyContent
        currentAnyContent = content

        val isNeedReopenVideo = videoOpeningReason == VideoOpeningReason.ON_RECREATE_ACTIVITY
                || videoOpeningReason == VideoOpeningReason.ON_RESUME
        if (isNeedReopenVideo) {
            content.setStartPosition(currentContentPositionMs)
        }
        val isContentChanged = videoOpeningReason == VideoOpeningReason.ON_TV_CHANNEL_CHANGE
                || videoOpeningReason == VideoOpeningReason.ON_TV_FIRST_TUNE
                || videoOpeningReason == VideoOpeningReason.ON_TV_PROGRAM_CHANGE
                || videoOpeningReason == VideoOpeningReason.ON_TV_PROGRAM_PART_CHANGE
                || videoOpeningReason == VideoOpeningReason.ON_VOD_CONTENT_SWITCH

        val isBeforeAds = videoOpeningReason == VideoOpeningReason.ON_NEED_SHOW_VIDEO_AD
        val isAfterAds = videoOpeningReason == VideoOpeningReason.ON_AFTER_VIDEO_AD

        val lastPlayerProxy = getPlayerProxy(lastAnyContent)
        val nextPlayerProxy = getPlayerProxy(content)
        if (isContentChanged || isNeedReopenVideo) {
            lastPlayerProxy?.release()
            //open video in case if content changed or if we must reopen current video
            nextPlayerProxy?.openVideo(content, true)
        } else if (isBeforeAds) {
            lastPlayerProxy?.pause()
            //here we launch all video ads playback
            nextPlayerProxy?.openVideo(content, true)
        } else if (isAfterAds) {
            lastPlayerProxy?.release()
            if (content.isStream() || nextPlayerProxy?.isStopWithError() == true) {
                //we should reopen stream after ads to get actual stream position
                nextPlayerProxy?.openVideo(content, true)
            } else {
                //in case of ordinary video content, we can resume playback
                nextPlayerProxy?.play()
            }
        }
        if (nextPlayerProxy != null) {
            listener.onPlayerSwitched(nextPlayerProxy)
        }
        Timber.d("openVideoInternal() lastPlayerProxy: ${lastPlayerProxy?.getType()} nextPlayerProxy: ${nextPlayerProxy?.getType()}")
    }

    fun setSecurityData(securityData: SecurityData?) {
        exoPlayerProxy.setSecurityData(securityData)
    }

    fun isPlaying(): Boolean {
        return getPlayerProxy(currentAnyContent)?.isPlaying() == true
    }

    fun pause() {
        getPlayerProxy(currentContent)?.pause()
    }

    fun play() {
        if (chromecastProxy == null) {
            getPlayerProxy(currentContent)?.play()
        }
    }

    // use example when open chromecast
    fun stop() {
        getPlayerProxy(currentContent)?.stop()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        currentPositionTimer.start(1)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        currentPositionTimer.stop()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Timber.d("onDestroy() save position == ${getCurrentVideoPosition()}")
        currentContentPositionMs = getCurrentVideoPosition()
    }

    fun updatePlayer() {
        getPlayerProxy(currentAnyContent)?.let { listener.onPlayerSwitched(it) }
    }

    private fun getCurrentVideoPosition(): Long {
        return getPlayerProxy(currentContent)?.getCurrentPosition() ?: -1L
    }

    fun getCurrentContentPlayerProxy(): InnerPlayerProxy? {
        return getPlayerProxy(currentContent)
    }

    private fun getPlayerProxy(playerObject: PlayerObject?): InnerPlayerProxy? {
        return when (playerObject?.getPlayerType()) {
            PlayerType.AD -> adsProxy
            PlayerType.YOUTUBE -> youtubeProxy
            PlayerType.EXOPLAYER -> exoPlayerProxy
            PlayerType.DAILYMOTION -> dailymotionProxy
            PlayerType.WEB -> webPlayerProxy
            else -> null
        }
    }

    fun setSubtitilesEnabled(enabled: Boolean) {
        subtitlesEnabled.postValue(enabled)
    }

    fun isSubtitlesEnabled(): Boolean {
        return subtitlesEnabled.value ?: false
    }

    fun isSubtitlesExists(): Boolean {
        return subtitlesExists.value ?: false
    }

    fun setSubtitlesLanguage(language: Language?) {
        Timber.d("setSubtitlesLanguage() ${language?.code}")
        exoPlayerProxy.setSubtitlesLanguage(language)
    }

    fun switchSubtitles() {
        val subtitlesEnabled = isSubtitlesEnabled()
        setSubtitilesEnabled(!subtitlesEnabled)
    }


    private fun startTestCueTones() {
        GlobalScope.launch(Dispatchers.Main) {
            val tags = listOf(
                "#EXT-X-CUE:DURATION=\"40.467\"",
                "#EXT-X-CUE-OUT:DURATION=10.50",
                "#EXT-X-CUE-OUT-CONT:ElapsedTime=5.939,Duration=45.467",
                "#EXT-X-CUE-OUT-CONT: 8.308/30",
                "#EXT-X-CUE-OUT-CONT: 30",
                "#EXT-X-CUE-OUT:30.987"
            )
            var tagIdx = 0
            while (true) {
                delay(60000)
                listener.onDetectedCueTones(tags[tagIdx])
                tagIdx++
                if (tagIdx == tags.size) tagIdx = 0
            }
        }
    }

    interface Listener {
        fun onPlayerSwitched(playerProxy: InnerPlayerProxy)
        fun onNeedRequestNextVideo()
        fun onNeedReloadVideo()
        fun onNeedSwitchFullscreen()
        fun onBufferingProgressEnabled(isEnabled: Boolean)
        fun onDetectedCueTones(cueTones: String)
        fun onError(error: PlayerError)
        fun onPositionUpdated(positionMs: Long)
        fun onAvailableLanguagesUpdated(languages: List<Language>)
        fun onChromecastConnectionUpdated(chromecastConnectionState: ChromecastConnectionState)
    }
}