package com.kokoconnect.android.model.player.proxy

import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.kokoconnect.android.R
import com.kokoconnect.android.model.player.PlayerObject
import com.kokoconnect.android.model.player.PlayerType
import com.kokoconnect.android.model.player.WebVideo
import com.kokoconnect.android.util.DateUtils
import com.kokoconnect.android.util.PlayerChromeClient
import com.kokoconnect.android.util.PlayerWebViewClient
import timber.log.Timber

class WebPlayerProxy(val listener: PlayerProxyListener) : InnerPlayerProxy() {
    class Params(
        override var playerView: View?,
        var fullscreenContainerView: ViewGroup?,
        override var uiControllerEnabled: Boolean
    ) : InnerPlayerProxy.Params()

    private var webView: WebView? = null
    var currentContent: WebVideo? = null
    private var isPaused: Boolean = false
    private var isFocus: Boolean = false

    private val webViewClient = PlayerWebViewClient()
    private val chromeClient = PlayerChromeClient()
    private val chromePlayerListener = object : PlayerChromeClient.Listener {
        override fun onLoadingProgressChanged(view: WebView?, progress: Int) {
            if (progress < 70) {
                listener.onBufferingProgressEnabled(this@WebPlayerProxy, true)
            } else {
                listener.onBufferingProgressEnabled(this@WebPlayerProxy, false)
            }
        }

        override fun onShowCustomView(
            customView: View?,
            callback: WebChromeClient.CustomViewCallback?
        ) {

        }

        override fun onHideCustomView() {

        }
    }
    private val webViewClientListener = object : PlayerWebViewClient.Listener {
        override fun onInterceptUrlLoading(view: WebView?, request: WebResourceRequest?) {}
    }
    private val webViewTouchListener = object : View.OnTouchListener {
        var point: PointF = PointF(0f, 0f)
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (event.pointerCount > 1) {
                return true
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    point.x = event.x
                }
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    point.y = event.y
                    event.setLocation(point.x, point.y)
                }
            }
            return false
        }
    }

    fun setFullscreenContainer(fullscreenView: ViewGroup?) {
        chromeClient.setFullscreenContainer(fullscreenView)
    }

    override fun isStopWithError(): Boolean = false

    override fun setup(params: InnerPlayerProxy.Params?) {
        val webParams = params as? Params ?: return
        setPlayerView(webParams.playerView)
        setUiControllerEnabled(webParams.uiControllerEnabled)
        setFullscreenContainer(webParams.fullscreenContainerView)
    }

    override fun setPlayerView(playerView: View?) {
        webView = playerView as? WebView
        webView?.settings?.apply {
            this.javaScriptEnabled = true
            this.javaScriptCanOpenWindowsAutomatically = true
            this.mediaPlaybackRequiresUserGesture = false
            this.domStorageEnabled = true
            this.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            this.useWideViewPort = true
            this.loadWithOverviewMode = true
            this.cacheMode = WebSettings.LOAD_NO_CACHE
            this.allowFileAccess = true
//            this.setAppCacheEnabled(true)
//            setAppCachePath(AiryTvApp.instance.cacheDir.absolutePath)
//            setBuiltInZoomControls(true)
//            setDisplayZoomControls(false)

            Timber.d("setupWebView() old user agent: ${userAgentString}")
            this.userAgentString =
                "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Mobile Safari/537.36"
            Timber.d("setupWebView() new user agent: ${userAgentString}")
        }
        chromeClient.setEventsListener(chromePlayerListener)
        webViewClient.eventsListener = webViewClientListener
        webView?.webChromeClient = chromeClient
        webView?.webViewClient = webViewClient
        webView?.setBackgroundResource(R.color.colorPrimaryDark)
        webView?.isHorizontalScrollBarEnabled = false
//        wvContentPlayer.setInitialScale(1)
        //horizontal scroll disabling
        webView?.setOnTouchListener(webViewTouchListener)
        reopenVideo()
    }

    override fun setUiControllerEnabled(isEnabled: Boolean) {  }

    override fun openVideo(video: PlayerObject, ignoreCheck: Boolean) {
        val webVideo = video as? WebVideo
        isPaused = false
        val videoUrl = webVideo?.videoUrl
        if (videoUrl == null || videoUrl.isEmpty()) return
        val currentVideoUrl = getCurrentVideoUrl()
        if (ignoreCheck || currentVideoUrl == null || !videoUrl.contains(currentVideoUrl)) {
            webView?.loadUrl(videoUrl)
        }
        play()
    }

    private fun reopenVideo() {
        currentContent?.let{
            openVideo(it)
        }
    }

    override fun getCurrentPosition(): Long {
        val startTimeMs = currentContent?.getStartTimeMs()
        return if (startTimeMs != null && startTimeMs > 0L) {
            DateUtils.getCurrentDate() - startTimeMs
        } else {
            0L
        }
    }

    private fun getCurrentVideoUrl(): String? {
        return currentContent?.getUrl()?.toString()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onStart() {
        reopenVideo()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        isFocus = true
        if (!isPaused) {
            webView?.onResume()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        isFocus = false
        webView?.onPause()
    }

    override fun getType(): PlayerType = PlayerType.WEB

    override fun isPlaying(): Boolean {
        return !isPaused && isFocus
    }

    override fun play() {
        isPaused = false
        webView?.onResume()
    }

    override fun pause() {
        isPaused = true
        webView?.onPause()
    }

    override fun stop() {
        pause()
    }

    override fun release() {
        stop()
    }
}