package com.kokoconnect.android.model.ads.interstitial

sealed class InterstitialTrigger(val reasonName: String, val type: Type) {
    object OnStartApp : InterstitialTrigger("OnStartApp", Type.OnStartApp)
    object OnScreenChange : InterstitialTrigger("OnScreenChange", Type.OnScreenChange)
    object OnTimer : InterstitialTrigger("OnTimer", Type.OnTimer)
    object OnChannelChange : InterstitialTrigger("OnChannelChange", Type.OnChannelChange)
    object OnVideoAdError : InterstitialTrigger("OnVideoAdError", Type.OnVideoAdError)
    object OnGetMoreTickets : InterstitialTrigger("OnGetMoreTickets", Type.OnGetMoreTickets)

    enum class Type() {
        OnStartApp,
        OnScreenChange,
        OnTimer,
        OnChannelChange,
        OnVideoAdError,
        OnGetMoreTickets
    }
}