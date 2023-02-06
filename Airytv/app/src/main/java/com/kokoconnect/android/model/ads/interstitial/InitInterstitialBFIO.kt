package com.kokoconnect.android.model.ads.interstitial

import android.app.Activity
import com.kokoconnect.android.R
import com.kokoconnect.android.model.ads.Ad
import com.kokoconnect.android.util.INTERSTITIAL_BFIO
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.bfio.ad.BFIOErrorCode
import com.bfio.ad.BFIOInterstitial
import com.bfio.ad.model.BFIOInterstitalAd
import timber.log.Timber

class InitInterstitialBFIO(
    val ad: Ad?,
    val activity: Activity?,
    ams: AmsEventsFacade?,
    listener: InterstitialListener
) :
    InitInterstitial(INTERSTITIAL_BFIO, ams, listener) {
    private var bfioInterstitial: BFIOInterstitial? = null
    private var bfioInterstitialAd: BFIOInterstitalAd? = null


    init {
        init()
    }

    override fun init() {
        super.init()
        if (activity != null) {
            bfioInterstitial =
                BFIOInterstitial(
                    activity,
                    object : BFIOInterstitial.InterstitialListener {
                        override fun onInterstitialFailed(p0: BFIOErrorCode?) {
                            Timber.d("AdsViewModel: bfio onInterstitialFailed() $p0")
                            onError()
                        }

                        override fun onInterstitialStarted() {
                            onStarted()
                        }

                        override fun onInterstitialDismissed() {
                            bfioInterstitialAd = null
                            resumeTimer()
                        }

                        override fun onInterstitialClicked() {
                            onClicked()
                        }

                        override fun onInterstitialCompleted() {
                            bfioInterstitialAd = null
                            resumeTimer()
                            onFinish()
                        }

                        override fun onReceiveInterstitial(receivedAd: BFIOInterstitalAd?) {
                            onLoaded()
                            bfioInterstitialAd = receivedAd
                        }

                    })
            bfioInterstitialAd = null
            val adUnitId = getAdUnitId() ?: activity.getString(R.string.bfio_key)
            bfioInterstitial?.requestInterstitial(adUnitId)
        }
    }

    override fun isExistOnStart(): Boolean = true

    override fun getAdUnitId(): String? {
        return ad?.payload ?: activity?.getString(R.string.bfio_key)
    }

    override fun show(trigger: InterstitialTrigger?) {
        super.show(trigger)
        Timber.d("InitInterstitialDFIO: show() isLoaded == ${isLoaded()}")
        if (isLoaded()) {
            bfioInterstitial?.showInterstitial(bfioInterstitialAd)
        }
    }
}