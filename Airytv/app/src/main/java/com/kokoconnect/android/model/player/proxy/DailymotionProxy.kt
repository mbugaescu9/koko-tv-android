package com.kokoconnect.android.model.player.proxy

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.kokoconnect.android.model.error.PlayerError
import com.kokoconnect.android.model.error.PlayerErrorType
import com.kokoconnect.android.model.player.*
import com.kokoconnect.android.ui.view.DailymotionPlayerWebView
import com.dailymotion.android.player.sdk.events.PlayerEvent
import com.dailymotion.android.player.sdk.events.TimeUpdateEvent
import com.dailymotion.android.player.sdk.events.VideoEndEvent
import timber.log.Timber
import kotlin.math.roundToLong

class DailymotionProxy(val listener: PlayerProxyListener) : InnerPlayerProxy() {
    class Params(
        override var playerView: View?,
        override var uiControllerEnabled: Boolean
        ) : InnerPlayerProxy.Params()

    private var dailymotionPlayerView: DailymotionPlayerWebView? = null
    private var uiControllerEnabled: Boolean = false
    private var currentStream: PlayerObject? = null
    private var isPaused: Boolean = false
    private var isFocus: Boolean = false

    private var eventListener = object: DailymotionPlayerWebView.EventListener{
        override fun onEventReceived(event: PlayerEvent) {
            if (event is VideoEndEvent) {
                Timber.d("DailymotionProxy: EventListener VideoEndEvent")
                listener.onNeedRequestNextVideo(this@DailymotionProxy)
            } else if (event is TimeUpdateEvent) {
                currentPositionMs = convertPosition(event.time)
            }
        }
    }
    private var currentPositionMs: Long = -1

    private fun convertPosition(positionString: String?): Long {
        val newPositionMs = ((positionString?.toDoubleOrNull() ?: -1.0) * 1000.0).roundToLong()
        return if (newPositionMs >= 0) {
            newPositionMs
        } else {
            -1
        }
    }

    override fun isStopWithError(): Boolean = false

    override fun setup(params: InnerPlayerProxy.Params?) {
        val dailymotionParams = params as? Params ?: return
        setPlayerView(dailymotionParams.playerView)
        setUiControllerEnabled(dailymotionParams.uiControllerEnabled)
    }

    override fun setPlayerView(playerView: View?) {
        dailymotionPlayerView = playerView as? DailymotionPlayerWebView
        dailymotionPlayerView?.setEventListener(eventListener)
        dailymotionPlayerView?.showControls(uiControllerEnabled)
        Timber.d("DailymotionProxy: setPlayerView() $dailymotionPlayerView")
    }

    override fun setUiControllerEnabled(isEnabled: Boolean) {
        uiControllerEnabled = isEnabled
        dailymotionPlayerView?.showControls(uiControllerEnabled)
    }

    override fun getCurrentPosition(): Long {
        return currentPositionMs
    }

    override fun getType(): PlayerType = PlayerType.DAILYMOTION

    override fun openVideo(video: PlayerObject, ignoreCheck: Boolean) {
        Timber.d("DailymotionProxy: openVideo() ${video.getUrl()} ${video.getStartPosition()}")
        if (dailymotionPlayerView == null) {
            Timber.d("DailymotionProxy: openVideo() view is null")
            listener.onError(
                this@DailymotionProxy,
                PlayerError(PlayerErrorType.DAILYMOTION_PLAYER_UNAVAILABLE)
            )
            return
        }
        isPaused = false
        dailymotionPlayerView?.let { player ->
            if (video is DailymotionVideo) {
                val params = mapOf(
                    "video" to video.getCue(),
                    "start" to video.getStartPosition() * 0.001,
                    "controls" to "false"
                )
                player.load(params)
                currentPositionMs = video.getStartPosition()
            }
        }
        currentStream = video
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        isFocus = false
        dailymotionPlayerView?.onPause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        isFocus = true
        dailymotionPlayerView?.onResume()
    }

    override fun isPlaying(): Boolean {
        return !isPaused && isFocus
    }

    override fun play() {
        isPaused = false
        dailymotionPlayerView?.play()
    }

    override fun pause() {
        isPaused = true
        dailymotionPlayerView?.pause()
    }

    override fun stop() {
        dailymotionPlayerView?.pause()
    }

    override fun release() {
        Timber.d("DailymotionProxy: release()")
        stop()
        currentStream = null
    }

}
