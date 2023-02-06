package com.kokoconnect.android.util

import android.webkit.WebChromeClient
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.R
import timber.log.Timber


class PlayerChromeClient() : WebChromeClient() {
    private var customView: View? = null
    private var fullscreenContainer: ViewGroup? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var eventsListener: PlayerChromeClient.Listener? = null

    fun setEventsListener(listener: PlayerChromeClient.Listener?) {
        eventsListener = listener
    }

    fun setFullscreenContainer(newFullscreenContainer: ViewGroup?) {
        fullscreenContainer = newFullscreenContainer
    }

    override fun getDefaultVideoPoster(): Bitmap? {
        Timber.d("getDefaultVideoPoster()")
        return if (customView == null) {
            null
        } else {
            BitmapFactory.decodeResource(
                AiryTvApp?.instance.resources,
                R.drawable.cast_album_art_placeholder
            )
        }
    }

    override fun onHideCustomView() {
        Timber.d("onHideCustomView()")
        fullscreenContainer?.removeView(customView)
        customView = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        fullscreenContainer?.visibility = View.GONE
        eventsListener?.onHideCustomView()
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        Timber.d("onShowCustomView()")
        if (this.customView != null) {
            Timber.d("onShowCustomView() hide previous view")
            onHideCustomView()
            return
        }
        if (view != null) {
            Timber.d("onShowCustomView() setup new custom view")
            customView = view
            fullscreenCallback = callback
            fullscreenContainer?.visibility = View.VISIBLE
            fullscreenContainer?.addView(customView)
            eventsListener?.onShowCustomView(view, callback)
        }

    }

    override fun onProgressChanged(view: WebView?, progress: Int) {
        super.onProgressChanged(view, progress)
        eventsListener?.onLoadingProgressChanged(view, progress)
    }

    interface Listener {
        fun onLoadingProgressChanged(view: WebView?, progress: Int)
        fun onShowCustomView(customView: View?, callback: CustomViewCallback?)
        fun onHideCustomView()
    }
}