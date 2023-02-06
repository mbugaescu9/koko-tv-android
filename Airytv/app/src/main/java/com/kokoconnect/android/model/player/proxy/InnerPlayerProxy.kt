package com.kokoconnect.android.model.player.proxy

import android.view.View
import androidx.lifecycle.LifecycleObserver
import com.kokoconnect.android.model.player.Language
import com.kokoconnect.android.model.error.PlayerError
import com.kokoconnect.android.model.player.PlayerObject
import com.kokoconnect.android.model.player.PlayerType

interface PlayerProxy : LifecycleObserver

abstract class InnerPlayerProxy : PlayerProxy {
    abstract fun setup(params: Params?)
    abstract fun setPlayerView(playerView: View?)
    abstract fun setUiControllerEnabled(isEnabled: Boolean)
    abstract fun openVideo(video: PlayerObject, ignoreCheck: Boolean = false)
    abstract fun getCurrentPosition(): Long
    abstract fun getType(): PlayerType
    abstract fun isPlaying(): Boolean
    abstract fun play()
    abstract fun pause()
    abstract fun stop()
    abstract fun release()
    abstract fun isStopWithError(): Boolean

    abstract class Params {
        abstract var playerView: View?
        abstract var uiControllerEnabled: Boolean
    }
}

interface PlayerProxyListener {
    fun onBufferingProgressEnabled(playerProxy: InnerPlayerProxy, isEnabled: Boolean)
    fun onDetectedCueTones(playerProxy: InnerPlayerProxy, tag: String)
    fun onError(playerProxy: InnerPlayerProxy, error: PlayerError)
    fun onPositionUpdated(playerProxy: InnerPlayerProxy, position: Long)
    fun onAvailableLanguagesUpdated(playerProxy: InnerPlayerProxy, languages: List<Language>)
    fun onNeedReloadVideo(playerProxy: InnerPlayerProxy)
    fun onNeedRequestNextVideo(playerProxy: InnerPlayerProxy)
    fun onNeedSwitchFullscreen(playerProxy: InnerPlayerProxy)
}