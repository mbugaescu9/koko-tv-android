package com.kokoconnect.android.model.player.proxy

import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.kokoconnect.android.model.ads.video.ImaVastVideoAd
import com.kokoconnect.android.model.ads.video.InfomercialVideoAd
import com.kokoconnect.android.model.error.EmptyPlayerError
import com.kokoconnect.android.model.player.*
import com.kokoconnect.android.util.isOrientationLandscape
import com.kokoconnect.android.util.isOrientationPortrait
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import org.jetbrains.anko.dip
import timber.log.Timber


class AdsProxy(val listener: PlayerProxyListener) : InnerPlayerProxy() {
    class Params(
        override var playerView: View?,
        override var uiControllerEnabled: Boolean
    ) : InnerPlayerProxy.Params()

    private var exoplayer: SimpleExoPlayer? = null
    private var currentMediaSource: MediaSource? = null
    private var exoPlayerView: PlayerView? = null
    private var uiControllerEnabled: Boolean = true

    private var defaultBandwidthMeter: DefaultBandwidthMeter? = null
    private var trackSelector: DefaultTrackSelector? = null

    var lifecyclePaused = false
    var isPaused = false
    var currentContent: PlayerObject? = null
    private var isFocus: Boolean = false

    private val exoplayerBufferingListener = object :
        Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Timber.d("AdsProxy: onPlayerStateChanged() playWhenReady == $playWhenReady")
            when (playbackState) {
                ExoPlayer.STATE_READY -> {
                    listener.onBufferingProgressEnabled(this@AdsProxy, false)
                }
                else -> {
                    listener.onBufferingProgressEnabled(this@AdsProxy, true)
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
                Timber.d("ProblemWithPlayerProxyListener: ima onPlayerStateChanged Player.STATE_READY")
                listener.onError(this@AdsProxy, EmptyPlayerError())
            }
        }
    }

    private fun getContext() = exoPlayerView?.context!!

    override fun setup(params: InnerPlayerProxy.Params?) {
        val adsParams = params as? Params ?: return
        setPlayerView(adsParams.playerView)
        setUiControllerEnabled(adsParams.uiControllerEnabled)
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
        Timber.d("AdsProxy: setPlayerView() $exoPlayerView")
    }

    override fun setUiControllerEnabled(isEnabled: Boolean) {
        uiControllerEnabled = isEnabled
        exoPlayerView?.useController = uiControllerEnabled
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        Timber.d("AdsProxy: Lifecycle.Event.ON_CREATE start")
        exoPlayerView?.let {
            val exoPlayerView = it
            defaultBandwidthMeter = DefaultBandwidthMeter.getSingletonInstance(getContext())
            trackSelector = DefaultTrackSelector(getContext())
            val exoplayerBuilder = SimpleExoPlayer.Builder(getContext())
            defaultBandwidthMeter?.let {
                exoplayerBuilder.setBandwidthMeter(it)
            }
            trackSelector?.let {
                exoplayerBuilder.setTrackSelector(it)
            }
            exoplayer = exoplayerBuilder.build()
            exoplayer?.addListener(exoplayerBufferingListener)
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
            exoPlayerView.player = exoplayer
            exoplayer?.addTextOutput { cues ->
                exoPlayerView.subtitleView?.setCues(cues)
            }
        }
        Timber.d("AdsProxy: Lifecycle.Event.ON_CREATE finish")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        Timber.d("AdsProxy: Lifecycle.Event.ON_RESUME")
        isFocus = true

        if (lifecyclePaused) {
            lifecyclePaused = false
            exoplayer?.playWhenReady = !isPaused
        } else {
            currentContent?.let {
                when (it) {
                    is VideoAdLoader -> {
                        listener.onNeedReloadVideo(this@AdsProxy)
                    }
                }
            } ?: listener.onNeedReloadVideo(this@AdsProxy)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Timber.d("AdsProxy: Lifecycle.Event.ON_PAUSE")
        isFocus = false
        lifecyclePaused = true
        exoplayer?.playWhenReady = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Timber.d("AdsProxy: Lifecycle.Event.ON_STOP")
        stop()
    }

    override fun openVideo(video: PlayerObject, ignoreCheck: Boolean) {
        Timber.d("AdsProxy: openVideo() url == ${video?.getUrl()} ignoreCheck == $ignoreCheck type ${video.getPlayerType()}")
        Timber.d("AdsProxy: openVideo() run playVideo()")
        playVideo(video)
        currentContent = video
    }

    override fun getCurrentPosition(): Long {
        return exoplayer?.contentPosition ?: 0
    }

    private fun getCurrentVideoString(): String {
        return currentContent?.getUrl().toString()
    }

    private fun playVideo(video: PlayerObject?) {
        Timber.d("AdsProxy: playVideo() ${video} isFocus == $isFocus")
        lifecyclePaused = false
        isPaused = false
        if (isFocus) {
            when (video) {
                is ImaVastVideoAd.ImaVastVideoAdLoader -> {
                    loadVastIma(video)
                }
                is InfomercialVideoAd.InfomercialVideoAdLoader -> {
                    loadInfomercial(video)
                }
            }
        }
    }

    override fun isPlaying(): Boolean {
        return !isPaused && isFocus
    }

    override fun play() {
        Timber.d("uprtjst: AdsProxy resume()")
        isPaused = false
        exoplayer?.apply {
            this.playWhenReady = true
        }
    }

    override fun pause() {
        Timber.d("uprtjst: AdsProxy pause()")
        isPaused = true
        exoplayer?.apply {
            this.playWhenReady = false
        }
    }

    override fun stop() {
        lifecyclePaused = false
        exoplayer?.apply {
            playWhenReady = false
            stop()
        }
    }

    override fun release() {
        Timber.d("AdsProxy: release()")
        stop()
        currentContent = null
        Timber.d("ProblemWithPlayerProxyListener: removeListener(imaDropErrorListener)")
        exoplayer?.removeListener(exoplayerDropErrorListener)
    }

    override fun isStopWithError(): Boolean = false

    override fun getType(): PlayerType = PlayerType.AD

    fun loadVastIma(imaVastVideoAd: ImaVastVideoAd.ImaVastVideoAdLoader) {
        val adsLoader: ImaAdsLoader? = imaVastVideoAd.imaVideoAd.imaLoader
        currentContent = imaVastVideoAd
        Timber.d("AdsProxy: loadVastIma()")
        if (isFocus && adsLoader != null && exoPlayerView != null) {
            try {
                Timber.d("loadVastIma() emptyPlaceholder")
                val dataSpec = DataSpec(Uri.parse("assets:///empty_placeholder.mp4"))
                val assetDataSource = AssetDataSource(getContext())
                try {
                    assetDataSource.open(dataSpec)
                } catch (e: AssetDataSource.AssetDataSourceException) {
                    e.printStackTrace()
                }
                val assetDataSourceFactory: DataSource.Factory =
                    DataSource.Factory { assetDataSource }
                val assetMediaSource = ProgressiveMediaSource.Factory(assetDataSourceFactory)
                    .createMediaSource(assetDataSource.uri)
                val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
                    getContext(),
                    Util.getUserAgent(getContext(), "Exo2")
                )
                val mediaSourceWithAds = AdsMediaSource(
                    assetMediaSource,
                    dataSourceFactory,
                    adsLoader,
                    exoPlayerView
                )
                adsLoader?.setPlayer(exoplayer)
                exoplayer?.prepare(mediaSourceWithAds)
                Timber.d("ProblemWithPlayerProxyListener: addListener(imaDropErrorListener)")
                exoplayer?.addListener(exoplayerDropErrorListener)
                exoplayer?.addListener(object : Player.EventListener {
                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        super.onPlayerStateChanged(playWhenReady, playbackState)
                        if (playbackState == ExoPlayer.STATE_ENDED) {
                            imaVastVideoAd.imaVideoAd.stopWait(true)
                            exoplayer?.removeListener(this)
                        }
                    }
                })
                exoplayer?.playWhenReady = true
                Timber.d("loadVastIma() start playing ad")
            } catch (e: Exception) {
                e.printStackTrace()
                imaVastVideoAd.imaVideoAd.stopWait(false)
                // ignore
            }
        } else {
            imaVastVideoAd.imaVideoAd.stopWait(false)
        }
    }

    fun loadInfomercial(infomercialVideoAd: InfomercialVideoAd.InfomercialVideoAdLoader?) {
        Timber.d("AdsProxy: loadInfomercial()")
        if (isFocus && infomercialVideoAd != null && exoPlayerView != null) {
            try {
                val url = infomercialVideoAd.getUrl()
                val dataSourceFactory: DataSource.Factory =
                    DefaultDataSourceFactory(
                        getContext(),
                        Util.getUserAgent(getContext(), "Exo2")
                    )
                val mediaSource = ProgressiveMediaSource.Factory(
                    dataSourceFactory
                )
                    .createMediaSource(Uri.parse(url))
                currentMediaSource = mediaSource
                exoplayer?.addListener(infomercialVideoAd.infomercialVideoAd.exoplayerEventListener)
                exoplayer?.addListener(object :
                    Player.EventListener {
                    override fun onPlayerStateChanged(
                        playWhenReady: Boolean,
                        playbackState: Int
                    ) {
                        when (playbackState) {
                            Player.STATE_ENDED -> {
                                exoplayer?.removeListener(infomercialVideoAd.infomercialVideoAd.exoplayerEventListener)
                                exoplayer?.removeListener(this)
                            }
                        }
                    }
                })
                exoplayer?.prepare(mediaSource)
                exoplayer?.playWhenReady = !isPaused
            } catch (e: Exception) {
                e.printStackTrace()
                infomercialVideoAd.infomercialVideoAd.stopWait(false)
                // ignore
            }
        } else {
            infomercialVideoAd?.infomercialVideoAd?.stopWait(false)
        }
    }
}
