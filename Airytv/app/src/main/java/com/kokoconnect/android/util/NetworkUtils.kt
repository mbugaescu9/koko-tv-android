package com.kokoconnect.android.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URI
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext


enum class NetworkConnectionType() {
    WIFI(),
    MOBILE(),
    ETHERNET(),
    NONE()
}
object NetworkUtils {
    private val connectivityManager = AiryTvApp.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Suppress("DEPRECATION")
    fun getNetworkConnectionType(): NetworkConnectionType {
        var connectionType = NetworkConnectionType.NONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val actNw = connectivityManager.activeNetwork ?: return connectionType
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(actNw) ?: return connectionType
            connectionType = when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    NetworkConnectionType.WIFI
                }
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    NetworkConnectionType.MOBILE
                }
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    NetworkConnectionType.ETHERNET
                }
                else -> NetworkConnectionType.NONE
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    connectionType = when (type) {
                        ConnectivityManager.TYPE_WIFI -> {
                            NetworkConnectionType.WIFI
                        }
                        ConnectivityManager.TYPE_MOBILE -> {
                            NetworkConnectionType.MOBILE
                        }
                        ConnectivityManager.TYPE_ETHERNET -> {
                            NetworkConnectionType.ETHERNET
                        }
                        else -> {
                            NetworkConnectionType.NONE
                        }
                    }

                }
            }
        }
        return connectionType
    }

    fun isInternetAvailable(): Boolean {
        return when (getNetworkConnectionType()) {
            NetworkConnectionType.NONE -> false
            else -> true
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun observeEthernet() = callbackFlowForType(NetworkCapabilities.TRANSPORT_ETHERNET).asLiveData()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun observeMobile() = callbackFlowForType(NetworkCapabilities.TRANSPORT_CELLULAR).asLiveData()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun observeWifi() = callbackFlowForType(NetworkCapabilities.TRANSPORT_WIFI).asLiveData()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun observeInternet() = MediatorLiveData<Boolean>().apply {
        addSource(observeWifi(), Observer {
            this.postValue(it)
        })
        addSource(observeEthernet(), Observer {
            this.postValue(it)
        })
        addSource(observeMobile(), Observer {
            this.postValue(it)
        })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun callbackFlowForType(type: Int) = callbackFlow {
//        offer(false)
        this.trySend(false).isSuccess
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(type)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                this@callbackFlow.trySend(false).isSuccess
//                offer(false)
            }

            override fun onUnavailable() {
                this@callbackFlow.trySend(false).isSuccess
//                offer(false)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                // do nothing
            }

            override fun onAvailable(network: Network) {
                //offer(true)
                this@callbackFlow.trySend(true).isSuccess
            }
        }
        connectivityManager.registerNetworkCallback(networkRequest, callback)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    fun isSameDomainName(url: String?, domainName: String?): Boolean {
        return url != null
                && domainName != null
                && getDomainName(url)?.contains(domainName, true) ?: false
    }


    fun getDomainName(url: String?) : String? {
        url ?: return null
        val uri = URI(url.replace(" ", "%20"))
        val domain = uri.getHost()
        domain ?: return null
        return if (domain.startsWith("www.")) {
            domain.substring(4)
        } else {
            domain
        }
    }

    fun getWebViewUserAgent(context: Context): String {
        var userAgent = ""
        try {
            userAgent = WebView(context).settings.userAgentString
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return userAgent
    }

    fun getIPAddress(useIPv4: Boolean): String? {
        try {
            val interfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs: List<InetAddress> =
                    Collections.list(intf.getInetAddresses())
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress()) {
                        val sAddr: String = addr.getHostAddress()
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (useIPv4) {
                            if (isIPv4) return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                                return if (delim < 0) sAddr.toUpperCase() else sAddr.substring(
                                    0,
                                    delim
                                ).toUpperCase()
                            }
                        }
                    }
                }
            }
        } catch (ignored: java.lang.Exception) {
        } // for now eat exceptions
        return ""
    }

    fun setupSSLConnection(context: Context) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            //network security config not works for all supported android versions
            //but there is some channels with SSLHandshakeException
            //the only way to solve this for now is to add certificates programmatically
            try {
                val cf = CertificateFactory.getInstance("X.509")
                val caInput = context.resources.openRawResource(R.raw.cert_tikilive)
                var ca: Certificate
                try {
                    ca = cf.generateCertificate(caInput)
                } finally {
                    caInput.close()
                }
                // Create a KeyStore containing our trusted CAs
                val keyStoreType = KeyStore.getDefaultType()
                val keyStore = KeyStore.getInstance(keyStoreType)
                keyStore.load(null, null)
                keyStore.setCertificateEntry("ca", ca)

                val trustManagers = CompositeX509TrustManager.getTrustManagers(keyStore)

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustManagers, SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}