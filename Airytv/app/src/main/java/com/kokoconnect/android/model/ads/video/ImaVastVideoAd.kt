package com.kokoconnect.android.model.ads.video

import android.content.Context
import android.net.Uri
import com.kokoconnect.android.model.ads.Ima
import com.kokoconnect.android.model.player.VideoAdLoader
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.util.AD_TYPE_IMA
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.android.exoplayer2.ext.ima.AdEventListener
import com.google.android.exoplayer2.ext.ima.CustomFields
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ImaVastVideoAd(val ima: Ima, val context: Context) : VideoAd {

    inner class ImaVastVideoAdLoader(val imaVideoAd: ImaVastVideoAd) : VideoAdLoader() {
        override fun getUrl(): String = ""
        override fun setUrl(url: String?) { }
    }

    private var listener: VideoAdListener? = null
    private var onNeedShowAd: ((VideoAdLoader?) -> Unit)? = null
    var imaLoader: ImaAdsLoader? = null
        private set
    private var imaBridge: CustomFields.ImaBridge? = null
    private var imaLoaderBuilder: ImaAdsLoader.Builder? = null
    private var url: String = ""
    private var vastXml = ""
    private var continuation: Continuation<Boolean>? = null
    private var isSuccessful = true

    override fun priority(): Int = ima.priority

    override fun name(): String = ima.name.toString()

    override fun url(): String = url

    override fun type(): String = AD_TYPE_IMA

    override fun setExtra(extra: String) {
        ima.extra = extra
    }

    override fun extra(): String = ima.extra ?: ""

    override fun prepare(videoAdsManager: VideoAdsManager,
                         listener: VideoAdListener,
                         onNeedShowAd: ((VideoAdLoader?) -> Unit)
    ) {
        this.listener = listener
        this.onNeedShowAd = onNeedShowAd
        imaBridge = ima.getBridge()
        this.url = videoAdsManager.prepareVastUrl(ima.payload ?: "") ?: ""
        Timber.d("ImaVastVideoAd: prepare() url == ${this.url}")
        this.imaLoaderBuilder = ImaAdsLoader.Builder(context)
                .setMediaLoadTimeoutMs(TimeUnit.SECONDS.toMillis(15).toInt())
                .setCustomFields(CustomFields(ima.getBridge(), object : AdEventListener {
                    override fun onAdEvent(event: AdEvent?, adsLoader: ImaAdsLoader?) {
                        adEventParser(event, imaBridge)
                    }

                    override fun onAdError(event: AdErrorEvent?, adsLoader: ImaAdsLoader?) {
                        isSuccessful = false
                        adErrorParser(event, imaBridge)
                    }
                }))
    }

    override suspend fun load(airyRepo: AiryRepository): Boolean = suspendCoroutine { continuation ->
        try {
            imaLoader = imaLoaderBuilder?.buildForAdTag(Uri.parse(url()))
            Timber.d("ImaVastVideoAd: load() url == ${this.url}")
            airyRepo.downloadVastXml(
                    url = url(),
                    onSuccess = {
                        vastXml = it
                        ima.vast = vastXml
                        Timber.d("ImaVastVideoAd: downloadVast() downloaded = ${vastXml}")
                        continuation.resume(true)
                    },
                    onError = {
                        Timber.d("ImaVastVideoAd: downloadVast() downloadFailed ${it}")
                        continuation.resume(false)
                    }
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            continuation.resume(false)
        }
    }

    override fun isLoaded(): Boolean {
        return vastXml.isNotEmpty() && imaLoader != null
    }

    override fun isSuccessful(): Boolean = isSuccessful

    override fun release() {
        imaLoader?.release()
    }

    override suspend fun startAndWait(): Boolean = suspendCancellableCoroutine { continuation ->
        if (isLoaded()) {
            Timber.d("ImaVastVideoAd: startAndWait() ad loaded, imaLoader = ${imaLoader}")
            listener?.onRun(this, getEventDescription(imaBridge))
            if (imaLoader != null) {
                onNeedShowAd?.invoke(ImaVastVideoAdLoader(this))
            } else {
                onNeedShowAd?.invoke(null)
            }
            this.continuation = continuation
            // continuation?.resume(true) called in adEventParser()
        } else {
            Timber.d("ImaVastVideoAd: startAndWait() ad not loaded")
            stopWait(false)
        }
    }

    override fun stopWait(successful: Boolean) {
        try {
            Timber.d("ImaVastVideoAd: stopWait()")
            continuation?.resume(successful)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun adErrorParser(event: AdErrorEvent?, imaBridge: CustomFields.ImaBridge?) {
        Timber.d("ImaVastVideoAd: adErrorParser()" +
                " error = ${event?.error?.errorType?.name}" +
                " for ${imaBridge?.name}")
        stopWait(false)
        listener?.onError(this, getErrorDescription(event, imaBridge))
    }

    private fun adEventParser(event: AdEvent?,
                              imaBridge: CustomFields.ImaBridge?) {
        if (event?.type != AdEvent.AdEventType.AD_PROGRESS) {
            Timber.d("ImaVastVideoAd: loadLoader2() event = ${event?.type?.name}" +
                    " for ${imaBridge?.name}")
            listener?.onEvent(this, event?.type?.name.toString())
        }
        when (event?.type) {
            AdEvent.AdEventType.STARTED -> {
                listener?.onStarted(this, getEventDescription(imaBridge))
            }
            AdEvent.AdEventType.LOADED -> {
                listener?.onLoaded(this, getEventDescription(imaBridge))
                listener?.onNeedLoadNextAd()
            }
//            AdEvent.AdEventType.PAUSED -> {
//                listener?.onNeedLoadNextAd()
//                stopWait(true)
//            }
            AdEvent.AdEventType.LOG -> {
                event.adData?.let { data ->
                    if (data.isNotEmpty() && data.containsKey("errorCode")) {
                        val errorText = data.toString() + ", VAST XML[${imaBridge?.vastXml}]"
                        isSuccessful = false
                        listener?.onError(this, errorText)
                    }
                }
            }
            AdEvent.AdEventType.SKIPPED,
            AdEvent.AdEventType.COMPLETED,
            AdEvent.AdEventType.ALL_ADS_COMPLETED -> {
                listener?.onNeedLoadNextAd()
                stopWait(true)
            }
            else -> {
            }
        }
    }

    private fun getErrorDescription(
            event: AdErrorEvent?,
            imaBridge: CustomFields.ImaBridge?,
            xmlEnabled: Boolean = true
    ): String = event?.error?.let {
        "Error type: ${it.errorType?.name}, " +
                "Error code: ${it.errorCode?.errorNumber}," +
                "Error message: ${it.message}," +
                "Error code number: ${it.errorCodeNumber}" +
                if (xmlEnabled) ", VAST XML[${imaBridge?.vastXml}]" else ""
    }.toString()

    private fun getEventDescription(imaBridge: CustomFields.ImaBridge?): String {
        val url = url()
        val xml = imaBridge?.vastXml
        return "VAST URL[${url}], VAST XML[${xml}]"
    }

}