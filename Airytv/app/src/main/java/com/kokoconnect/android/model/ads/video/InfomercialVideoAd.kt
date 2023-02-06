package com.kokoconnect.android.model.ads.video

import com.kokoconnect.android.model.ads.Infomercial
import com.kokoconnect.android.model.player.VideoAdLoader
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.util.AD_TYPE_INFOMERCIAL
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.lang.Exception
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class InfomercialVideoAd(val infomercial: Infomercial) : VideoAd {

    inner class InfomercialVideoAdLoader(
        val infomercialVideoAd: InfomercialVideoAd
    ) : VideoAdLoader() {
        override fun getUrl(): String = infomercialVideoAd.url()
        override fun setUrl(url: String?) {}
    }

    private var listener: VideoAdListener? = null
    private var onNeedShowAd: ((VideoAdLoader?) -> Unit)? = null
    private var continuation: Continuation<Boolean>? = null
    val exoplayerEventListener = ExoplayerInfomercialListener()

    inner class ExoplayerInfomercialListener(): Player.EventListener  {
        var isReady: Boolean = false

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    isReady = false
                    Timber.d("InfomercialVideoAd: exoplayerEventListener.onPlayerStateChanged() Player.STATE_ENDED continuation?.resume(true) ${continuation}")
                    stopWait(true)
                }
                Player.STATE_READY -> {
                    Timber.d("InfomercialVideoAd: exoplayerEventListener.onPlayerStateChanged() Player.STATE_READY")
                    if (!isReady) {
                        isReady = true
                        listener?.onLoaded(this@InfomercialVideoAd, getEventDescription())
                        listener?.onStarted(this@InfomercialVideoAd, getEventDescription())
                    }
                }
            }
        }

//            override fun onPlayerError(error: ExoPlaybackException?) {
//                super.onPlayerError(error)
//                listener?.onError(this@InfomercialVideoAd, getErrorDescription(error))
//            }
    }


    override fun priority(): Int = infomercial.priority

    override fun name(): String = infomercial.name.toString()

    override fun url(): String = infomercial.payload ?: ""

    override fun type(): String = AD_TYPE_INFOMERCIAL

    override fun setExtra(extra: String) {
        infomercial.extra = extra
    }

    override fun extra(): String = infomercial.extra ?: ""

    override fun prepare(
        videoAdsManager: VideoAdsManager,
        listener: VideoAdListener,
        onNeedShowAd: (VideoAdLoader?) -> Unit
    ) {
        this.listener = listener
        this.onNeedShowAd = onNeedShowAd
    }

    override fun isLoaded(): Boolean = true

    override fun isSuccessful(): Boolean = true

    override fun release() {
//        continuation?.resume(false)
    }

    override suspend
    fun load(airyRepo: AiryRepository): Boolean {
        // we load infomercial video at the time of playback in exoplayer
        return true
    }

    override suspend fun startAndWait(): Boolean = suspendCancellableCoroutine { continuation ->
        listener?.onRun(this, getEventDescription())
        exoplayerEventListener.isReady = false
        onNeedShowAd?.invoke(InfomercialVideoAdLoader(this))
        this.continuation = continuation
        continuation.invokeOnCancellation {
            Timber.d("InfomercialVideoAd: startAndWait() continuation.invokeOnCancellation")
            continuation.cancel()
        }
        // continuation?.resume(true) called in exoplayerEventListener.
    }

    override fun stopWait(successful: Boolean) {
        try {
            continuation?.resume(successful)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun getEventDescription(): String {
        return "INFOMERCIAL URL[${url()}]"
    }

    private fun getErrorDescription(error: ExoPlaybackException?): String {
        return "Error code: ${error?.type}," +
                "Error message: ${error?.message}," +
                "Error stack stace: ${error?.stackTrace}," +
                "INFOMERCIAL URL[${url()}]"
    }

}