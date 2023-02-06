package com.kokoconnect.android.model.player.proxy.chromecast

import android.app.Activity
import android.content.Context
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.model.tv.ProgramDescription
import com.kokoconnect.android.model.player.proxy.ChromecastConnectionListener
import com.kokoconnect.android.model.player.proxy.ChromecastProxy
import com.kokoconnect.android.model.vod.Content
import com.kokoconnect.android.util.DateUtils
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.SessionManager
import com.google.gson.internal.bind.util.ISO8601Utils
import timber.log.Timber
import java.util.*
import kotlin.collections.HashSet

class ChromecastProxyImplementation(
    sessionManager: SessionManager,
    vararg connectionListeners: ChromecastConnectionListener
) : ChromecastProxy, ChromecastConnectionListener {
    private val chromecastConnectionListeners = HashSet<ChromecastConnectionListener>()

    companion object {
        fun builder(
            activity: Activity,
            connectionListeners: ChromecastConnectionListener
        ): ChromecastProxyImplementation {
            val manager = CastContext.getSharedInstance(activity).sessionManager
            return ChromecastProxyImplementation(manager, connectionListeners)
        }
    }

    private val chromecastManager =
        ChromecastManager(
            this,
            sessionManager,
            this.chromecastConnectionListeners
        )
    val helper = ChromecastProxyHelper(chromecastManager.chromecastCommunicationChannel)

    private var chromecastConnected = false

    init {
        connectionListeners.forEach { this.chromecastConnectionListeners.add(it) }
        chromecastManager.restoreSession()
        chromecastManager.addSessionManagerListener()
    }

    override fun openChannel(context: Context, programDescription: ProgramDescription) {
        val prefs = Preferences(context).Guide()
        val lastChannelNumber = prefs.getChromecastChannelNumber()
        val channelNumber = programDescription.channelNumber
        if (lastChannelNumber != channelNumber) {
            helper.openChannel(channelNumber)
            prefs.setChromecastChannelNumber(channelNumber)
        }
    }

    override fun openContent(context: Context, content: Content) {
        val prefs = Preferences(context).Guide()
        val contentId = content.id
        if (contentId != null) {
            helper.openContent(contentId)
        }
    }

//    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//    fun onResume() {
//        Timber.d("ChromecastProxy: onResume() chromecastManager.addSessionManagerListener()")
//        chromecastManager.addSessionManagerListener()
//    }
//
//    // if this is enabled the library can't know when a session is terminated (if the app is in the background), therefore it can't remove notifications etc.
//    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
//    fun onPause() {
//        Timber.d("ChromecastProxy: onPause() chromecastManager.removeSessionManagerListener()")
//        chromecastManager.removeSessionManagerListener()
//    }

    fun release() {
        endCurrentSession()
        chromecastManager.release()
        chromecastConnectionListeners.clear()
    }

    fun endCurrentSession() {
        chromecastManager.endCurrentSession()
    }

    override fun onChromecastConnecting() {
    }

    override fun onChromecastConnected(chromecastContext: ChromecastProxy) {
        chromecastConnected = true
    }

    override fun onChromecastDisconnected() {
        chromecastConnected = false
    }

    fun addChromecastConnectionListener(chromecastConnectionListener: ChromecastConnectionListener) =
        chromecastConnectionListeners.add(chromecastConnectionListener)

    fun removeChromecastConnectionListener(chromecastConnectionListener: ChromecastConnectionListener) =
        chromecastConnectionListeners.remove(chromecastConnectionListener)

    inner class ChromecastProxyHelper internal constructor(private val castChannel: ChromecastManager.CommunicationChannel) {
        fun openChannel(channelNumber: Int) {
            val message = ChromecastManager.JSONUtils.buildFlatJson(
                "command" to ChromecastCommunicationConstants.LOAD,
                "contentType" to "channel",
                "channelNumber" to channelNumber.toString(),
                "currentTime" to ISO8601Utils.format(Date(), false, TimeZone.getDefault()),
                "currentTimeZone" to DateUtils.getTimezoneString()
            )
            Timber.d("ChromecastProxyImplementation: openChannel() send message: $message")
            castChannel.sendMessage(message)
        }

        fun openContent(contentId: Long) {
            val message = ChromecastManager.JSONUtils.buildFlatJson(
                "command" to ChromecastCommunicationConstants.LOAD,
                "contentType" to "video",
                "contentId" to contentId.toString(),
                "currentTime" to ISO8601Utils.format(Date(), false, TimeZone.getDefault()),
                "currentTimeZone" to DateUtils.getTimezoneString()
            )
            Timber.d("ChromecastProxyImplementation: openContent() send message: $message")
            castChannel.sendMessage(message)
        }
    }

}