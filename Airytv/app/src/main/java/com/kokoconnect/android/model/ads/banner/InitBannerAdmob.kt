package com.kokoconnect.android.model.ads.banner

import android.app.Activity
import android.view.ViewGroup
import com.kokoconnect.android.util.*
import com.kokoconnect.android.vm.AmsEventsViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.*
import timber.log.Timber
import kotlin.math.roundToInt
import com.kokoconnect.android.R

class InitBannerAdmob(
    bannerData: Banner,
    amsModel: AmsEventsViewModel?
) : InitBanner(
    bannerData = bannerData,
    amsModel = amsModel
) {
    private var lastAdUnitId: String? = null

    override fun getAdUnitId(): String? {
        return lastAdUnitId
    }

    override fun load(activity: Activity, targetView: ViewGroup) {
        val mAdView = AdView(activity)
        mAdView.setAdSize( if (activity.resources.configuration.isOrientationLandscape()) {
            AdSize.BANNER
        } else {
            when (bannerData.tag) {
                BANNER_ADAPTIVE_ADMOB -> {
                    val width = getDisplayWidth(activity)
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, width)
                }
                BANNER_320x50_ADMOB -> {
                    AdSize.BANNER
                }
                BANNER_320x100_ADMOB -> {
                    AdSize.LARGE_BANNER
                }
                else -> {
                    AdSize.BANNER
                }
            }

        }
        )

        val adUnitId = bannerData.payload ?: activity.getString(R.string.admob_unitid_banner)
        lastAdUnitId = adUnitId
        mAdView.adUnitId = adUnitId
        targetView.addView(mAdView)
        mAdView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                super.onAdFailedToLoad(adError)
                val code = adError.code
                val message = adError.message
                Timber.d("InitBannerAdmob: AdListener() onAdFailedToLoad() error: $code message: $message payload: $adUnitId")
                this@InitBannerAdmob.onError("Error code: $code, Error message: $message")
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                Timber.d("InitBannerAdmob: AdListener() onAdLoaded() payload: $adUnitId")
                this@InitBannerAdmob.onLoaded()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Timber.d("InitBannerAdmob: AdListener() onAdClicked() payload: $adUnitId")
                this@InitBannerAdmob.onClicked()
            }
        }
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    private fun getDisplayWidth(activity: Activity): Int {
        val metrics = DeviceUtils.getDisplayMetrics(activity)
        val widthPixels = metrics.widthPixels
        val density = metrics.density
        return (widthPixels / density).roundToInt()
    }
}