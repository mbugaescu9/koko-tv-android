package com.kokoconnect.android.util

import android.content.pm.PackageManager
object InflatingErrorUtils {
    private const val GOOGLE_WEBVIEW_PACKAGE_NAME = "com.google.android.webview"

    fun isWebViewNotFound(ex: Exception?): Boolean {
        ex ?: return false
        var cause: Throwable? = ex.cause
        while (cause != null) {
            if (cause is PackageManager.NameNotFoundException && cause.message?.contains(GOOGLE_WEBVIEW_PACKAGE_NAME) == true) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

}