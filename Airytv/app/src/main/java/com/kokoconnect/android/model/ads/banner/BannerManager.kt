package com.kokoconnect.android.model.ads.banner

import android.app.Activity
import android.view.ViewGroup
import com.kokoconnect.android.util.*
import com.kokoconnect.android.vm.AmsEventsViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.suspendCoroutine

class BannerManager(
    val banners: List<Banner>,
    private val eventsViewModel: AmsEventsViewModel? = null
) {
    fun loadBannerInContainer(
        activity: Activity,
        container: ViewGroup,
        loadingListener: BannerLoadingListener? = null,
        coroutineScope: CoroutineScope = GlobalScope
    ) {
        coroutineScope.launch {
            val iterator = banners.listIterator()
            var bannerLoaded = false
            while (!bannerLoaded && iterator.hasNext()) {
                val currentBannerData = iterator.next()
                bannerLoaded = loadBanner(activity, container, currentBannerData, coroutineScope)
            }
            loadingListener?.onFinished(bannerLoaded)
        }
    }

    private fun createBannerObject(bannerData: Banner): InitBanner? {
        return when (bannerData.tag) {
            BANNER_ADAPTIVE_ADMOB, BANNER_320x50_ADMOB, BANNER_320x100_ADMOB -> InitBannerAdmob(
                bannerData,
                eventsViewModel
            )
            BANNER_GAM, BANNER_GAM_PUBDESK -> InitBannerGAM(bannerData, eventsViewModel)
            BANNER_GAM_THEADSHOP -> InitBannerTheadshop(bannerData, eventsViewModel)
            else -> null
        }
    }

    private suspend fun loadBanner(
        activity: Activity,
        container: ViewGroup,
        bannerData: Banner,
        coroutineScope: CoroutineScope
    ): Boolean = suspendCoroutine { continuation ->
        createBannerObject(bannerData)?.apply {
            setFinishLoadingListener(object: InitBanner.LoadingListener{
                override fun onLoaded() {
                    continuation.resumeWith(Result.success(true))
                }
                override fun onLoadError() {
                    continuation.resumeWith(Result.success(false))
                }
            })
            coroutineScope.launch(Dispatchers.Main) {
                load(activity, container)
            }
        }
    }


    interface BannerLoadingListener {
        fun onFinished(loaded: Boolean)
    }

}