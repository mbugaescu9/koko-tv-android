package com.kokoconnect.android.model.ads.interstitial

import android.app.Activity
import android.content.ActivityNotFoundException
import com.kokoconnect.android.model.ads.Ad
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.util.INTERSTITIAL_ADMOB
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import timber.log.Timber
import com.kokoconnect.android.R

class InitInterstitialAdmob(
    val ad: Ad?,
    val activity: Activity?,
    ams: AmsEventsFacade?,
    listener: InterstitialListener
) :
    InitInterstitial(INTERSTITIAL_ADMOB, ams, listener) {
    private var admobInterstitial: InterstitialAd? = null

    init {
        init()
    }

    override fun init() {
        super.init()
        if (activity == null) {
            onError()
            return
        }
        val adUnitId = getAdUnitId() ?: activity.getString(R.string.admob_unitid_interstitial)
        Timber.d("InitInterstitialAdmob: init() adUnitId ${adUnitId}")
        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object: InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    Timber.d("InitInterstitialAdmob: onAdFailedToLoad() on ad errors $error")
                    onError()
                }

                override fun onAdLoaded(interstitial: InterstitialAd) {
                    super.onAdLoaded(interstitial)
                    Timber.d("InitInterstitialAdmob: onAdLoaded()")
                    admobInterstitial = interstitial
                    onLoaded()
                }
            })
    }

    override fun isExistOnStart(): Boolean = true

    override fun getAdUnitId(): String? {
        return ad?.payload ?: activity?.getString(R.string.admob_unitid_interstitial)
    }

    override fun show(trigger: InterstitialTrigger?) {
        super.show(trigger)
        if (activity == null) {
            onError()
            return
        }
        Timber.d("InitInterstitialAdmob: show() isLoaded == ${isLoaded()}")
        if (isLoaded()) {
            admobInterstitial?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    Timber.d("InitInterstitialAdmob: onAdDismissedFullScreenContent()")
                    admobInterstitial = null
                    resumeTimer()
                    onFinish()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    super.onAdFailedToShowFullScreenContent(error)
                    Timber.d("InitInterstitialAdmob: onAdFailedToShowFullScreenContent() on ad errors $error")
                    admobInterstitial = null
                    onError()
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    Timber.d("InitInterstitialAdmob: onAdShowedFullScreenContent()")
                    onStarted()
                }
            }
            try {
                admobInterstitial?.show(activity)
            } catch(ex: ActivityNotFoundException) {
                ex.printStackTrace()
            }
        }
    }

    override fun getMediationName(): String? = admobInterstitial?.responseInfo?.mediationAdapterClassName
}