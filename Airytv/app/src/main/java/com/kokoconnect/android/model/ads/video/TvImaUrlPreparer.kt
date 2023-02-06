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
import com.kokoconnect.android.vm.tv.VideoAdTvGuideInfo
import kotlin.random.Random

class TvImaUrlPreparer {
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
        SHOW("%SHOW%"),
        CHANNEL("%CHANNEL%")
    }

    private var deviceId: String = ""
    private var appName = ""
    private var appBundle = ""
    private var appStoreUrl = ""
    private var width: String = ""
    private var height: String = ""
    private var userAgent: String = ""
    private var videoAdTvGuideInfo: VideoAdTvGuideInfo? = null
    private var ams: AmsEventsFacade? = null

    fun initialize(
        activity: FragmentActivity,
        videoAdTvGuideInfo: VideoAdTvGuideInfo?,
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
        this.videoAdTvGuideInfo = videoAdTvGuideInfo
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
            .replace(ImaMacros.SHOW, videoAdTvGuideInfo?.getShowName() ?: "")
            .replace(ImaMacros.CHANNEL, videoAdTvGuideInfo?.getChannelName() ?: "")
    }

    private fun getCacheBuster(): String =
        (Random.nextInt(0, Int.MAX_VALUE)).toString()
}

private fun String.replace(variable: TvImaUrlPreparer.ImaMacros, value: String): String {
    return this.replace(variable.constName, Uri.encode(value))
}