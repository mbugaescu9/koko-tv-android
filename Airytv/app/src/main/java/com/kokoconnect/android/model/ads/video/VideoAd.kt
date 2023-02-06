package com.kokoconnect.android.model.ads.video

import com.kokoconnect.android.model.player.VideoAdLoader
import com.kokoconnect.android.repo.AiryRepository

interface VideoAd {
    fun setExtra(extra: String)
    fun extra(): String
    fun priority(): Int
    fun name(): String
    fun url(): String
    fun type(): String
    fun prepare(videoAdsManager: VideoAdsManager,
                listener: VideoAdListener,
                onNeedShowAd: ((VideoAdLoader?) -> Unit)
    )
    fun isLoaded(): Boolean
    fun isSuccessful(): Boolean
    fun release()
    suspend fun load(airyRepo: AiryRepository): Boolean
    suspend fun startAndWait(): Boolean
    fun stopWait(successful: Boolean)
}

interface VideoAdListener {
    fun onEvent(ad: VideoAd, event: String) // for firebase logger
    fun onError(ad: VideoAd, description: String)
    fun onStarted(ad: VideoAd, description: String)
    fun onLoaded(ad: VideoAd, description: String)
    fun onRun(ad: VideoAd, description: String)
    fun onNeedLoadNextAd()
}

class VideoAdNumber(
    var currentAdNumber: Int,
    var adsCount: Int
) {

}