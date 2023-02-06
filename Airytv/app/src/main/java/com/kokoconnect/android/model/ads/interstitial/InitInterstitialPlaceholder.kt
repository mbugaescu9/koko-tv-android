package com.kokoconnect.android.model.ads.interstitial

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import com.kokoconnect.android.model.ads.Ad
import com.kokoconnect.android.util.INTERSTITIAL_PLACEHOLDER
import com.kokoconnect.android.model.ads.interstitial.placeholder.InterstitialAd
import com.kokoconnect.android.model.ads.interstitial.placeholder.InterstitialListener
import com.kokoconnect.android.model.event.AmsEventsFacade
import timber.log.Timber

class InitInterstitialPlaceholder(
    val ad: Ad?,
    val activity: Activity?,
    ams: AmsEventsFacade?,
    listener: com.kokoconnect.android.model.ads.interstitial.InterstitialListener
) : InitInterstitial(INTERSTITIAL_PLACEHOLDER, ams, listener) {

    companion object {
        // this field is needed to interact with InterstitialPlaceholderActivity.
        // keep an instance in it only when you call InterstitialPlaceholderActivity.
        // don't forget clear this field when close InterstitialPlaceholderActivity.
        @SuppressLint("StaticFieldLeak")
        var instanceInterstitialAd: InterstitialAd? = null
    }

    private var interstitialAd: InterstitialAd? = null

    init {
        init()
    }

    override fun init() {
        super.init()
        Timber.d("InitInterstitialPlaceholder: init()")
        interstitialAd = InterstitialAd()
        interstitialAd?.adListener = object : InterstitialListener {
            override fun onAdLoaded() {
                onLoaded()
            }

            override fun onAdClosed() {
                resumeTimer()
                instanceInterstitialAd = null
                onFinish()
            }

            override fun onAdClicked() {
                onClicked()
            }

            override fun onAdOpened() {
                onStarted()
            }
        }
        interstitialAd?.loadAd(activity ?: return)
    }

    override fun isExistOnStart(): Boolean = true

    override fun getAdUnitId(): String? {
        return ad?.payload
    }

    override fun show(trigger: InterstitialTrigger?) {
        super.show(trigger)
        Timber.d("InitInterstitialPlaceholder: show() isLoaded == ${isLoaded()}")
        if (isLoaded()) {
            try {
                instanceInterstitialAd = interstitialAd
                interstitialAd?.show(activity)
            } catch (ex: ActivityNotFoundException) {
                ex.printStackTrace()
            }
        }
    }

}