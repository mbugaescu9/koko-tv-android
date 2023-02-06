package com.kokoconnect.android.util

import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class CompositeX509TrustManager : X509TrustManager {
    private var trustManagers: List<X509TrustManager?>

    constructor(trustManagers: List<X509TrustManager>) {
        this.trustManagers = trustManagers
    }

    constructor(keystore: KeyStore) {
        this.trustManagers = listOf(defaultTrustManager, getTrustManager(keystore))
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        val certificates = mutableListOf<X509Certificate>()
        for (trustManager in trustManagers) {
            trustManager ?: continue
            for (cert in trustManager.acceptedIssuers) {
                certificates.add(cert)
            }
        }
        return certificates.toTypedArray()
    }

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        for (trustManager in trustManagers) {
            try {
                trustManager?.checkClientTrusted(chain, authType)
                return  // someone trusts them. success!
            } catch (e: CertificateException) {
                // maybe someone else will trust them
            }
        }
        throw CertificateException("None of the TrustManagers trust this certificate chain")
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        for (trustManager in trustManagers) {
            try {
                trustManager?.checkServerTrusted(chain, authType)
                return  // someone trusts them. success!
            } catch (e: CertificateException) {
                // maybe someone else will trust them
            }
        }
        throw CertificateException("None of the TrustManagers trust this certificate chain")
    }

    companion object {
        fun getTrustManagers(keyStore: KeyStore): Array<TrustManager> {
            return arrayOf<TrustManager>(CompositeX509TrustManager(keyStore))
        }

        val defaultTrustManager: X509TrustManager?
            get() = getTrustManager(null)

        fun getTrustManager(keystore: KeyStore?): X509TrustManager? {

            return getTrustManager(TrustManagerFactory.getDefaultAlgorithm(), keystore)

        }

        fun getTrustManager(algorithm: String, keystore: KeyStore?): X509TrustManager? {

            val factory: TrustManagerFactory

            try {
                factory = TrustManagerFactory.getInstance(algorithm)
                factory.init(keystore)
                return factory.trustManagers.filter { it is X509TrustManager }.firstOrNull() as? X509TrustManager
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: KeyStoreException) {
                e.printStackTrace()
            }
            return null
        }
    }

}