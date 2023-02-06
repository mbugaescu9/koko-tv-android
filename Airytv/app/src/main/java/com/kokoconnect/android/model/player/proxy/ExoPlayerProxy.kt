package com.kokoconnect.android.model.player.proxy

import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.kokoconnect.android.model.player.Language
import com.kokoconnect.android.model.error.EmptyPlayerError
import com.kokoconnect.android.model.error.PlayerError
import com.kokoconnect.android.model.error.PlayerErrorType
import com.kokoconnect.android.model.player.*
import com.kokoconnect.android.util.DateUtils
import com.kokoconnect.android.util.NetworkUtils
import com.kokoconnect.android.util.isOrientationLandscape
import com.kokoconnect.android.util.isOrientationPortrait
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.hls.DefaultHlsExtractorFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import org.jetbrains.anko.dip
import timber.log.Timber


class ExoPlayerProxy(val listener: PlayerProxyListener) : InnerPlayerProxy() {
    class Params(
        override var playerView: View?,
        override var uiControllerEnabled: Boolean
    ) : InnerPlayerProxy.Params()

    private var isStopWithErrorValue: Boolean = true
    private var securityData: SecurityData? = null
    private var exoplayer: SimpleExoPlayer? = null
    private var currentMediaSource: MediaSource? = null
    private var exoPlayerView: PlayerView? = null
    private var uiControllerEnabled: Boolean = false

    private var defaultBandwidthMeter: DefaultBandwidthMeter? = null
    private var trackSelector: DefaultTrackSelector? = null

    var lifecyclePaused = false
    var isPaused = false
    var currentExoplayerPosition = -1L
    var currentPositionLoaded = false
    var currentContent: PlayerObject? = null
    private var isFocus: Boolean = false

    private val exoplayerSubtitlesListener = object : Player.EventListener {
        fun getSupportedLanguages(): List<Language> {
            val languages = mutableListOf<Language>()
            val mappedTrackInfo = trackSelector?.currentMappedTrackInfo ?: return languages
            val trackGroups =
                mappedTrackInfo.getTrackGroups(C.TRACK_TYPE_VIDEO) ?: return languages
            for (trackGroupIdx in 0 until trackGroups.length) {
                val trackGroup = trackGroups.get(trackGroupIdx)
                for (trackIdx in 0 until trackGroup.length) {
                    trackGroup.getFormat(trackIdx).language?.let {
                        languages.add(Language(it))
                    }
                }
            }
            return languages
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
            listener.onAvailableLanguagesUpdated(this@ExoPlayerProxy, getSupportedLanguages())
        }
    }
    private val exoplayerBufferingListener = object :
        Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Timber.d("ExoPlayerProxy: onPlayerStateChanged() playWhenReady == $playWhenReady")
            when (playbackState) {
                ExoPlayer.STATE_READY -> {
                    Timber.d("ProblemWithPlayerProxyListener: exoplayer isStopWithErrorValue = false")
                    isStopWithErrorValue = false
                    listener.onBufferingProgressEnabled(this@ExoPlayerProxy, false)
                }
                ExoPlayer.STATE_IDLE -> {
                    Timber.d("ProblemWithPlayerProxyListener: exoplayer isStopWithErrorValue = true")
                    isStopWithErrorValue = true
                    listener.onBufferingProgressEnabled(this@ExoPlayerProxy, false)
                }
                else -> {
                    listener.onBufferingProgressEnabled(this@ExoPlayerProxy, true)
                }
            }
        }
    }
    private val exoplayerDropErrorListener = object :
        Player.EventListener {
        override fun onPlayerStateChanged(
            playWhenReady: Boolean,
            playbackState: Int
        ) {
            if (exoplayer?.playWhenReady == true &&
                (playbackState == Player.STATE_BUFFERING
                        || playbackState == Player.STATE_READY)
            ) {
                Timber.d("ProblemWithPlayerProxyListener: exoplayer onPlayerStateChanged Player.STATE_READY")
                listener.onError(this@ExoPlayerProxy, EmptyPlayerError())
            } else if (playbackState == Player.STATE_IDLE) {
                Timber.d("ProblemWithPlayerProxyListener: exoplayer onPlayerStateChanged Player.STATE_IDLE")
                val type = if(NetworkUtils.isInternetAvailable()) {
                    PlayerErrorType.EXOPLAYER_PLAYER_STATE_IDLE
                } else {
                    PlayerErrorType.NO_INTERNET_CONNECTION
                }
                listener.onError(
                    this@ExoPlayerProxy,
                    PlayerError(type)
                )
            }
        }
    }
    private val exoplayerMp4Listeners = listOf(
        object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (currentExoplayerPosition != -1L) {
                            loadCurrentPosition()
                            exoplayer?.playWhenReady = !isPaused
                            exoplayer?.removeListener(this)
                        }
                    }
                }
            }
        },
        object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                Timber.d("playersViewModel onPlayerStateChanged() playWhenReady = $playWhenReady playbackState = ${playbackState}")
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        if (currentContent is Video) {
                            Timber.d("ExoplayerProxy onPlayerStateChanged() STATE_ENDED")
                            listener.onNeedRequestNextVideo(this@ExoPlayerProxy)
                        }
                        currentContent = null
                    }
                }
            }
        }
    )

    private fun getContext() = exoPlayerView?.context!!

    override fun setup(params: InnerPlayerProxy.Params?) {
        val exoplayerParams = params as? Params ?: return
        setPlayerView(exoplayerParams.playerView)
        setUiControllerEnabled(exoplayerParams.uiControllerEnabled)
    }

    override fun setPlayerView(playerView: View?) {
        exoPlayerView = playerView as? PlayerView
        exoPlayerView?.useController = uiControllerEnabled
        if (exoPlayerView?.context?.isOrientationPortrait() == true) {
            exoPlayerView?.layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    getContext().dip(230)
                )
        }
        Timber.d("ExoPlayerProxy: setPlayerView() $exoPlayerView")
    }

    override fun setUiControllerEnabled(isEnabled: Boolean) {
        uiControllerEnabled = isEnabled
        exoPlayerView?.useController = uiControllerEnabled
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onStart() {
        Timber.d("ExoPlayerProxy: Lifecycle.Event.ON_START start")

        exoPlayerView?.let {
            val exoPlayerView = it
            defaultBandwidthMeter =
                DefaultBandwidthMeter.getSingletonInstance(exoPlayerView.context)
            trackSelector = DefaultTrackSelector(exoPlayerView.context)
            val exoplayerBuilder = SimpleExoPlayer.Builder(exoPlayerView.context)
            defaultBandwidthMeter?.let {
                exoplayerBuilder.setBandwidthMeter(it)
            }
            trackSelector?.let {
                exoplayerBuilder.setTrackSelector(it)
            }
            exoplayer = exoplayerBuilder.build()
            exoplayer?.addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    super.onPlayerStateChanged(playWhenReady, playbackState)
                    when (playbackState) {
                        Player.STATE_READY -> {
                            exoPlayerView.layoutParams =
                                if (getContext().isOrientationLandscape()) {
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                } else {
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT
                                    )
                                }
                        }
                        Player.STATE_IDLE -> {
                            if (getContext().isOrientationPortrait()) {
                                exoPlayerView.layoutParams =
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        getContext().dip(230)
                                    )
                            }
                        }
                    }
                }
            })
            exoplayer?.addListener(exoplayerBufferingListener)
            exoPlayerView.player = exoplayer
            exoplayer?.addListener(exoplayerSubtitlesListener)
            exoPlayerView.subtitleView?.visibility = View.GONE
            exoplayer?.addTextOutput { cues ->
//                Timber.d("textOutput ${cues.map { it.text }.joinToString()}")
                exoPlayerView.subtitleView?.setCues(cues)
            }
        }
        Timber.d("ExoPlayerProxy: Lifecycle.Event.ON_START finish")
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        try {
            exoplayerMp4Listeners.forEach { exoplayer?.addListener(it) }
        } catch (e: java.lang.Exception) {/* ignore */
        }
        Timber.d("ExoPlayerProxy: Lifecycle.Event.ON_RESUME")
        isFocus = true

        if (lifecyclePaused) {
            lifecyclePaused = false
            exoplayer?.playWhenReady = !isPaused
        } else {
            currentContent?.let {
                when (it) {
                    is HlsStream -> {
                        loadHlsVideo(it.videoUrl)
                    }
                    is Mpeg4Video -> {
                        if (it.isSecured) {
                            loadMp4SecuredVideo(it.videoUrl, currentExoplayerPosition, securityData)
                        } else {
                            loadMp4Video(it.videoUrl, currentExoplayerPosition)
                        }
                    }
                    is VideoAdLoader -> {
                        listener.onNeedReloadVideo(this)
                    }
                }
            } ?: listener.onNeedReloadVideo(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Timber.d("ExoPlayerProxy: Lifecycle.Event.ON_PAUSE")
        isFocus = false
        lifecyclePaused = true
        saveCurrentPosition()
        try {
            exoplayerMp4Listeners.forEach { exoplayer?.removeListener(it) }
        } catch (e: java.lang.Exception) {/* ignore */
        }
        exoplayer?.playWhenReady = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onStop() {
        Timber.d("ExoPlayerProxy: Lifecycle.Event.ON_STOP")
        stop()
    }

    override fun openVideo(video: PlayerObject, ignoreCheck: Boolean) {
        Timber.d("ExoPlayerProxy: openVideo() url == ${video?.getUrl()} ignoreCheck == $ignoreCheck")
        Timber.d("ExoPlayerProxy: openVideo() newPosition = ${video.getStartPosition()} currentPosition = ${currentExoplayerPosition}")
        if (exoPlayerView == null) {
            Timber.d("ExoPlayerProxy: openVideo() view is null")
            listener.onError(this, PlayerError(PlayerErrorType.EXOPLAYER_PLAYER_UNAVAILABLE))
            return
        }
        val videoUrl = video.getUrl()
        if (videoUrl == null || videoUrl.isEmpty()) return
        if (ignoreCheck || !videoUrl.contains(getCurrentVideoString())) {
            Timber.d("ExoPlayerProxy: openVideo() run playVideo()")
            playVideo(video)
            currentContent = video
        } else {
            Timber.d("ExoPlayerProxy: openVideo() video already open")
        }
    }

    override fun getCurrentPosition(): Long {
        return if (currentContent is Video) {
            val position = exoplayer?.contentPosition ?: 0
            if (currentPositionLoaded && position > 0) {
                currentExoplayerPosition = position
                Timber.d("getCurrentPosition()")
            }
            currentExoplayerPosition
//            if (!currentPositionLoaded){
//                Timber.d("getCurrentPosition() 2")
//                currentExoplayerPosition
//            } else {
//                Timber.d("getCurrentPosition() 3")
//                val value = currentStream?.getCurrentPosition() ?: 0
//                value
//            }
        } else if (currentContent is Stream) {
            val startTimeMs = currentContent?.getStartTimeMs()
            if (startTimeMs != null && startTimeMs > 0L) {
                DateUtils.getCurrentDate() - startTimeMs
            } else {
                0L
            }
        } else {
            0L
        }
    }

    private fun getCurrentVideoString(): String {
        return currentContent?.getUrl().toString()
    }

    private fun saveCurrentPosition() {
        if (currentContent is Mpeg4Video) {
            exoplayer?.currentPosition?.let { position: Long ->
                Timber.d("ExoPlayerProxy: saveCurrentPosition() ${position}")
                if (position > 0L) {
                    currentPositionLoaded = false
                    currentExoplayerPosition = position
                    listener.onPositionUpdated(this, position)
                }
            }
        }
    }

    private fun loadCurrentPosition() {
        Timber.d("ExoPlayerProxy: loadCurrentPosition() $currentExoplayerPosition already loaded = ${currentPositionLoaded}")
        if (currentContent is Mpeg4Video && !currentPositionLoaded) {
            currentPositionLoaded = true
            exoplayer?.seekTo(currentExoplayerPosition)
        }
    }

    private fun playVideo(it: PlayerObject?) {
        Timber.d("ExoPlayerProxy: playVideo() ${it} isFocus == $isFocus")
        lifecyclePaused = false
        isPaused = false
        currentExoplayerPosition = it?.getStartPosition()?.toLong() ?: -1L
        currentPositionLoaded = false
        if (isFocus) {
            when (it) {
                is Mpeg4Video -> {
                    if (it.isSecured) {
                        loadMp4SecuredVideo(it.videoUrl, it.seekToTime.toLong(), securityData)
                    } else {
                        loadMp4Video(it.videoUrl, it.seekToTime.toLong())
                    }
                }
                is HlsStream -> {
                    loadHlsVideo(it.videoUrl)
                }
            }
        }
    }

    override fun isPlaying(): Boolean {
        return !isPaused && isFocus
    }

    override fun play() {
        Timber.d("ExoPlayerProxy: ExoPlayerProxy resume()")
        isPaused = false
        loadCurrentPosition()
        exoplayer?.apply {
            this.playWhenReady = true
            addDropErrorListener()
        }
    }

    override fun pause() {
        Timber.d("ExoPlayerProxy: ExoPlayerProxy pause()")
        isPaused = true
        saveCurrentPosition()
        exoplayer?.apply {
            this.playWhenReady = false
            removeDropErrorListener()
        }
    }

    override fun stop() {
        lifecyclePaused = false
        exoplayer?.apply {
            playWhenReady = false
            stop()
            removeDropErrorListener()
        }
    }

    private fun removeDropErrorListener() {
        Timber.d("ProblemWithPlayerProxyListener: removeListener(exoplayerDropErrorListener)")
        exoplayer?.removeListener(exoplayerDropErrorListener)
    }

    override fun release() {
        Timber.d("ExoPlayerProxy: release()")
        stop()
        currentContent = null
    }

    override fun isStopWithError(): Boolean = isStopWithErrorValue

    override fun getType(): PlayerType = PlayerType.EXOPLAYER

    fun setSecurityData(securityData: SecurityData?) {
        this.securityData = securityData
    }

    fun setSubtitlesLanguage(language: Language?) {
        if (language != null) {
            exoPlayerView?.subtitleView?.visibility = View.VISIBLE
            trackSelector?.let {
                val trackSelectorParams = it.buildUponParameters()
                trackSelectorParams.setPreferredTextLanguage(language.code)
                it.parameters = trackSelectorParams.build()
            }
        } else {
            exoPlayerView?.subtitleView?.visibility = View.GONE
        }
    }

    private fun loadHlsVideo(url: String?) {
        Timber.d("ExoPlayerProxy: ExoPlayerProxy loadHlsVideo($url) ")
//        var url = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
        try {
            val dataSourceFactory: DataSource.Factory =
                DefaultDataSourceFactory(
                    getContext(),
                    Util.getUserAgent(getContext(), "Exo2"),
                    defaultBandwidthMeter
                )
            val defaultHlsExtractorFactory =
                DefaultHlsExtractorFactory(
                    DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES,
                    true
                )
            val mediaSource = HlsMediaSource.Factory(
                dataSourceFactory
            )
                .setExtractorFactory(defaultHlsExtractorFactory)
                .setXCueOutListener { tag ->
                    Timber.d("ExoPlayerProxy: onDetectedCueTones() cue detected $tag")
                    listener.onDetectedCueTones(this, tag)
                }
                .createMediaSource(Uri.parse(url))
            currentMediaSource = mediaSource
            addDropErrorListener()
            exoplayer?.prepare(mediaSource)
            exoplayer?.playWhenReady = !isPaused
        } catch (e: Exception) {
            e.printStackTrace()
            // ignore
        } finally {
            exoplayerMp4Listeners.forEach { exoplayer?.removeListener(it) }
        }
    }

    private fun loadMp4Video(videoUrl: String?, seekToTime: Long) {
        try {
            Timber.d("ExoPlayerProxy: loadMp4Video(${videoUrl}, ${seekToTime})")
            val dataSourceFactory: DataSource.Factory =
                DefaultDataSourceFactory(
                    exoPlayerView?.context,
                    Util.getUserAgent(getContext(), "Exo2")
                )
            val mediaSource = ProgressiveMediaSource.Factory(
                dataSourceFactory
            )
                .createMediaSource(Uri.parse(videoUrl))

            currentMediaSource = mediaSource
            addDropErrorListener()
            exoplayerMp4Listeners.forEach { exoplayer?.addListener(it) }
            exoplayer?.prepare(mediaSource)
            exoplayer?.playWhenReady = !isPaused
        } catch (e: Exception) {
            e.printStackTrace()
            // ignore
        }
    }

    private fun addDropErrorListener() {
        Timber.d("ProblemWithPlayerProxyListener: addListener(exoplayerDropErrorListener)")
        exoplayer?.addListener(exoplayerDropErrorListener)
    }

    private fun loadMp4SecuredVideo(
        videoUrl: String?,
        seekToTime: Long,
        securityData: SecurityData?
    ) {
        try {
            Timber.d("ExoPlayerProxy: loadMp4SecuredVideo(${videoUrl}, ${seekToTime})")
            val dataSourceFactory =
                DefaultHttpDataSourceFactory(
                    Util.getUserAgent(getContext(), "Exo2")
                )
            if (securityData?.authKey?.header?.isNotEmpty() == true) {
                dataSourceFactory.defaultRequestProperties.set(
                    "Authorization",
                    securityData.authKey.header
                )
            } else if (securityData?.cookie?.isNotEmpty() == true) {
                dataSourceFactory.defaultRequestProperties.set("Cookie", securityData.cookie)
            }
            val mediaSource = ProgressiveMediaSource.Factory(
                dataSourceFactory
            )
                .createMediaSource(Uri.parse(videoUrl))

            currentMediaSource = mediaSource
            addDropErrorListener()
            exoplayerMp4Listeners.forEach { exoplayer?.addListener(it) }
            exoplayer?.prepare(mediaSource)
            exoplayer?.playWhenReady = !isPaused
        } catch (e: Exception) {
            e.printStackTrace()
            // ignore
        }
    }

}
