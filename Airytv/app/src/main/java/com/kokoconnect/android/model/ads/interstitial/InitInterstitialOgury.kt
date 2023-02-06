package com.kokoconnect.android.model.ads.interstitial

import android.app.Activity
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.kokoconnect.android.model.ads.Ad
import com.kokoconnect.android.util.INTERSTITIAL_OGURY
import com.kokoconnect.android.model.event.AmsEventsFacade
import io.presage.common.AdConfig
import io.presage.interstitial.PresageInterstitial
import io.presage.interstitial.PresageInterstitialCallback
import timber.log.Timber


class InitInterstitialOgury(
    val ad: Ad?,
    val isInitOgurySdk: MediatorLiveData<Boolean>,
    val activity: Activity?,
    ams: AmsEventsFacade?,
    listener: InterstitialListener
) :
    InitInterstitial(INTERSTITIAL_OGURY, ams, listener) {

    init {
        init()
    }

    var interstitial: PresageInterstitial? = null

    override fun init() {
        super.init()
        val unitId = getAdUnitId()
        if (unitId != null) {
            val config = AdConfig(unitId)
            Timber.d("InitInterstitialOgury: current unit id $unitId")
            interstitial = PresageInterstitial(activity, config)
        } else {
            Timber.d("InitInterstitialOgury: without AdConfig")
            interstitial = PresageInterstitial(activity)
        }
        interstitial?.setInterstitialCallback(object :
            PresageInterstitialCallback {

            override fun onAdNotLoaded() {
                Timber.d("InitInterstitialOgury: onAdNotLoaded()")
            }

            override fun onAdLoaded() {
                Timber.d("InitInterstitialOgury: onAdLoaded()")
                onLoaded()
            }

            override fun onAdNotAvailable() {
                Timber.d("InitInterstitialOgury: onAdNotAvailable()")
                Log.i("Ogury", "on ad not available")
                onError()
            }

            override fun onAdAvailable() {
                Timber.d("InitInterstitialOgury: onAdAvailable()")
                Log.i("Ogury", "on ad available")
            }

            override fun onAdError(code: Int) {
                Timber.d("InitInterstitialOgury: onAdFailedToLoad() on ad errors $code")
                onError()
                /*
                 code 0: load failed
                 code 1: phone not connected to internet
                 code 2: ad disabled
                 code 3: various errors (configuration file not synced)
                 code 4: ad expires in 4 hours if it was not shown
                 code 5: start method not called
                 code 6: the sdk initialisation failed
                 code 7: app in background
                 code 8: another FloatingAd ad is already displayed
                */
            }

            override fun onAdClosed() {
                Timber.d("InitInterstitialOgury: onAdClosed()")
                resumeTimer()
                onFinish()
            }

            override fun onAdDisplayed() {
                Timber.d("InitInterstitialOgury: onAdDisplayed()")
                onStarted()
            }

        })

        if (activity is FragmentActivity) {
            isInitOgurySdk.observe(activity, Observer {
                if (it != null && it == true) {
                    Timber.d("InitInterstitialOgury: run load()")
                    interstitial?.load()
                } else {
                    Timber.d("InitInterstitialOgury: cancel run load() - not init sdk")
                }
            })
        }
    }

    override fun isExistOnStart(): Boolean = false

    override fun getAdUnitId(): String? {
        return ad?.payload
    }

    override fun show(trigger: InterstitialTrigger?) {
        super.show(trigger)
        Timber.d("InitInterstitialOgury: show() isLoaded == ${isLoaded()}")
        if (isLoaded()) {
            interstitial?.show()
        }
    }

}