package com.kokoconnect.android.model.player.proxy

import android.content.Context
import com.kokoconnect.android.model.tv.ProgramDescription
import com.kokoconnect.android.model.vod.Content

interface ChromecastProxyBuilder {
    fun initChromecastProxy(listener: ChromecastConnectionListener)
}

interface ChromecastConnectionListener {
    fun onChromecastConnecting()
    fun onChromecastConnected(chromecastContext: ChromecastProxy)
    fun onChromecastDisconnected()
}

interface ChromecastProxy : PlayerProxy {
    fun openChannel(context: Context, programDescription: ProgramDescription)
    fun openContent(context: Context, content: Content)
}