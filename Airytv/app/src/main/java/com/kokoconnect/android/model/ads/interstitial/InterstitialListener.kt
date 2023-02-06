package com.kokoconnect.android.model.ads.interstitial

interface InterstitialListener {
    fun onStartInterstitialTimer()
    fun onStopInterstitialTimer()
    fun onFinishInitInterstitial() // use for run ad when start app
}