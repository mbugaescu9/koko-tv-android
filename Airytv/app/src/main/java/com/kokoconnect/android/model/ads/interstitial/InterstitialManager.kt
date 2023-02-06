package com.kokoconnect.android.model.ads.interstitial

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.kokoconnect.android.model.ads.Ad
import com.kokoconnect.android.model.ads.AdPurpose
import com.kokoconnect.android.model.ads.AdsStatus
import com.kokoconnect.android.model.ads.interstitial.rewarded.InitRewardedTapjoy
import com.kokoconnect.android.model.ads.interstitial.rewarded.RewardedListener
import com.kokoconnect.android.model.ads.interstitial.timer.InterstitialTimer
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.ui.activity.BaseActivity
import com.kokoconnect.android.util.*
import com.ogury.consent.manager.ConsentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.coroutines.suspendCoroutine

class InterstitialManager(
    val context: Context,
    val ams: AmsEventsFacade,
    val adsStatus: LiveData<AdsStatus>,
    val onNeedShowInterstitial: MutableLiveData<InterstitialTrigger?>,
    val isInitTapjoySdk: MediatorLiveData<Boolean>,
    val isInitOgurySdk: MediatorLiveData<Boolean>
) : InterstitialListener, RewardedListener {
    class InitializationStatus() {

    }

    var interstitialTimer: InterstitialTimer = InterstitialTimer(adsStatus, onNeedShowInterstitial)
    private var thread: Thread? = null
    private var needShowAdOnStart = true
    private var channelChangesCount: Int = -1
    private var screenChangesCount: Int = 0
    private var interstitialAds: ArrayList<Pair<Ad, InitInterstitial>> = ArrayList()

    private fun getAdsStatus(): AdsStatus? = adsStatus.value

    private fun isAdsEnabled(): Boolean {
        return adsStatus.value?.enabled == true
    }

    override fun onStartInterstitialTimer() {
        interstitialTimer.start()
    }

    override fun onStopInterstitialTimer() {
        interstitialTimer.stop()
    }

    override fun onFinishInitInterstitial() {
        Timber.d("onFinishInitInterstitial()")
        if (checkOnStartAppTrigger()) {
            if (thread == null || thread?.isAlive == false) {
                interstitialTimer.stop()
                thread = Thread {
                    val delay = getAdsStatus()?.interstitialOnStartDelay ?: 0
                    Timber.d("onFinishInitInterstitial() sleep for ${delay} sec")
                    TimeUnit.SECONDS.sleep(delay)
                    needShowAdOnStart = false
                    Timber.d("onFinishInitInterstitial() show interstitial")
                    onNeedShowInterstitial.postValue(InterstitialTrigger.OnStartApp)
                }
                thread?.start()
            }
        }
    }

    override fun onRewarded() {

    }

    private fun getInitInterstitialObject(
        ad: Ad,
        activity: Activity?
    ): InitInterstitial? {
        return when (ad.name) {
            INTERSTITIAL_OGURY -> {
                if (ConsentManager.gdprApplies()) {
                    InitInterstitialOgury(ad, isInitOgurySdk, activity, ams, this)
                } else {
                    null
                }
            }
            INTERSTITIAL_ADMOB -> InitInterstitialAdmob(ad, activity, ams, this)
            INTERSTITIAL_GAM_PUBDESK -> InitInterstitialGAM(ad, activity, ams, this)
            INTERSTITIAL_GAM -> InitInterstitialGAM(ad, activity, ams, this)
            INTERSTITIAL_BFIO -> InitInterstitialBFIO(ad, activity, ams, this)
            INTERSTITIAL_PLACEHOLDER -> InitInterstitialPlaceholder(ad, activity, ams, this)
            REWARDED_TAPJOY -> InitRewardedTapjoy(
                ad,
                isInitTapjoySdk,
                activity as FragmentActivity,
                ams,
                this,
                this
            )
            else -> null
        }
    }

    // initialize interstitial and rewarded ads.
    private fun initAds(
        activity: Activity?,
        sortedAds: List<Ad>,
        addPlaceholder: Boolean = false,
        minSize: Int = 0
    ) {
        if (activity == null /* || interstitialTimer.executorIsNotNull()*/) return
        interstitialAds.clear()
        val ads = mutableListOf<Ad>()
        ads.addAll(sortedAds)
        if (addPlaceholder) {
            val hasPlaceholder = sortedAds.find {
                it.name == INTERSTITIAL_PLACEHOLDER
            } != null
            if (!hasPlaceholder) {
                ads.add(Ad(1, null).apply { this.name = INTERSTITIAL_PLACEHOLDER })
            }
        }
        for (ad in ads) {
            val interstitial = getInitInterstitialObject(ad, activity)
            if (interstitial != null) {
                interstitialAds.add(ad to interstitial)
            }
        }
        var adKeysString = interstitialAds.map { it.second.adKey }.joinToString(" ")
        Timber.d("initAds() end $adKeysString")
        val amounts = ArrayList<Int>().apply {
            add(getAdsStatus()?.interstitialOnStartNumberAds ?: 0)
            add(getAdsStatus()?.interstitialOnChannelChangeNumberAds ?: 0)
            add(getAdsStatus()?.interstitialOnTimerNumberAds ?: 0)
            add(minSize)
        }
        val amount = amounts.maxOrNull() ?: 0
        if (interstitialAds.isNotEmpty()
            && interstitialAds.size == 1
            && amount > 1
            && interstitialAds.first().second.adKey != REWARDED_TAPJOY
        ) {
            if (interstitialAds.size < amount) {
                for (index in 0 until amount - interstitialAds.size) {
                    val interstitial = interstitialAds.get(index)
                    val adKey = interstitial.second.adKey
                    val adPurposes = interstitial.first.adPurposes
                    val ad = ads.find { it.name == adKey && it.adPurposes.containsAll(adPurposes) }
                        ?: break
                    val adInterstitialObject = getInitInterstitialObject(ad, activity) ?: break
                    interstitialAds.add(ad to adInterstitialObject)
                }
            }
        }
        adKeysString = interstitialAds.map { it.second.adKey }.joinToString(" ")
        Timber.d("initAds() end $adKeysString")
    }

    private fun checkOnStartAppTrigger(): Boolean {
        val isNeedShow = (getAdsStatus()?.enabled ?: false)
                && (getAdsStatus()?.interstitialOnStartEnabled ?: false)
                && needShowAdOnStart
        Timber.d("isNeedShowInterstitialOnStart() isNeedShow ${isNeedShow}")
        return isNeedShow
    }

    private fun checkOnChannelChangeTrigger(): Boolean {
        var isNeedShow = false
        if (getAdsStatus()?.enabled != true) {
            isNeedShow = false
            return isNeedShow
        }
        if (getAdsStatus()?.interstitialOnChannelChangeEnabled == true) {
            val channelsNumber = getAdsStatus()?.interstitialOnChannelChangeNumberChannels ?: 0
            if (channelChangesCount >= channelsNumber && channelsNumber != 0) {
                channelChangesCount = -1
                isNeedShow = true
            }
        }
        return isNeedShow
    }

    private fun checkOnScreenChangeTrigger(): Boolean {
        var isNeedShow = false
        if (getAdsStatus()?.enabled != true) {
            isNeedShow = false
            return isNeedShow
        }
        if (getAdsStatus()?.interstitialOnScreenChangeEnabled == true) {
            val screensNumber = getAdsStatus()?.interstitialOnScreenChangeNumberScreens ?: 0
            if (screenChangesCount >= screensNumber && screensNumber != 0) {
                screenChangesCount = 0
                isNeedShow = true
            }
        }
        return isNeedShow
    }

    fun showInterstitialIfReady(
        activity: Activity?,
        trigger: InterstitialTrigger?,
        purposes: List<AdPurpose> = listOf(AdPurpose.MAIN, AdPurpose.GIVEAWAYS),
        finishListener: InitInterstitial.FinishListener? = null,
        rewardedListener: RewardedListener? = null
    ): Boolean {
        activity ?: return false
        Timber.d("showAdIfReady() purposes ${purposes.joinToString()}")
        val filteredInterstitials =
            interstitialAds.filter { it.first.adPurposes.containsAny(purposes) }
        for (adObject in filteredInterstitials) {
            val ad = adObject.first
            val interstitialObject = adObject.second
            when (interstitialObject.getStatus()) {
                InitInterstitial.Status.LOADED -> {
                    if (finishListener != null) {
                        interstitialObject.setFinishListener(finishListener)
                    }
                    interstitialObject.show(trigger)
                    Timber.d("showAdIfReady: true ${interstitialObject.adKey}")
                    channelChangesCount = 0
                    return true
                }
                InitInterstitial.Status.ERROR, InitInterstitial.Status.EMPTY -> {
                    Timber.d("showAdIfReady: false ${interstitialObject.adKey}")
                    if (!AppParams.appType.isServerRelease) {
                        activity.runOnUiThread {
                            activity.toast("Ad not available: ${interstitialObject.adKey}, run reinit.")
                        }
                    }
                    interstitialObject.init()
                }
                else -> {
                    // ignore
                }
            }
        }
        // if not one of the ads returned true -> restart the timer manually
        onStartInterstitialTimer()
        finishListener?.onNotLoadedAds()
        channelChangesCount = 0
        screenChangesCount = 0
        return false
    }

    fun showInterstitials(
        activity: BaseActivity?,
        coroutineScope: CoroutineScope,
        trigger: InterstitialTrigger?,
        channelId: Int? = null,
        onCompleted: (() -> Unit)? = null
    ) {
        Timber.d("showInterstitials() trigger ${trigger} channelId ${channelId}")

        val amount = when (trigger?.type) {
            InterstitialTrigger.Type.OnStartApp -> getAdsStatus()?.interstitialOnStartNumberAds
            InterstitialTrigger.Type.OnTimer -> getAdsStatus()?.interstitialOnTimerNumberAds
            InterstitialTrigger.Type.OnChannelChange -> getAdsStatus()?.interstitialOnChannelChangeNumberAds
            InterstitialTrigger.Type.OnScreenChange -> getAdsStatus()?.interstitialOnScreenChangeNumberAds
            InterstitialTrigger.Type.OnVideoAdError -> 1
            else -> null
        }

        val purposes = listOf(AdPurpose.MAIN)
        val interstitialsDisabled =
            channelId != null && getAdsStatus()?.isInterstitialsEnabled(channelId) == false
        var uiLocked = false

        if (activity == null
            || interstitialAds.isEmpty()
            || amount == null
            || amount == 0
            || interstitialsDisabled
        ) {
            Timber.d("showInterstitials(): no need run intertitial! interstitials count ${interstitialAds.size} run length ${amount} disabled ${interstitialsDisabled} ")
            onStartInterstitialTimer()
            onCompleted?.invoke()
            return
        }

        Timber.d("showInterstitials(): start $amount ads in sequence")
        val job = coroutineScope.launch(Dispatchers.Main) {
            val interstitialObjects = interstitialAds.filter {
                it.first.adPurposes.containsAny(purposes)
            }.map { it.second }
            val sequence: Queue<InitInterstitial> = LinkedList<InitInterstitial>()
            sequence.addAll(interstitialObjects)
            Timber.d("showInterstitials(): sequence ${sequence.size}")
            for (i in 0 until amount) {
                Timber.d("showInterstitials(): runNext")
                if (activity == null) break
                val ad = getLoadedInterstitial(sequence, i == amount - 1)
                if (ad != null) {
                    if (!uiLocked) {
                        // lock UI only when any loaded interstitial exists
                        activity.lockUi()
                        uiLocked = true
                    }
                    showInterstitial(activity, ad, trigger)
                } else {
                    Timber.d("showInterstitials(): no more loaded interstitals")
                }
            }
            onStartInterstitialTimer()
            channelChangesCount = 0
            activity.unlockUi()
            onCompleted?.invoke()
        }
        job.start()
    }

    private suspend fun showInterstitial(
        activity: Activity,
        ad: InitInterstitial?,
        trigger: InterstitialTrigger?
    ): Boolean = suspendCoroutine { continuation ->
        Timber.d("showInterstitial(): interstitial ${ad?.adKey}")
        when (ad?.getStatus()) {
            InitInterstitial.Status.LOADED -> {
                ad.setFinishListener(object : InitInterstitial.FinishListener {
                    override fun onFinish() {
                        continuation.resumeWithSafe(Result.success(true))
                    }

                    override fun onNotLoadedAds() {
                        continuation.resumeWithSafe(Result.success(false))
                    }
                })
                ad.show(trigger)
                Timber.d("showInterstitial() show ${ad.adKey}")
            }
            InitInterstitial.Status.ERROR, InitInterstitial.Status.EMPTY -> {
                Timber.d("showInterstitial() error ${ad.adKey}")
                if (!AppParams.appType.isServerRelease) {
                    activity.runOnUiThread {
                        activity.toast("Ad not available: ${ad.adKey}, run reinit.")
                    }
                }
                ad.reInit()
            }
            else -> {
                // ignore
            }
        }
        continuation.resumeWithSafe(Result.success(true))
    }

    private fun getLoadedInterstitial(
        sequence: Queue<InitInterstitial>,
        isLastNeedAd: Boolean
    ): InitInterstitial? {
        for (ad in sequence) {
            when (ad.getStatus()) {
                InitInterstitial.Status.LOADED -> {
                    Timber.d("getLoadedInterstitial(): ${ad.adKey} InitInterstitial.Status.LOADED")
                    sequence.remove(ad)
                    if (!isLastNeedAd) {
                        sequence.add(ad)
                    }
                    return ad
                }
                InitInterstitial.Status.ERROR, InitInterstitial.Status.EMPTY -> {
                    Timber.d("getLoadedInterstitial(): ${ad.adKey} InitInterstitial.Status.ERROR, InitInterstitial.Status.EMPTY")
                    ad.reInit()
                }
                else -> {
                    // ignore
                }
            }
        }
        return null
    }


}