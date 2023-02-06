package com.kokoconnect.android.model.ads.interstitial

import android.app.Activity
import android.content.ActivityNotFoundException
import com.kokoconnect.android.model.ads.Ad
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.util.INTERSTITIAL_GAM
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import timber.log.Timber

class InitInterstitialGAM(
    val ad: Ad?,
    val activity: Activity?,
    ams: AmsEventsFacade?,
    listener: InterstitialListener
) :
    InitInterstitial(INTERSTITIAL_GAM, ams, listener) {
    private var gamInterstitial: AdManagerInterstitialAd? = null

    init {
        init()
    }

    override fun init() {
        super.init()
        val adUnitId = getAdUnitId()
        if (adUnitId == null || activity == null) {
            onError()
            return
        }
        Timber.d("InitInterstitialGAM: init() adUnitId $adUnitId")
        AdManagerInterstitialAd.load(
            activity,
            adUnitId,
            AdManagerAdRequest.Builder().build(),
            object : AdManagerInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    Timber.d("InitInterstitialGAM: onAdFailedToLoad() on ad errors $error")
                    onError()
                }

                override fun onAdLoaded(interstitial: AdManagerInterstitialAd) {
                    super.onAdLoaded(interstitial)
                    gamInterstitial = interstitial
                    Timber.d("InitInterstitialGAM: onAdLoaded()")
                    onLoaded()
                }
            }
        )
    }

    override fun isExistOnStart(): Boolean = true

    override fun getAdUnitId(): String? {
        return ad?.payload
    }

    override fun show(trigger: InterstitialTrigger?) {
        super.show(trigger)
        if (activity == null) {
            onError()
            return
        }
        Timber.d("InitInterstitialGAM: show() isLoaded == ${isLoaded()}")
        if (isLoaded()) {
            try {
                gamInterstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()
                        Timber.d("InitInterstitialGAM onAdDismissedFullScreenContent()")
                        gamInterstitial = null
                        resumeTimer()
                        onFinish()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        super.onAdFailedToShowFullScreenContent(error)
                        Timber.d("InitInterstitialGAM onAdFailedToShowFullScreenContent() on ad errors $error")
                        gamInterstitial = null
                        onError()
                    }

                    override fun onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent()
                        Timber.d("InitInterstitialAdmob onAdShowedFullScreenContent()")
                        onStarted()
                    }
                }

                gamInterstitial?.show(activity)
            } catch (ex: ActivityNotFoundException) {
                ex.printStackTrace()
            }
        }
    }

    override fun getMediationName(): String? =
        gamInterstitial?.responseInfo?.mediationAdapterClassName
}