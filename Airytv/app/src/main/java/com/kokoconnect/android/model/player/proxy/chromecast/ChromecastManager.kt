package com.kokoconnect.android.model.player.proxy.chromecast

import com.kokoconnect.android.model.player.proxy.ChromecastConnectionListener
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import timber.log.Timber

internal object ChromecastCommunicationConstants {
    // receiver to sender
    const val INIT_COMMUNICATION_CONSTANTS = "INIT_COMMUNICATION_CONSTANTS"
    const val LOAD = "LOAD"
    fun asJson() = ChromecastManager.JSONUtils.buildFlatJson(
        LOAD to LOAD
    )
}

/**
 * Class responsible for chromecast sessions.
 */
class ChromecastManager(
    private val chromecastAirytvContext: ChromecastProxyImplementation,
    private val sessionManager: SessionManager,
    private val chromecastConnectionListeners: Set<ChromecastConnectionListener>) :
    CastSessionListener {

    val chromecastCommunicationChannel = object : CommunicationChannel {
        override val namespace get() = "urn:x-cast:com.airytv.android.chromecast.communication"
        override val observers = HashSet<CommunicationChannel.ChromecastChannelObserver>()

        override fun sendMessage(message: String) {
            try {
                sessionManager.currentCastSession
                    ?.sendMessage(namespace, message)
//                .setResultCallback {
//                    if(it.isSuccess)
//                        Log.d(this.javaClass.simpleName, "message sent")
//                    else
//                        Log.e(this.javaClass.simpleName, "failed, can't send message")
//                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
            val parsedMessage = JSONUtils.parseMessageFromReceiverJson(message)
            observers.forEach{ it.onMessageReceived(parsedMessage) }
        }

    }
    private val castSessionManagerListener =
        CastSessionManagerListener(
            this
        )

    override fun onCastSessionConnecting() {
        Timber.d("onCastSessionConnecting()")
        chromecastConnectionListeners.forEach { it.onChromecastConnecting() }
    }

    override fun onCastSessionConnected(castSession: CastSession) {
        Timber.d("onCastSessionConnected()")
        castSession.removeMessageReceivedCallbacks(chromecastCommunicationChannel.namespace)
        castSession.setMessageReceivedCallbacks(chromecastCommunicationChannel.namespace, chromecastCommunicationChannel)

        sendCommunicationConstants(chromecastCommunicationChannel)

        chromecastAirytvContext.onChromecastConnected(chromecastAirytvContext)
        chromecastConnectionListeners.forEach { it.onChromecastConnected(chromecastAirytvContext) }
    }

    override fun onCastSessionDisconnected(castSession: CastSession) {
        Timber.d("onCastSessionDisconnected()")
        castSession.removeMessageReceivedCallbacks(chromecastCommunicationChannel.namespace)

        chromecastAirytvContext.onChromecastDisconnected()
        chromecastConnectionListeners.forEach { it.onChromecastDisconnected() }
    }

    fun restoreSession() {
        val currentCastSessions = sessionManager.currentCastSession
        if(currentCastSessions != null)
            onCastSessionConnected(currentCastSessions)
    }

    fun endCurrentSession() {
        sessionManager.endCurrentSession(true)
    }

    fun addSessionManagerListener() = sessionManager.addSessionManagerListener(castSessionManagerListener, CastSession::class.java)
    fun removeSessionManagerListener() = sessionManager.removeSessionManagerListener(castSessionManagerListener, CastSession::class.java)

    fun release() {
        removeSessionManagerListener()
    }

    private fun sendCommunicationConstants(chromecastCommunicationChannel: CommunicationChannel) {
        val communicationConstants = ChromecastCommunicationConstants.asJson()

        val message = JSONUtils.buildCommunicationConstantsJson(
                "command" to ChromecastCommunicationConstants.INIT_COMMUNICATION_CONSTANTS,
                "communicationConstants" to communicationConstants
        )

        chromecastCommunicationChannel.sendMessage(message)
    }

    /**
     * Custom channel for full-duplex communication between sender and receiver, on a specific namespace.
     *
     * The channel can be observed with a [ChromecastChannelObserver]
     */
    interface CommunicationChannel : Cast.MessageReceivedCallback {
        val namespace: String
        val observers : HashSet<ChromecastChannelObserver>

        fun sendMessage(message: String)
        override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String)

        fun addObserver(channelObserver: ChromecastChannelObserver) = observers.add(channelObserver)
        fun removeObserver(channelObserver: ChromecastChannelObserver) = observers.remove(channelObserver)

        /**
         * Implement this interface to observe a [ChromecastCommunicationChannel]
         */
        interface ChromecastChannelObserver {
            fun onMessageReceived(messageFromReceiver: MessageFromReceiver)
        }

        class MessageFromReceiver(val type: String, val data: String)
    }

    /**
     * Utility class to read and parse JSON messages exchanged between sender and receiver.
     * The format of the messages is basic, no external library is needed.
     */
    object JSONUtils {
        fun buildFlatJson(vararg args: Pair<String, String>) : String {
            val jsonBuilder = StringBuilder("{")
            args.forEach { jsonBuilder.append("\"${it.first}\": \"${it.second}\",") }
            jsonBuilder.deleteCharAt(jsonBuilder.length-1)
            jsonBuilder.append("}")

            return jsonBuilder.toString()
        }

        fun buildCommunicationConstantsJson(command: Pair<String, String>, communicationConstants: Pair<String, String>) : String {
            val jsonBuilder = StringBuilder("{")
            jsonBuilder.append("\"${command.first}\": \"${command.second}\",")
            jsonBuilder.append("\"${communicationConstants.first}\": ${communicationConstants.second}")
            jsonBuilder.append("}")

            return jsonBuilder.toString()
        }

        fun parseMessageFromReceiverJson(json: String) : CommunicationChannel.MessageFromReceiver {
            val strings = json.split(",")
            val values = strings.map { it.split(":")[1].trim().replace("\"", "").replace("{", "").replace("}", "") }

            return ChromecastManager.CommunicationChannel.MessageFromReceiver(values[0], values[1])
        }
    }

}