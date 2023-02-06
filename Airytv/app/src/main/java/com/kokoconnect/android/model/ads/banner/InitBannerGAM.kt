package com.kokoconnect.android.model.ads.banner

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.kokoconnect.android.vm.AmsEventsViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import timber.log.Timber

class InitBannerGAM(
    bannerData: Banner,
    amsModel: AmsEventsViewModel?
) : InitBanner(
    bannerData = bannerData,
    amsModel = amsModel
) {
    companion object {
        private val pubdeskCharIterator = PubdeskCharIterator()
    }

    private var lastAdUnitId: String? = null

    override fun getAdUnitId(): String? {
        return lastAdUnitId
    }

    override fun load(activity: Activity, targetView: ViewGroup) {
        super.load(activity, targetView)

        val mAdView = AdManagerAdView(activity)
        mAdView.setAdSizes(AdSize.BANNER)
        mAdView.setAdSize(when (bannerData.tag) {
//            BANNER_300x250_GAM -> { // todo need fix after merge
//                AdSize.MEDIUM_RECTANGLE
//            }
            else -> { //BANNER_GAM, BANNER_GAM_PUBDESK
                AdSize.BANNER
            }
        })

        mAdView.visibility = View.VISIBLE
        mAdView.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        val adUnitId = bannerData.payload?.replace(
            "%PUBDESK_CHAR%",
            pubdeskCharIterator.getChar().toString()
        ) ?: ""
        lastAdUnitId = adUnitId
        Timber.d("InitBannerGAM: load() ${adUnitId}")
        mAdView.adUnitId = adUnitId
        targetView.addView(mAdView)

        mAdView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                super.onAdFailedToLoad(adError)
                val code = adError.code
                val message = adError.message
                Timber.d("InitBannerGAM: AdListener() onAdFailedToLoad() error: $code message: $message payload: $adUnitId")
                this@InitBannerGAM.onError("Error code: $code, Error message: $message")
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                Timber.d("InitBannerGAM: AdListener() onAdLoaded() payload: $adUnitId")
                this@InitBannerGAM.onLoaded()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Timber.d("InitBannerGAM: AdListener() onAdClicked() payload: $adUnitId")
                this@InitBannerGAM.onClicked()
            }
        }
        val adRequest = AdManagerAdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

}