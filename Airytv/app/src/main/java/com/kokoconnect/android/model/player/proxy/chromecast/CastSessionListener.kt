package com.kokoconnect.android.model.player.proxy.chromecast

import com.google.android.gms.cast.framework.CastSession

internal interface CastSessionListener {
    fun onCastSessionConnecting()
    fun onCastSessionConnected(castSession: CastSession)
    fun onCastSessionDisconnected(castSession: CastSession)
}