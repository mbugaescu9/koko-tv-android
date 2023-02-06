package com.kokoconnect.android.model.ads.interstitial.placeholder

interface InterstitialListener{
    fun onAdLoaded()
    fun onAdClosed()
    fun onAdClicked()
    fun onAdOpened()
}