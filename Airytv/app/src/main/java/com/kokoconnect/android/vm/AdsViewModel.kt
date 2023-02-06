package com.kokoconnect.android.vm

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.model.tv.ProgramDescription
import com.kokoconnect.android.model.ads.Ad
import com.kokoconnect.android.model.ads.AdPurpose
import com.kokoconnect.android.model.ads.AdsStatus
import com.kokoconnect.android.model.ads.banner.BannerManager
import com.kokoconnect.android.model.ads.interstitial.*
import com.kokoconnect.android.model.ads.interstitial.rewarded.InitRewardedTapjoy
import com.kokoconnect.android.model.ads.interstitial.rewarded.RewardedListener
import com.kokoconnect.android.model.ads.interstitial.timer.InterstitialTimer
import com.kokoconnect.android.model.player.YouTube
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.ui.activity.BaseActivity
import com.kokoconnect.android.util.*
import com.google.android.gms.ads.MobileAds
import com.ironsource.mediationsdk.IronSource
import com.ogury.consent.manager.ConsentListener
import com.ogury.consent.manager.ConsentManager
import com.ogury.consent.manager.util.consent.ConsentException
import com.tapjoy.TJConnectListener
import com.tapjoy.Tapjoy
import com.tapjoy.TapjoyConnectFlag
import io.presage.Presage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.chartboost.sdk.Chartboost
import com.kokoconnect.android.R

class AdsViewModel @Inject constructor(
    private val airyRepo: AiryRepository,
    private val app: AiryTvApp
) : AndroidViewModel(app), InterstitialListener, RewardedListener {

    companion object {
        var needShowAdOnStart = true
        var screenChangesNumber: Int = 0
        private var intertitialAds: ArrayList<Pair<Ad, InitInterstitial>> = ArrayList()

        fun onScreenChange() {
            screenChangesNumber++
            Timber.d("AdsViewModel: onScreenChange() screenChangesNumber++ $screenChangesNumber")
        }
    }

    private var thread: Thread? = null
    private var countSwitches = -1

    val adsStatus: LiveData<AdsStatus> by lazy { airyRepo.getAdsStatus() }
    val needShowInterstitial = MediatorLiveData<InterstitialTrigger?>()
    val isInitAdmobSdk = MediatorLiveData<Boolean>().apply { postValue(false) }
    val isInitTapoySdk = MediatorLiveData<Boolean>().apply { postValue(false) }
    val isInitOgurySdk = MediatorLiveData<Boolean>().apply { postValue(false) }
    val bannersEnabled = MediatorLiveData<Boolean>().apply {
        postValue(false)
        addSource(isInitAdmobSdk) {
            if (it == true && getStatus()?.enabled == true) {
                postValue(true)
            }
        }
    }

    val lockUi: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply {
        postValue(false)
    }
    val pubdeskCharIterator: PubdeskCharIterator = PubdeskCharIterator()

    var interstitialTimer: InterstitialTimer = InterstitialTimer(adsStatus, needShowInterstitial)

    fun adsEnabled(): Boolean {
        return adsStatus.value?.enabled == true
    }

    fun isNeedShowAd(): Boolean {
        var needShowAd = false
        if (screenChangesNumber >= (getStatus()?.interstitial?.screenChange?.numberScreens ?: 0)
            && getStatus()?.enabled == true
            && getStatus()?.interstitial?.screenChange?.enable == true
        ) {
            screenChangesNumber = 0
            return true
        }
        if (getStatus()?.interstitialOnChannelChangeEnabled == true) {
            val countViewsBeforeAdShow = getCountViewsBeforeAdShow()
            if (countSwitches >= countViewsBeforeAdShow && countViewsBeforeAdShow != 0) {
                needShowAd = true
                countSwitches = -1
            }
        }
        if (getStatus()?.enabled != true) {
            needShowAd = false
        }
        return needShowAd
    }

    private fun getCountViewsBeforeAdShow(): Int =
        getStatus()?.interstitialOnChannelChangeNumberChannels ?: 0

    private fun isNeedShowInterstitialOnStart(): Boolean {
        val isNeedShow = (getStatus()?.enabled ?: false)
                && (getStatus()?.interstitialOnStartEnabled ?: false)
                && needShowAdOnStart
        Timber.d("isNeedShowInterstitialOnStart() isNeedShow ${isNeedShow}")
        return isNeedShow
    }

    private fun getInitInterstitialObject(
        ad: Ad,
        activity: Activity?,
        amsModel: AmsEventsViewModel?
    ): InitInterstitial? {
        return when (ad.name) {
            INTERSTITIAL_OGURY -> {
                if (ConsentManager.gdprApplies()) {
                    InitInterstitialOgury(ad, isInitOgurySdk, activity, amsModel, this)
                } else {
                    null
                }
            }
            INTERSTITIAL_ADMOB -> InitInterstitialAdmob(ad, activity, amsModel, this)
            INTERSTITIAL_GAM_PUBDESK -> InitInterstitialGAM(ad, activity, amsModel, this)
            INTERSTITIAL_GAM -> InitInterstitialGAM(ad, activity, amsModel, this)
            INTERSTITIAL_BFIO -> InitInterstitialBFIO(ad, activity, amsModel, this)
            INTERSTITIAL_PLACEHOLDER -> InitInterstitialPlaceholder(ad, activity, amsModel, this)
            REWARDED_TAPJOY -> InitRewardedTapjoy(
                ad,
                isInitTapoySdk,
                activity as FragmentActivity,
                amsModel,
                this,
                this
            )
            else -> null
        }
    }

    private fun getAmsViewModel(activity: Activity?): AmsEventsViewModel? {
        return if (activity is FragmentActivity) {
            ViewModelProvider(activity).get(AmsEventsViewModel::class.java)
        } else null
    }

    fun getStatus() = adsStatus.value

    fun openChannel() {
        adsStatus.doAsync {
            interstitialTimer?.apply {
                if (isShutdown()) {
                    start()
                }
            }
            countSwitches++
            Timber.d("AdsViewModel: countSwitches++ ($countSwitches) max (${getCountViewsBeforeAdShow()})")
            if (isNeedShowAd()) {
                needShowInterstitial.postValue(InterstitialTrigger.OnChannelChange)
            }
        }
    }

    fun initAdsSDK(activity: FragmentActivity?, priorities: Map<String, Ad>) {
        if (activity == null) return
        activity.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                IronSource.onPause(activity)
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                IronSource.onResume(activity)
            }
        })
        if (priorities.containsKey(INTERSTITIAL_OGURY)) {
            Timber.d("MainActivity: initAdsSDK() init OGURY")
            val assetKey = activity.getString(R.string.ogury_key)
            ConsentManager.ask(activity, assetKey, object : ConsentListener {
                override fun onComplete(answer: ConsentManager.Answer) {
                    activity.runOnUiThread(object : Runnable {
                        public override fun run() {
                            Log.d("Ogury", "ConsentManager onComplete")
                            Presage.getInstance().start(assetKey, activity)
                            isInitOgurySdk.postValue(true)
                            // We are automatically transferring the consent between the Ogury Choice Manager and the Ads SDK
                            // You can now call load method here
                            // Start here your others ad networks and transfert the consent using ConsentManager.isAccepted(...) or ConsentManager.getIabString() methods.
                        }
                    })
                }

                override fun onError(e: ConsentException) {
                    activity.runOnUiThread(object : Runnable {
                        public override fun run() {
                            Log.e("Ogury", "ConsentManager onError", e)
                            // No internet is an errors
                            Presage.getInstance().start(assetKey, activity)
                            isInitOgurySdk.postValue(false)
                            // Start here your other ad networks without giving the consent
                        }
                    })
                }
            })
        } else {
            Timber.d("MainActivity: initAdsSDK() ignore OGURY")
        }
//        if (priorities.containsKey(INTERSTITIAL_ON_APP_LOAD)) {
//            MobileAds.initialize(activity) {
//                Timber.d("MainActivity: initAdsSDK() init ADMOB ON_APP_LOAD")
//            }
//        } else {
//            Timber.d("MainActivity: initAdsSDK() ignore ADMOB ON_APP_LOAD")
//        }
        if (priorities.containsKey(INTERSTITIAL_ADMOB)
            || priorities.containsKey(INTERSTITIAL_GAM)
            || priorities.isContainsAdmobBanner()
            || priorities.containsKey(BANNER_GAM)
            || priorities.containsKey(BANNER_GAM_THEADSHOP)
        ) {
            Timber.d("MainActivity: initAdsSDK() init ADMOB")
            MobileAds.initialize(activity) {
                isInitAdmobSdk.postValue(true)
            } // activity.getString(R.string.admob_appid) - deprecated way
        } else {
            Timber.d("MainActivity: initAdsSDK() ignore ADMOB")
        }
        if (priorities.containsKey(REWARDED_TAPJOY) ||
            priorities.containsKey(INTERSTITIAL_TAPJOY)
        ) {
            Timber.d("MainActivity: initAdsSDK() init TAPJOY")
            val connectFlags = Hashtable<String, Any>().apply {
                if (AppParams.isDebug) {
                    put(
                        TapjoyConnectFlag.ENABLE_LOGGING,
                        "true"
                    ) // remember to turn this off for your production builds!
                }
            }
            Tapjoy.connect(activity,
                activity.getString(R.string.tapjoy_api_key),
                connectFlags,
                object : TJConnectListener {
                    override fun onConnectSuccess() {
                        Timber.d("TAPJOY AD TEST: TAPJOY onConnectSuccess()")
                        if (AppParams.isDebug) {
                            Tapjoy.setDebugEnabled(true)
                        }
                        isInitTapoySdk.postValue(true)
                    }

                    override fun onConnectFailure() {
                        Timber.d("TAPJOY AD TEST: TAPJOY onConnectFailure()")
                    }
                })
        } else {
            Timber.d("MainActivity: initAdsSDK() ignore TAPJOY")
        }
        if (
            true // todo BANNER_THEADSHOP, INTERSTITIAL_THEADSHOP
        ) {
            Timber.d("MainActivity: initAdsSDK() init THEADSHOP")
            PrebidMobile.setPrebidServerHost(
                Host.CUSTOM.apply {
                    hostUrl = "https://pb.theadshop.co/s/v1/openrtb2/auction"
                }
            )
            PrebidMobile.setPrebidServerAccountId("522c0ee6-0d0f-458a-84d6-3ff789381de5")
            PrebidMobile.setApplicationContext(getApplication())
        } else {
            Timber.d("MainActivity: initAdsSDK() ignore THEADSHOP")
        }
    }

    fun onBackPressed(): Boolean {

        return Chartboost.onBackPressed()

    }


//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event)
//    {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            // Do something.
//        }
//    }

    private fun Map<String, Ad>.isContainsAdmobBanner(): Boolean {
        return this.containsKey(BANNER_ADAPTIVE_ADMOB) ||
                this.containsKey(BANNER_320x50_ADMOB) ||
                this.containsKey(BANNER_320x100_ADMOB)
    }

    // for initializate interstitial and rewarded ads.
    private fun initAds(
        activity: Activity?,
        sortedAds: List<Ad>,
        addPlaceholder: Boolean = false,
        minSize: Int = 0
    ) {
        if (activity == null /* || interstitialTimer.executorIsNotNull()*/) return
        val ams = getAmsViewModel(activity)
        intertitialAds.clear()
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
            val interstitial = getInitInterstitialObject(ad, activity, ams)
            if (interstitial != null) {
                intertitialAds.add(ad to interstitial)
            }
        }
        var adKeysString = intertitialAds.map { it.second.adKey }.joinToString(" ")
        Timber.d("initAds() end $adKeysString")
        val amounts = ArrayList<Int>().apply {
            add(getStatus()?.interstitialOnStartNumberAds ?: 0)
            add(getStatus()?.interstitialOnChannelChangeNumberAds ?: 0)
            add(getStatus()?.interstitialOnTimerNumberAds ?: 0)
            add(minSize)
        }
        val amount = amounts.maxOrNull() ?: 0
        if (intertitialAds.isNotEmpty()
            && intertitialAds.size == 1
            && amount > 1
            && intertitialAds.first().second.adKey != REWARDED_TAPJOY
        ) {
            if (intertitialAds.size < amount) {
                for (index in 0 until amount - intertitialAds.size) {
                    val interstitial = intertitialAds.get(index)
                    val adKey = interstitial.second.adKey
                    val adPurposes = interstitial.first.adPurposes
                    val ad = ads.find { it.name == adKey && it.adPurposes.containsAll(adPurposes) }
                        ?: break
                    val adInterstitialObject = getInitInterstitialObject(ad, activity, ams) ?: break
                    intertitialAds.add(ad to adInterstitialObject)
                }
            }
        }
        adKeysString = intertitialAds.map { it.second.adKey }.joinToString(" ")
        Timber.d("initAds() end $adKeysString")
    }

    fun initAdsAll(activity: Activity?) {
        val status = getStatus() ?: return
        initAds(
            activity = activity,
            sortedAds = status.getSortedAdsAll(),
            minSize = 1
        )
    }

    fun showAdIfReady(
        activity: Activity?,
        trigger: InterstitialTrigger?,
        purposes: List<AdPurpose> = listOf(AdPurpose.MAIN, AdPurpose.GIVEAWAYS),
        finishListener: InitInterstitial.FinishListener? = null,
        rewardedListener: RewardedListener? = null
    ): Boolean {
        activity ?: return false
        Timber.d("showAdIfReady() purposes ${purposes.joinToString()}")
        val filteredInterstitials =
            intertitialAds.filter { it.first.adPurposes.containsAny(purposes) }
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
                    countSwitches = 0
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
        countSwitches = 0
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
            InterstitialTrigger.Type.OnStartApp -> getStatus()?.interstitialOnStartNumberAds
            InterstitialTrigger.Type.OnTimer -> getStatus()?.interstitialOnTimerNumberAds
            InterstitialTrigger.Type.OnChannelChange -> getStatus()?.interstitialOnChannelChangeNumberAds
            InterstitialTrigger.Type.OnScreenChange -> getStatus()?.interstitialOnScreenChangeNumberAds
            InterstitialTrigger.Type.OnVideoAdError -> 1
            else -> null
        }

        val purposes = listOf(AdPurpose.MAIN)
        val interstitialsDisabled =
            channelId != null && getStatus()?.isInterstitialsEnabled(channelId) == false
        var uiLocked = false

        if (activity == null
            || intertitialAds.isEmpty()
            || amount == null
            || amount == 0
            || interstitialsDisabled
        ) {
            Timber.d("showInterstitials(): no need run intertitial! interstitials count ${intertitialAds.size} run length ${amount} disabled ${interstitialsDisabled} ")
            onStartInterstitialTimer()
            onCompleted?.invoke()
            return
        }

        Timber.d("showInterstitials(): start $amount ads in sequence")
        val job = coroutineScope.launch(Dispatchers.Main) {
            val interstitialObjects = intertitialAds.filter {
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
            countSwitches = 0
            activity.unlockUi()
            onCompleted?.invoke()
        }
        job.start()
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
                        continuation.resume(true)
                    }

                    override fun onNotLoadedAds() {
                        continuation.resume(false)
                    }
                })
                ad.show(trigger)
                Timber.d("showInterstitial() show ${ad.adKey}")
                countSwitches = 0
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
        continuation.resume(true)
    }

    // banners

    fun getBannerManager(eventsModel: AmsEventsViewModel? = null): BannerManager? {
        val status = getStatus() ?: return null
        val banners = status.getSortedBanners()
        if (banners.isEmpty()) return null
        return BannerManager(banners, eventsModel)
    }

    fun loadBannerInContainer(
        activity: FragmentActivity,
        eventsModel: AmsEventsViewModel,
        bannerContainer: ViewGroup?,
        currentProgramDescription: MutableLiveData<ProgramDescription?>
    ) {
        bannerContainer ?: return
        val bannerManager = getBannerManager(eventsModel) ?: return
        bannerManager.loadBannerInContainer(activity, bannerContainer)
        currentProgramDescription.observe(activity, Observer {
            bannerContainer.isVisible = !(it != null && it.video is YouTube)
        })
    }

    // ima sdk (other methods in ImaAdsViewModel)

    fun getImaAds() = getStatus()?.ima

    fun getInfomercialAds() = getStatus()?.infomercial

    // EventsForInterstitialTimers callbacks
    override fun onFinishInitInterstitial() {
        Timber.d("onFinishInitInterstitial()")
        if (isNeedShowInterstitialOnStart()) {
            if (thread?.isAlive == true) {
                Timber.d("onFinishInitInterstitial(): already run")
            } else {
                interstitialTimer?.stop()
                Timber.d("onFinishInitInterstitial() run timer for interstitialOnStartDelay")
                thread = Thread(Runnable {
                    Timber.d("onFinishInitInterstitial() sleep for interstitialOnStart")
                    TimeUnit.SECONDS.sleep(getStatus()?.interstitialOnStartDelay ?: 0)
                    needShowAdOnStart = false
                    Timber.d("onFinishInitInterstitial() needShowInterstitialOnStart == true")
                    needShowInterstitial.postValue(InterstitialTrigger.OnStartApp)
                })
                thread?.start()
            }
        }
    }

    override fun onStartInterstitialTimer() {
        interstitialTimer?.start()
        Timber.d("EventsForInterstitialTimers: startInterstitialTimer()")
    }

    override fun onStopInterstitialTimer() {
        interstitialTimer?.stop()
        Timber.d("EventsForInterstitialTimers: stopInterstitialTimer()")
    }

    // RewardedListener callback
    override fun onRewarded() {

    }

    inner class PubdeskCharIterator {

        val chars = listOf<Char>('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j')
        var iterator = chars.listIterator()

        fun getChar(): Char {
            return if (iterator.hasNext()) {
                iterator.next()
            } else {
                iterator = chars.listIterator()
                iterator.next()
            }
        }

    }

}