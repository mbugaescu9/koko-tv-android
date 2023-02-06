package com.kokoconnect.android.model.ads.interstitial

import com.kokoconnect.android.util.FirebaseLogger
import com.kokoconnect.android.model.event.AdEventParams
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.util.AD_TYPE_INTERSTITIAL
import timber.log.Timber
import java.util.*

abstract class InitInterstitial(
    val adKey: String,
    val ams: AmsEventsFacade?,
    private val listener: InterstitialListener
) {
    private var startInitTime: Long = -1
    private var status: Status = Status.EMPTY
    private var lastTrigger: InterstitialTrigger? = null

    enum class Status {
        EMPTY, LOADING, LOADED, ERROR
    }

    /*
    Since cannot call abstract methods inside init - don't forget added in child class this code:
    init {
        init()
    }
    */

    // run called inside init { } and getStatus().
    // when override this method - be sure to call the super.init()
    open fun init() {
        lastTrigger = null
        startInitTime = Date().time
        status = Status.LOADING
        ams?.sendAdEventInit(
            AdEventParams(
                adKey = adKey,
                adClick = adClickSource(),
                adType = AD_TYPE_INTERSTITIAL
            )
        )
        Timber.d("init()")
    }

    // set value to false, if there is no method onAdOpened()/onAdDisplayed()
    // in the callback interstitial ad.
    abstract fun isExistOnStart(): Boolean

    // special for admob getMediationAdapterClassName()
    open fun getMediationName(): String? = null

    abstract fun getAdUnitId(): String?

    protected fun isLoaded(): Boolean = status == Status.LOADED

    // when override this method - be sure to call the super.show()
    open fun show(trigger: InterstitialTrigger?) {
        lastTrigger = trigger
        if (!isExistOnStart()) {
            onStarted()
        }
    }

    fun getStatus(): Status = status

    fun resetStatus() {
        status = Status.EMPTY
    }

    fun reInit() {
        ams?.sendAdEventReinit(
            AdEventParams(
                adKey = adKey,
                adClick = adClickSource(),
                adType = AD_TYPE_INTERSTITIAL
            )
        )
        init()
    }

    // other methods are called in a callback interstitial ad.
    fun onLoaded() {
        val time = Date().time - startInitTime
        val minutes = time / (60 * 1000)
        val seconds = time / 1000 % 60
        val str = String.format("%d:%02d", minutes, seconds)
        Timber.d("InitInterstitial: onLoaded() key == ${adKey} time == ${str}")
        status = Status.LOADED
        listener.onFinishInitInterstitial()
        ams?.sendAdEventLoaded(
            AdEventParams(
                adKey = adKey,
                adClick = adClickSource(),
                adType = AD_TYPE_INTERSTITIAL,
                adTrigger = lastTrigger?.reasonName,
                adDescription = getAdUnitId()
            )
        )
    }

    fun onClicked() {
        ams?.sendAdEventClicked(
            AdEventParams(
                adKey = adKey,
                adClick = adClickSource(),
                adType = AD_TYPE_INTERSTITIAL,
                adTrigger = lastTrigger?.reasonName,
                adDescription = getAdUnitId()
            )
        )
        FirebaseLogger.logAdClickEvent()
    }

    fun resumeTimer() {
        listener.onStartInterstitialTimer()
    }

    fun onError() {
        status = Status.ERROR
        ams?.sendAdEventLoadFail(
            AdEventParams(
                adKey = adKey,
                adClick = adClickSource(),
                adType = AD_TYPE_INTERSTITIAL,
                adTrigger = lastTrigger?.reasonName,
                adDescription = getAdUnitId()
            )
        )
    }

    fun onStarted() {
        val mediation = getMediationName()
        if (mediation != null) {
            ams?.sendAdEventShow(
                AdEventParams(
                    adKey = adKey,
                    adClick = mediation,
                    adType = AD_TYPE_INTERSTITIAL,
                    adTrigger = lastTrigger?.reasonName,
                    adDescription = getAdUnitId()
                )
            )
        } else {
            ams?.sendAdEventShow(
                AdEventParams(
                    adKey = adKey,
                    adClick = adClickSource(),
                    adType = AD_TYPE_INTERSTITIAL,
                    adTrigger = lastTrigger?.reasonName,
                    adDescription = getAdUnitId()
                )
            )
        }
        listener.onStopInterstitialTimer()
    }

    fun onFinish() {
        status = Status.EMPTY
        reInit()
        finishListener?.onFinish() // for giveaways logic
    }

    fun setFinishListener(finishListener: FinishListener) {
        this.finishListener = finishListener
    }

    interface FinishListener {
        fun onFinish()
        fun onNotLoadedAds()
    }

    private var finishListener: FinishListener? = null

    private fun adClickSource(): String? {
        return when (lastTrigger?.type) {
            InterstitialTrigger.Type.OnGetMoreTickets -> {
                "giveaway"
            }
            else -> {
                "main"
            }
        }
    }

}