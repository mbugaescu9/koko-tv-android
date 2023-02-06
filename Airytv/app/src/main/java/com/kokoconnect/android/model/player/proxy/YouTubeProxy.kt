package com.kokoconnect.android.model.player.proxy

import android.view.View
import androidx.lifecycle.*
import com.kokoconnect.android.model.error.PlayerError
import com.kokoconnect.android.model.error.PlayerErrorType
import com.kokoconnect.android.model.error.YouTubePlayerError
import com.kokoconnect.android.model.player.*
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import timber.log.Timber
import kotlin.math.absoluteValue

class YouTubeProxy(val listener: PlayerProxyListener) : InnerPlayerProxy() {
    class Params(
        override var playerView: View?,
        override var uiControllerEnabled: Boolean
    ) : InnerPlayerProxy.Params()

    var whoLastLoadVideo: String? = "nobody"
    private var youtubePlayerView: YouTubePlayerView? = null
    private var uiControllerEnabled: Boolean = true
    var youtubePlayer: YouTubePlayer? = null
    var currentStream: PlayerObject? = null
    private var currentYoutubePosition = -1L
    var youtubePlayerTracker = YouTubePlayerTracker()
    var isPaused = false
    private var isFocus = false
    private var isReady = false
    private val callbacks = object :
        YouTubePlayerListener {
        var currentVideoId = ""
        override fun onApiChange(youTubePlayer: YouTubePlayer) {
            // ignore
        }

        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
            currentYoutubePosition = (second.toLong() * 1000L)
        }

        override fun onError(
            youTubePlayer: YouTubePlayer,
            reason: PlayerConstants.PlayerError
        ) {
            when (reason) {
                PlayerConstants.PlayerError.UNKNOWN,
                PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST,
                PlayerConstants.PlayerError.HTML_5_PLAYER,
                PlayerConstants.PlayerError.VIDEO_NOT_FOUND,
                PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> {
                    listener.onError(this@YouTubeProxy, YouTubePlayerError(reason.toString(),
                        whoLastLoadVideo.toString(),
                        currentStream?.getUrl().toString(),
                        currentYoutubePosition / 1000)
                    )
                }
            }
        }

        override fun onPlaybackQualityChange(
            youTubePlayer: YouTubePlayer,
            playbackQuality: PlayerConstants.PlaybackQuality
        ) {
            // ignore
        }

        override fun onPlaybackRateChange(
            youTubePlayer: YouTubePlayer,
            playbackRate: PlayerConstants.PlaybackRate
        ) {
            // ignore
        }

        override fun onReady(youTubePlayer: YouTubePlayer) {
            Timber.d("YouTubeProxy: onReady() currentStream == $currentStream youtubePlayer = ${youtubePlayer} view = ${youtubePlayerView}")
            isReady = true
            youtubePlayer = youTubePlayer
            currentStream?.let { playerObject ->
                whoLastLoadVideo = "onInitializationSuccess()"
                youtubePlayer?.addListener(youtubePlayerTracker)
                //youtubePlayerView?.postDelayed({
                    openVideo(playerObject as YouTube, true)
                //}, 50)
            }
        }

        override fun onStateChange(
            youTubePlayer: YouTubePlayer,
            state: PlayerConstants.PlayerState
        ) {
            if (state == PlayerConstants.PlayerState.ENDED) {
                listener.onNeedRequestNextVideo(this@YouTubeProxy)
            }
            when(state){
                PlayerConstants.PlayerState.PLAYING, PlayerConstants.PlayerState.UNSTARTED -> {
                    listener.onBufferingProgressEnabled(this@YouTubeProxy, false)
                    if (!isFocus) {
                        youtubePlayer?.pause()
                    }
                }
                else -> {
                    listener.onBufferingProgressEnabled(this@YouTubeProxy, true)
                }
            }
        }

        override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
            // ignore
        }

        override fun onVideoId(youTubePlayer: YouTubePlayer, videoId: String) {
            currentVideoId = videoId
        }

        override fun onVideoLoadedFraction(
            youTubePlayer: YouTubePlayer,
            loadedFraction: Float
        ) {
            // ignore
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        Timber.d("YouTubeProxy: onResume()")
        isFocus = true
        if (currentStream != null && !isPaused) {
            youtubePlayer?.play()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Timber.d("YouTubeProxy: onPause()")
        isFocus = false
        if (currentYoutubePosition != -1L) {
            listener.onPositionUpdated(this, currentYoutubePosition)
        }
        youtubePlayer?.pause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Timber.d("YouTubeProxy: release()")
        youtubePlayerView?.removeYouTubePlayerListener(callbacks)
        youtubePlayerView = null
        youtubePlayer?.pause()
        youtubePlayer = null
    }

    override fun setup(params: InnerPlayerProxy.Params?) {
        val youtubeParams = params as? Params ?: return
        setPlayerView(youtubeParams.playerView)
        setUiControllerEnabled(youtubeParams.uiControllerEnabled)
    }

    override fun setPlayerView(playerView: View?) {
//        youtubePlayerView = playerView as? YouTubePlayerView
        youtubePlayerView = playerView as? YouTubePlayerView
        Timber.d("YouTubeProxy: setPlayerView() $youtubePlayerView")
        youtubePlayerView?.addYouTubePlayerListener(callbacks)

        youtubePlayerView?.getPlayerUiController()?.apply {
            showSeekBar(uiControllerEnabled)
            showUi(uiControllerEnabled)
            setFullScreenButtonClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    listener.onNeedSwitchFullscreen(this@YouTubeProxy)
                }
            })
        }
    }

    override fun setUiControllerEnabled(isEnabled: Boolean) {
        uiControllerEnabled = isEnabled
        youtubePlayerView?.getPlayerUiController()?.showUi(uiControllerEnabled)
    }

    override fun getCurrentPosition(): Long {
        Timber.d("YouTubeProxy: getCurrentPosition() currentYoutubePosition == $currentYoutubePosition")
        return currentYoutubePosition
    }

    override fun openVideo(video: PlayerObject, ignoreCheck: Boolean) {
        Timber.d("YouTubeProxy: openVideo() url == ${video?.getUrl()} ignoreCheck == $ignoreCheck")
        Timber.d("YouTubeProxy: openVideo() newPosition = ${video.getStartPosition()} currentPosition = ${currentYoutubePosition}")

        if (video !is YouTube) return
        val videoCue = video.getCue()
        if (videoCue == null || videoCue.isEmpty()) return

        if (youtubePlayerView == null) {
            Timber.d("YouTubeProxy: openVideo() view is null")
            listener.onError(this@YouTubeProxy, PlayerError(PlayerErrorType.YOUTUBE_PLAYER_UNAVAILABLE))
        }
        val seekTo = (video as? YouTubeVideo)?.seekToTime?.absoluteValue ?: 0
        // if player not init - no matter need save videoCue, pick him up callback onReady(youTubePlayer: YouTubePlayer)
        currentStream = video
        Timber.d("YouTubeProxy: openVideo() run openCue seekTo == $seekTo")
        currentYoutubePosition = seekTo
        openCue(videoCue, seekTo)
    }

    // only for run from openVideoIfOnReady observe
    private fun openCue(videoCue: String?, seekTo: Long) {
        if (youtubePlayer != null) {
            Timber.d("YouTubeProxy: openCue($videoCue, $seekTo)")
            whoLastLoadVideo = "openCue($videoCue, $seekTo)"
            isPaused = false
            try {
                if (videoCue != null && videoCue.isNotEmpty()) {
                    youtubePlayer?.loadVideo(videoCue, seekTo / 1000f)
                } else {
                    Timber.e("onReady() cannot play empty cue")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Timber.d("YouTubeProxy: openCue() waiting work onReady() callback")
        }
    }

    override fun getType(): PlayerType = PlayerType.YOUTUBE

    override fun isPlaying(): Boolean {
        return !isPaused && isFocus
    }

    override fun pause() {
        Timber.d("YouTubeProxy: lifecycle pause()")
        isPaused = true
        youtubePlayer?.pause()
    }

    override fun play() {
        Timber.d("YouTubeProxy: lifecycle resume()")
        isPaused = false
        if (isFocus) {
            youtubePlayer?.play()
        }
    }

    override fun stop() {
        Timber.d("YouTubeProxy: lifecycle stop()")
        isPaused = true
        youtubePlayer?.pause()
    }

    override fun release() {
        Timber.d("YouTubeProxy: lifecycle release()")
        stop()
        currentStream = null
    }

    override fun isStopWithError(): Boolean = false

}