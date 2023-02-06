package com.kokoconnect.android.util

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.kokoconnect.android.R


class ContentUrlPreparer {
    enum class ContentMacros(val paramName: String, val constName: String) {
        DEVICE_ID("did", "{{did}}"),
        APP_NAME("app_name", "{{app_name}}"),
        APP_BUNDLE("app_bundle", "{{app_bundle}}"),
        APP_STORE_URL("app_store_url", "{{app_store_url}}")
    }

    private var deviceId = ""
    private var appName = ""
    private var appBundle = ""
    private var appStoreUrl = ""

    fun initialize(activity: FragmentActivity) {
        deviceId = DeviceUtils.getDeviceUUID(activity).toString()
        GPSUtils.googleAdsId.observe(activity, Observer { adsId ->
            adsId?.let { deviceId = it }
        })
        appBundle = AppParams.appId
        appName = activity.getString(R.string.app_name)
        appStoreUrl = AppParams.appStoreUrl
    }

    fun prepareContentUrl(url: String?): String? {
        url ?: return null
        val preparedUrl = url.replace(ContentMacros.DEVICE_ID.constName, deviceId)
            .replace(ContentMacros.APP_NAME.constName, appName)
            .replace(ContentMacros.APP_BUNDLE.constName, appBundle)
            .replace(ContentMacros.APP_STORE_URL.constName, AppParams.appStoreUrl)
        return if (NetworkUtils.isSameDomainName(preparedUrl, DOMAIN_CLOUDFRONT)) {
            val uri = Uri.parse(preparedUrl)
            val uriBuilder = uri.buildUpon()
            if (!uri.hasQueryParameter(ContentMacros.DEVICE_ID.paramName)) {
                uriBuilder.appendQueryParameter(ContentMacros.DEVICE_ID.paramName, deviceId)
            }
            if (!uri.hasQueryParameter(ContentMacros.APP_NAME.paramName)) {
                uriBuilder.appendQueryParameter(ContentMacros.APP_NAME.paramName, appName)
            }
            if (!uri.hasQueryParameter(ContentMacros.APP_BUNDLE.paramName)) {
                uriBuilder.appendQueryParameter(ContentMacros.APP_BUNDLE.paramName, appBundle)
            }
            if (!uri.hasQueryParameter(ContentMacros.APP_STORE_URL.constName)) {
                uriBuilder.appendQueryParameter(ContentMacros.APP_STORE_URL.paramName, appStoreUrl)
            }

            uriBuilder.build().toString()
        } else {
            preparedUrl
        }
    }
}
