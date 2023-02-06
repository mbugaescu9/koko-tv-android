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
import com.google.gson.Gson
import timber.log.Timber
import org.prebid.mobile.*

class InitBannerTheadshop(
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
        super.load(activity, targetView)
        val mAdView = AdManagerAdView(activity)
        mAdView.visibility = View.VISIBLE
        mAdView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        val json: String? = bannerData.payload
        if (json == null) {
            Timber.d("InitBannerTheadshop: Not data for TheadshopAd")
            this@InitBannerTheadshop.onError("Not data for TheadshopAd")
            return
        }
        val theadshopAd = try {
            Gson().fromJson(json, TheadshopAd::class.java)
        } catch (e: Exception) {
            Timber.d("InitBannerTheadshop: Wrong data for TheadshopAd")
            this@InitBannerTheadshop.onError("Wrong data for TheadshopAd")
            return
        }
        theadshopAd.apply {
            lastAdUnitId = currentAdUnitId
            Timber.d("InitBannerTheadshop: load() ${currentAdUnitId} $configId")
            mAdView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    val code = adError.code
                    val message = adError.message
                    Timber.d("InitBannerTheadshop: AdListener() onAdFailedToLoad() error: $code message: $message id: $configId")
                    this@InitBannerTheadshop.onError("Error code: $code, Error message: $message")
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Timber.d("InitBannerTheadshop: AdListener() onAdLoaded() id: $configId")
                    this@InitBannerTheadshop.onLoaded()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Timber.d("InitBannerTheadshop: AdListener() onAdClicked() id: $configId")
                    this@InitBannerTheadshop.onClicked()
                }
            }
            val adRequest = AdManagerAdRequest.Builder().build()
            mAdView.setAdSizes(AdSize(width, height))
            mAdView.setAdUnitId(currentAdUnitId)
            targetView.addView(mAdView)

            val bannerAdUnit = BannerAdUnit(configId, width, height)
            bannerAdUnit.fetchDemand(adRequest, object : OnCompleteListener {
                override fun onComplete(resultCode: ResultCode?) {
                    Timber.d("InitBannerTheadshop: AdListener() onComplete() resultCode $resultCode")
                    mAdView.loadAd(adRequest)
                }
            })
        }
    }

    inner class TheadshopAd (
        val width: Int,
        val height: Int,
        val configId: String,
        val currentAdUnitId: String,
    )

}