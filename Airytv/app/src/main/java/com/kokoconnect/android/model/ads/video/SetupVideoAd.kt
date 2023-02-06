package com.kokoconnect.android.model.ads.video


const val IGNORE_INT = -1
const val IGNORE_LONG = -1L
const val IGNORE_BOOLEAN = false

interface SetupVideoAd {
    fun imaVast(): SetupItemVideoAd
    fun infomercial(): SetupItemVideoAd
    fun isAnyEnabled(): Boolean
}

interface SetupItemVideoAd {
    fun enable(): Boolean
    fun numberOfAdsToServe(): Int
    fun numberOfChannels(): Int
    fun repeatIntervalSec(): Long
}

class EmptySetupVideoAd: SetupVideoAd {
    override fun imaVast(): SetupItemVideoAd = EmptySetupItemVideoAd()
    override fun infomercial(): SetupItemVideoAd = EmptySetupItemVideoAd()
    override fun isAnyEnabled(): Boolean = IGNORE_BOOLEAN
}

class EmptySetupItemVideoAd: SetupItemVideoAd {
    override fun enable(): Boolean = IGNORE_BOOLEAN
    override fun numberOfAdsToServe(): Int = IGNORE_INT
    override fun numberOfChannels(): Int = IGNORE_INT
    override fun repeatIntervalSec(): Long = IGNORE_LONG
}