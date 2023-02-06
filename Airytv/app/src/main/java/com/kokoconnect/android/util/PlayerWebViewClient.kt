package com.kokoconnect.android.util

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import timber.log.Timber

class PlayerWebViewClient: WebViewClient() {
    var eventsListener: Listener? = null

    override fun shouldOverrideUrlLoading(webView: WebView?, request: WebResourceRequest?): Boolean {
        eventsListener?.onInterceptUrlLoading(webView, request)
        Timber.d("shouldOverrideUrlLoading()")
        return super.shouldOverrideUrlLoading(webView, request)
    }



    interface Listener {
        fun onInterceptUrlLoading(view: WebView?, request: WebResourceRequest?)
    }
}