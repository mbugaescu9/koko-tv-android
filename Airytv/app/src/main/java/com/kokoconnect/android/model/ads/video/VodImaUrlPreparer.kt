package com.kokoconnect.android.model.ads.video

import android.net.Uri
import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.kokoconnect.android.R
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.util.DeviceUtils
import com.kokoconnect.android.util.GPSUtils
import com.kokoconnect.android.util.NetworkUtils
import com.kokoconnect.android.vm.vod.VideoAdVodContentInfo
import kotlin.random.Random

class VodImaUrlPreparer {
    enum class ImaMacros(val constName: String) {
        USER_AGENT("%USER_AGENT%"),
        APP_NAME("%APP_NAME%"),
        APP_BUNDLE("%APP_BUNDLE%"),
        ANDROID_ID("%ANDROID_ID%"),
        CACHE_BUSTER("%CACHE_BUSTER%"),
        IP("%IP%"),
        SCREEN_WIDTH("%SCREEN_WIDTH%"),
        SCREEN_HEIGHT("%SCREEN_HEIGHT%"),
        DEVICE_OS_VERSION("%DEVICE_OS_VERSION%"),
        DEVICE_MODEL("%DEVICE_MODEL%"),
        DEVICE_MANUFACTURER("%DEVICE_MANUFACTURER%"),
        CONTENT_NAME("%CONTENT_NAME%"),
        CONTENT_TYPE("%CONTENT_TYPE%"),
    }

    private var deviceId: String = ""
    private var appName = ""
    private var appBundle = ""
    private var appStoreUrl = ""
    private var width: String = ""
    private var height: String = ""
    private var userAgent: String = ""
    private var videoAdContentInfo: VideoAdVodContentInfo? = null
    private var ams: AmsEventsFacade? = null

    fun initialize(
        activity: FragmentActivity,
        videoAdGuideInfo: VideoAdVodContentInfo?,
        ams: AmsEventsFacade?
    ) {
        deviceId = DeviceUtils.getDeviceUUID(activity).toString()
        GPSUtils.googleAdsId.observe(activity, Observer { adsId ->
            adsId?.let { deviceId = it }
        })
        appBundle = AppParams.appId
        appName = activity.getString(R.string.app_name)
        appStoreUrl = AppParams.appStoreUrl
        val displaySize = DeviceUtils.getDisplaySize(activity.windowManager)
        width = displaySize.x.toString()
        height = displaySize.y.toString()
        userAgent = NetworkUtils.getWebViewUserAgent(activity)
        this.videoAdContentInfo = videoAdGuideInfo
        this.ams = ams
    }

    fun prepareImaUrl(url: String?): String? {
        url ?: return null
        return url.replace(ImaMacros.USER_AGENT, userAgent)
            .replace(ImaMacros.APP_NAME, appName)
            .replace(ImaMacros.APP_BUNDLE, appBundle)
            .replace(ImaMacros.ANDROID_ID, deviceId)
            .replace(ImaMacros.CACHE_BUSTER, getCacheBuster())
            .replace(ImaMacros.IP, ams?.getExternalIp() ?: "")
            .replace(ImaMacros.SCREEN_WIDTH, width)
            .replace(ImaMacros.SCREEN_HEIGHT, height)
            .replace(ImaMacros.DEVICE_MANUFACTURER, Build.MANUFACTURER)
            .replace(ImaMacros.DEVICE_MODEL, Build.MODEL)
            .replace(ImaMacros.DEVICE_OS_VERSION, Build.VERSION.SDK_INT.toString())
            .replace(ImaMacros.CONTENT_NAME, videoAdContentInfo?.getContentName() ?: "")
            .replace(ImaMacros.CONTENT_TYPE, videoAdContentInfo?.getContentName() ?: "")
    }

    private fun getCacheBuster(): String =
        (Random.nextInt(0, Int.MAX_VALUE)).toString()
}

private fun String.replace(variable: VodImaUrlPreparer.ImaMacros, value: String): String {
    return this.replace(variable.constName, Uri.encode(value))
}