package com.kokoconnect.android.model.ads

import android.content.Context
import android.content.res.Configuration
import com.kokoconnect.android.model.ads.banner.Banner
import com.kokoconnect.android.model.ads.video.*
import com.kokoconnect.android.util.*
import java.util.*

class AdsStatus {

    constructor(adsEnabled: Boolean, ads: Ads? = null) {
        this.enabled = adsEnabled
        this.ads = ads
    }

    var enabled: Boolean = false
        private set
    var ads: Ads? = null
        private set(value) {
            value?.options?.let {
                this.options = it
            }
            field = value
        }
    private var options: AdsOptions = AdsOptions()

    val interstitial: InterstitialOptions
        get() = options.interstitial

    val interstitialOnStartEnabled: Boolean
        get() = options.interstitial.start.enable
    val interstitialOnStartDelay: Long
        get() = options.interstitial.start.timerInterval
    val interstitialOnStartNumberAds: Int
        get() = options.interstitial.start.numberAds

    val interstitialOnTimerEnabled: Boolean
        get() = options.interstitial.timer.enable
    val interstitialOnTimerInterval: Long
        get() = options.interstitial.timer.timerInterval
    val interstitialOnTimerNumberAds: Int
        get() = options.interstitial.timer.numberAds

    val interstitialOnChannelChangeEnabled: Boolean
        get() = options.interstitial.channelChange.enable
    val interstitialOnChannelChangeNumberAds: Int
        get() = options.interstitial.channelChange.numberAds
    val interstitialOnChannelChangeNumberChannels: Int
        get() = options.interstitial.channelChange.numberChannels

    val interstitialOnScreenChangeEnabled: Boolean
        get() = options.interstitial.screenChange.enable
    val interstitialOnScreenChangeNumberAds: Int
        get() = options.interstitial.screenChange.numberAds
    val interstitialOnScreenChangeNumberScreens: Int
        get() = options.interstitial.screenChange.numberScreens

    val vastOnFirstTuneEnabled: Boolean
        get() = options.vast.firstTune.enable
    val vastOnFirstTuneNumberAds: Int
        get() = options.vast.firstTune.numberAds

    val vastOnChannelChangeEnabled: Boolean
        get() = options.vast.channelChange.enable
    val vastOnChannelChangeNumberAds: Int
        get() = options.vast.channelChange.numberAds
    val vastOnChannelChangeNumberChannels: Int
        get() = options.vast.channelChange.numberChannels

    val vastOnProgramChangeEnabled: Boolean
        get() = options.vast.programChange.enable
    val vastOnProgramChangeNumberAds: Int
        get() = options.vast.programChange.numberAds

    val vastOnCueTonesEnabled: Boolean
        get() = options.vast.cueTones.enable
    val vastOnCueTonesNumberAds: Int
        get() = options.vast.cueTones.numberAds

    val vastOnTimerEnabled: Boolean
        get() = options.vast.timer.enable
    val vastOnTimerNumberAds: Int
        get() = options.vast.timer.numberAds
    val vastOnTimerInterval: Long
        get() = options.vast.timer.timerInterval

    val vastOnVodTimerEnabled: Boolean
        get() = options.vast.vodTimer.enable
    val vastOnVodTimerNumberAds: Int
        get() = options.vast.vodTimer.numberAds
    val vastOnVodTimerInterval: Long
        get() = options.vast.vodTimer.timerInterval

    val infomercialOnFirstTuneEnabled: Boolean
        get() = options.infomercial.firstTune.enable
    val infomercialOnFirstTuneNumberAds: Int
        get() = options.infomercial.firstTune.numberAds

    val infomercialOnChannelChangeEnabled: Boolean
        get() = options.infomercial.channelChange.enable
    val infomercialOnChannelChangeNumberAds: Int
        get() = options.infomercial.channelChange.numberAds
    val infomercialOnChannelChangeNumberChannels: Int
        get() = options.infomercial.channelChange.numberChannels

    val infomercialOnProgramChangeEnabled: Boolean
        get() = options.infomercial.programChange.enable
    val infomercialOnProgramChangeNumberAds: Int
        get() = options.infomercial.programChange.numberAds

    val infomercialOnCueTonesEnabled: Boolean
        get() = options.infomercial.cueTones.enable
    val infomercialOnCueTonesNumberAds: Int
        get() = options.infomercial.cueTones.numberAds

    val infomercialOnTimerEnabled: Boolean
        get() = options.infomercial.timer.enable
    val infomercialOnTimerNumberAds: Int
        get() = options.infomercial.timer.numberAds
    val infomercialOnTimerInterval: Long
        get() = options.infomercial.timer.timerInterval

    val priorities: Map<String, Ad>
        get() = ads?.priorities ?: emptyMap()
    val giveawaysPriorities: Map<String, Ad>
        get() = ads?.giveaways ?: emptyMap()


    val ima: List<Ima>?
        get() {
            val imaList = LinkedList<Ima>()
            ads?.ima?.forEach {
                imaList.add(Ima(it.key, it.value))
            }
            imaList.sortByDescending { it.priority }
            return imaList
        }
    val infomercial: List<Infomercial>?
        get() {
            val list = LinkedList<Infomercial>()
            ads?.infomercial?.forEach {
                list.add(
                    Infomercial(
                        it.key,
                        it.value
                    )
                )
            }
            list.sortByDescending { it.priority }
            return list
        }

    fun isCueTonesEnabled(channelId: Int): Boolean {
        return options.cueTonesChannels.contains(channelId)
    }

    fun isInterstitialsEnabled(channelId: Int): Boolean {
        return !options.noInterstitialChannels.contains(channelId)
    }

    fun getBannerFactorData(tag: String): Banner? {
        val item = getAd(tag) ?: return null
        return Banner(item.priority, tag, item.payload)
    }

    fun getSortedBanners(): List<Banner> {
        val sortedAds = getSortedAdsMain()
        return sortedAds.filter {
            when (it.name) {
                BANNER_ADAPTIVE_ADMOB, BANNER_320x50_ADMOB, BANNER_320x100_ADMOB, BANNER_GAM, BANNER_GAM_PUBDESK, BANNER_GAM_THEADSHOP -> true
                else -> false
            }
        }.map {
            Banner(
                it.priority,
                it.name!!,
                it.payload
            )
        }
    }

    fun isBannersEnabled(context: Context): Boolean {
        return enabled &&
                context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    fun getAllAds(): Map<String, Ad> {
        val allPriorities = HashMap<String, Ad>()
        allPriorities.putAll(priorities)
        allPriorities.putAll(giveawaysPriorities)
        ads?.ima?.let {
            allPriorities.putAll(it)
        }
        return allPriorities
    }

    fun getSortedAdsMain(): List<Ad> {
        val list = ArrayList<Ad>()
        val map = priorities ?: emptyMap()
        for (item in map) {
            if (item.value.priority > -1) {
                list.add(
                    item.value.apply {
                        name = item.key
                        adPurposes = listOf(AdPurpose.MAIN)
                    }
                )
            }
        }
        list.sortByDescending { it.priority }
        return list
    }

    fun getSortedAdsGiveaways(): List<Ad> {
        val list = ArrayList<Ad>()
        val map = giveawaysPriorities
        for (item in map) {
            if (item.value.priority > -1) {
                list.add(
                    item.value.apply {
                        name = item.key
                        adPurposes = listOf(AdPurpose.GIVEAWAYS)
                    }
                )
            }
        }
        list.sortByDescending { it.priority }
        return list
    }


    fun getSortedAdsAll(): List<Ad> {
        val list = ArrayList<Ad>()
        for (item in giveawaysPriorities) {
            if (item.value.priority > -1) {
                list.add(
                    item.value.apply {
                        name = item.key
                        adPurposes = listOf(AdPurpose.GIVEAWAYS)
                    }
                )
            }
        }
        for (item in priorities) {
            if (item.value.priority > -1) {
                list.add(
                    item.value.apply {
                        name = item.key
                        adPurposes = listOf(AdPurpose.MAIN)
                    }
                )
            }
        }
        list.sortByDescending { it.priority }
        return list
    }

    fun getAd(tag: String): Ad? {
        return ads?.priorities?.get(tag)
    }

    fun getSetupVideoAd(reason: VideoAdTrigger): SetupVideoAd {
        lateinit var ima: SetupItemVideoAd
        lateinit var infomercial: SetupItemVideoAd
        when (reason) {
            is VideoAdTrigger.OnTvFirstTune -> {
                ima = object : SetupItemVideoAd {
                    override fun enable(): Boolean = vastOnFirstTuneEnabled
                    override fun numberOfAdsToServe(): Int = vastOnFirstTuneNumberAds
                    override fun numberOfChannels(): Int = IGNORE_INT
                    override fun repeatIntervalSec(): Long = IGNORE_LONG
                }
                infomercial = object : SetupItemVideoAd {
                    override fun enable(): Boolean = infomercialOnFirstTuneEnabled
                    override fun numberOfAdsToServe(): Int = infomercialOnFirstTuneNumberAds
                    override fun numberOfChannels(): Int = IGNORE_INT
                    override fun repeatIntervalSec(): Long = IGNORE_LONG
                }
            }
            is VideoAdTrigger.OnTvCueTones -> {
                ima = object : SetupItemVideoAd {
                    override fun enable(): Boolean = vastOnCueTonesEnabled
                    override fun numberOfAdsToServe(): Int = vastOnCueTonesNumberAds
                    override fun numberOfChannels(): Int = IGNORE_INT
                    override fun repeatIntervalSec(): Long = IGNORE_LONG
                }
                infomercial = object : SetupItemVideoAd {
                    override fun enable(): Boolean = infomercialOnCueTonesEnabled
                    override fun numberOfAdsToServe(): Int = infomercialOnCueTonesNumberAds
                    override fun numberOfChannels(): Int = IGNORE_INT
                    override fun repeatIntervalSec(): Long = IGNORE_LONG
                }
            }
            is VideoAdTrigger.OnTvProgramChange -> {
                ima = object : SetupItemVideoAd {
                    override fun enable(): Boolean = vastOnProgramChangeEnabled
                    override fun numberOfAdsToServe(): Int = vastOnProgramChangeNumberAds
                    override fun numberOfChannels(): Int = IGNORE_INT
                    override fun repeatIntervalSec(): Long = IGNORE_LONG
                }
                infomercial = object : SetupItemVideoAd {
                    override fun enable(): Boolean = infomercialOnProgramChangeEnabled
                    override fun numberOfAdsToServe(): Int = infomercialOnProgramChangeNumberAds
                    override fun numberOfChannels(): Int = IGNORE_INT
                    override fun repeatIntervalSec(): Long = IGNORE_LONG
                }
            }
            is VideoAdTrigger.OnTvChannelChange -> {
                ima = object : SetupItemVideoAd {
                    override fun enable(): Boolean = vastOnChannelChangeEnabled
                    override fun numberOfAdsToServe(): Int = vastOnChannelChangeNumberAds
                    override fun numberOfChannels(): Int = vastOnChannelChangeNumberChannels
                    override fun repeatIntervalSec(): Long = IGNORE_LONG
                }
                infomercial = object : SetupItemVideoAd {
                    override fun enable(): Boolean = infomercialOnChannelChangeEnabled
                    override fun numberOfAdsToServe(): Int = infomercialOnChannelChangeNumberAds
                    override fun numberOfChannels(): Int = infomercialOnChannelChangeNumberChannels
                    override fun repeatIntervalSec(): Long = IGNORE_LONG
                }
            }
            is VideoAdTrigger.OnTvTimer -> {
                ima = object : SetupItemVideoAd {
                    override fun enable(): Boolean = vastOnTimerEnabled
                    override fun numberOfAdsToServe(): Int = vastOnTimerNumberAds
                    override fun numberOfChannels(): Int = IGNORE_INT
                    override fun repeatIntervalSec(): Long = vastOnTimerInterval
                }
                infomercial = object : SetupItemVideoAd {
                    override fun enable(): Boolean = infomercialOnTimerEnabled
                    override fun numberOfAdsToServe(): Int = infomercialOnTimerNumberAds
                    override fun numberOfChannels(): Int = IGNORE_INT
                    override fun repeatIntervalSec(): Long = infomercialOnTimerInterval
                }
            }
            is VideoAdTrigger.OnVodTimer -> {
                ima = object : SetupItemVideoAd {
                    override fun enable(): Boolean = vastOnVodTimerEnabled
                    override fun numberOfAdsToServe(): Int = vastOnVodTimerNumberAds
                    override fun numberOfChannels(): Int = IGNORE_INT
                    override fun repeatIntervalSec(): Long = vastOnVodTimerInterval
                }
                infomercial = object : SetupItemVideoAd {
                    override fun enable(): Boolean = false//infomercialOnTimerEnabled
                    override fun numberOfAdsToServe(): Int = infomercialOnTimerNumberAds
                    override fun numberOfChannels(): Int = IGNORE_INT
                    override fun repeatIntervalSec(): Long = infomercialOnTimerInterval
                }
            }
            else -> {
                ima = EmptySetupItemVideoAd()
                infomercial = EmptySetupItemVideoAd()
            }
        }
        return object : SetupVideoAd {
            override fun isAnyEnabled(): Boolean {
                return ima.enable() || infomercial.enable()
            }

            override fun imaVast(): SetupItemVideoAd = ima
            override fun infomercial(): SetupItemVideoAd = infomercial
        }
    }

}