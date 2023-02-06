package com.kokoconnect.android.model.ads.banner

import android.app.Activity
import android.view.ViewGroup
import com.kokoconnect.android.model.event.AdEventParams
import com.kokoconnect.android.util.AD_TYPE_BANNER
import com.kokoconnect.android.vm.AmsEventsViewModel

abstract class InitBanner(
    val bannerData: Banner,
    val amsModel: AmsEventsViewModel?
) {

    interface LoadingListener {
        fun onLoaded()
        fun onLoadError()
    }

    private var onFinishLoading: LoadingListener? = null

    fun setFinishLoadingListener(listener: LoadingListener) {
        this.onFinishLoading = listener
    }

    abstract fun getAdUnitId(): String?

    open fun onLoaded() {
        amsModel?.sendAdEventLoaded(
            AdEventParams(
                adKey = bannerData.tag,
                adDescription = getAdUnitId(),
                adClick = "main",
                adType = AD_TYPE_BANNER
            )
        )
        onFinishLoading?.onLoaded()
    }

    open fun onError(error: String) {
        amsModel?.sendAdEventLoadFail(
            AdEventParams(
                adKey = bannerData.tag,
                adDescription = getAdUnitId(),
                adClick = error,
                adType = AD_TYPE_BANNER
            )
        )
        onFinishLoading?.onLoadError()
    }

    open fun onClicked() {
        amsModel?.sendAdEventClicked(
            AdEventParams(
                adKey = bannerData.tag,
                adDescription = getAdUnitId(),
                adClick = "main",
                adType = AD_TYPE_BANNER
            )
        )
    }

    open fun load(activity: Activity, targetView: ViewGroup) {}
}