package com.kokoconnect.android.util

import android.annotation.SuppressLint
import android.util.Log
import com.kokoconnect.android.BuildConfig
import java.lang.Exception

@SuppressLint("LogNotTimber")
/** Singletone object to manage all app build settings in one place. You can place all BuildConfig calls here. **/
object AppParams {
    val isDebug = BuildConfig.DEBUG
    const val appId: String = BuildConfig.APPLICATION_ID
    const val versionCode: Int = BuildConfig.VERSION_CODE
    const val versionName: String = BuildConfig.VERSION_NAME
    const val platform: String = BuildConfig.PLATFORM
    const val adPlatform: String = BuildConfig.AD_PLATFORM
    const val amsPlatform: String = BuildConfig.AMS_PLATFORM
    const val apiVersion: String = "v2.1.7"
    const val serverApiUrl: String = BuildConfig.SERVER_API_URL
    const val serverAmsApiUrl: String = BuildConfig.SERVER_AMS_EVENTS_URL
    const val fileProviderAuthority = "${BuildConfig.APPLICATION_ID}.fileprovider"
    const val appStoreUrl: String = BuildConfig.APP_STORE_URL

    const val isChromecastTvEnabled: Boolean = true
    const val isChromecastVodEnabled: Boolean = true

    val firebaseId: String
    const val googleWebClientId: String = BuildConfig.GOOGLE_WEB_CLIENTE_ID
    val appType: AppTypes

    init {
        appType = when {
            versionName.contains("ngc") -> {
                AppTypes.NONGPS_MOBILE
            }
            versionName.contains("as") -> {
                AppTypes.AMAZON_MOBILE
            }
            else -> {
                AppTypes.GPS_MOBILE
            }
        }
        firebaseId = if (appType.isServerRelease) {
            FIREBASE_ID_RELEASE // firebase release
        } else {
            FIREBASE_ID_DEMO // firebase demo
        }
        if (isDebug) {
            Log.d("AppParams", "${toString()}")
        }
    }


    override fun toString(): String {
        return try {
            AppParams::class.java.declaredFields.filter {
                it.name != "INSTANCE"
            }.map {
                try {
                    "${it.name} = ${it.get(AppParams)}"
                } catch (ex: Exception) {
                    null
                }
            }.filterNotNull().joinToString("; ")
        } catch (ex: Exception) {
            ""
        }
    }

    enum class AppTypes(
        val hasGooglePlayServices: Boolean,
        var isServerRelease: Boolean = BuildConfig.IS_RELEASE_SERVER
    ) {
        GPS_MOBILE(true),
        NONGPS_MOBILE(false),
        AMAZON_MOBILE(false)
    }
}