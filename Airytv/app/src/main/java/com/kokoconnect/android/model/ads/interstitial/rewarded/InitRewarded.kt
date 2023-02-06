package com.kokoconnect.android.model.ads.interstitial.rewarded

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.kokoconnect.android.model.ads.Ad
import com.kokoconnect.android.model.ads.interstitial.InitInterstitial
import com.kokoconnect.android.model.ads.interstitial.InterstitialTrigger
import com.kokoconnect.android.model.ads.interstitial.InterstitialListener
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.util.REWARDED_TAPJOY
import com.tapjoy.*
import timber.log.Timber

abstract class InitRewarded(
    adKey: String,
    ams: AmsEventsFacade?,
    listener: InterstitialListener,
    val rewardedListener: RewardedListener,
): InitInterstitial(adKey, ams, listener) {

    fun onRewarded() {
        rewardedListener.onRewarded()
    }

}

class InitRewardedTapjoy(
    val ad: Ad?,
    val isInitTapoySdk: MediatorLiveData<Boolean>,
    val activity: FragmentActivity?,
    ams: AmsEventsFacade?,
    listener: InterstitialListener,
    rewardedListener: RewardedListener
): InitRewarded(REWARDED_TAPJOY, ams, listener, rewardedListener), LifecycleObserver {

    init {
        init()
    }

    private var placement: TJPlacement? = null
    private var videoCompleted = false
    private var placementClosed = false

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onAdClosed() {
        if (!placementClosed) {
            placementClosed = true
            Timber.d("onTapjoyClosed, videoCompleted = $videoCompleted, alreadyClosed = $placementClosed")
            activity?.lifecycle?.removeObserver(this)
            onFinish()
        }
    }

    override fun init() {
        super.init()
        resetStatus()

        videoCompleted = false

        Timber.d("TAPJOY AD TEST: call init() ${this@InitRewardedTapjoy.toString()}")

        Tapjoy.setEarnedCurrencyListener(object : TJEarnedCurrencyListener {
            override fun onEarnedCurrency(currencyName: String?, amount: Int) {
                Timber.d("TAPJOY AD TEST: You've just earned " + amount + " " + currencyName)
                onRewarded()
            }
        })

        Tapjoy.setActivity(activity)

        val adUnitId = getAdUnitId()
        if (adUnitId == null) {
            onError()
            return
        }

        placement = Tapjoy.getPlacement(adUnitId, object :
            TJPlacementListener {
            override fun onClick(p0: TJPlacement?) {
                Timber.d("TAPJOY AD TEST: onClick() ${this@InitRewardedTapjoy.toString()}")
                onClicked()
            }

            override fun onContentShow(p0: TJPlacement?) {
                Timber.d("TAPJOY AD TEST: onContentShow() ${this@InitRewardedTapjoy.toString()}")
                activity?.lifecycle?.addObserver(this@InitRewardedTapjoy)
                onStarted()
            }

            override fun onRequestFailure(p0: TJPlacement?, p1: TJError?) {
                Timber.d("TAPJOY AD TEST: onRequestFailure() ${p1} ${this@InitRewardedTapjoy.toString()}")
                onError()
            }

            override fun onPurchaseRequest(
                p0: TJPlacement?,
                p1: TJActionRequest?,
                p2: String?
            ) {
                Timber.d("TAPJOY AD TEST: onPurchaseRequest() ${this@InitRewardedTapjoy.toString()}")
            }

            override fun onRequestSuccess(p0: TJPlacement?) {
                Timber.d("TAPJOY AD TEST: onRequestSuccess() ${this@InitRewardedTapjoy.toString()} readyToShow = ${p0?.isContentReady}")
            }

            override fun onRewardRequest(
                p0: TJPlacement?,
                p1: TJActionRequest?,
                p2: String?,
                p3: Int
            ) {
                Timber.d("TAPJOY AD TEST: onRewardRequest() p2==$p2 ${this@InitRewardedTapjoy.toString()}")
            }

            override fun onContentReady(p0: TJPlacement?) {
                Timber.d("TAPJOY AD TEST: onContentReady() ${this@InitRewardedTapjoy.toString()}")
                onLoaded()
            }

            override fun onContentDismiss(p0: TJPlacement?) {
                Timber.d("TAPJOY AD TEST: onContentDismiss() ${this.toString()} ${this@InitRewardedTapjoy.toString()}")
                if (!placementClosed) {
                    placementClosed = true
                    onFinish()
                }
                Tapjoy.getCurrencyBalance(object : TJGetCurrencyBalanceListener {
                    override fun onGetCurrencyBalanceResponseFailure(error: String?) {
                        Timber.d("TAPJOY AD TEST: getCurrencyBalance error: " + error);
                    }

                    override fun onGetCurrencyBalanceResponse(currencyName: String?, balance: Int) {
                        Timber.d("TAPJOY AD TEST: getCurrencyBalance returned " + currencyName + ":" + balance);
                    }
                })
            }
        })

        placement?.videoListener = object: TJPlacementVideoListener {
            override fun onVideoStart(p0: TJPlacement?) {
                videoCompleted = false
                Timber.d("TAPJOY AD TEST: onVideoStart()")
            }

            override fun onVideoComplete(p0: TJPlacement?) {
                videoCompleted = true
                Timber.d("TAPJOY AD TEST: onVideoComplete()")

            }

            override fun onVideoError(p0: TJPlacement?, p1: String?) {
                Timber.d("TAPJOY AD TEST: onVideoError()")
            }

        }


        activity?.let {
            isInitTapoySdk.observe(it, Observer {
                if (it != null && it == true) {
                    if (Tapjoy.isConnected()) {
                        Timber.d("TAPJOY AD TEST: requestContent()")
                        placement?.requestContent()
                    } else {
                        Timber.d("TAPJOY AD TEST: Tapjoy SDK must finish connecting before requesting content. ${this@InitRewardedTapjoy.toString()}")
                    }
                }
            })
        }
    }

    override fun isExistOnStart(): Boolean = true

    override fun getAdUnitId(): String? {
        return ad?.payload
    }

    override fun show(trigger: InterstitialTrigger?) {
        super.show(trigger)
        if(placement?.isContentReady == true) {
            placementClosed = false
            placement?.showContent()
            Timber.d("TAPJOY AD TEST: onRequestSuccess() isContentReady() == true ${this@InitRewardedTapjoy.toString()}")
        } else {
            Timber.d("TAPJOY AD TEST: onRequestSuccess() isContentReady() == false ${this@InitRewardedTapjoy.toString()}")
        }
    }

}