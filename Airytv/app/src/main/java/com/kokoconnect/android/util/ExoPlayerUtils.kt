package com.kokoconnect.android.util

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ParserException
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.upstream.HttpDataSource

object ExoPlayerUtils {
    const val ERROR_VIDEO_UNAVAILABLE = "Video unavailable"

    fun isBehindLiveWindow(ex: ExoPlaybackException?): Boolean {
        ex ?: return false
        if (ex.type != ExoPlaybackException.TYPE_SOURCE) {
            return false
        }
        var cause: Throwable? = ex.sourceException
        while (cause != null) {
            if (cause is BehindLiveWindowException) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    fun isParserException(ex: ExoPlaybackException?): Boolean {
        ex ?: return false
        if (ex.type != ExoPlaybackException.TYPE_SOURCE) {
            return false
        }
        var cause: Throwable? = ex.sourceException
        while (cause != null) {
            if (cause is ParserException) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    fun isInvalidResponseCode(ex: ExoPlaybackException?): Boolean {
        ex ?: return false
        if (ex.type != ExoPlaybackException.TYPE_SOURCE) {
            return false
        }
        var cause: Throwable? = ex.sourceException
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                return true
            }
            cause = cause.cause
        }
        return false
    }
}

fun ExoPlaybackException.isBehindLiveWindow(): Boolean {
    val ex = this
    if (ex.type != ExoPlaybackException.TYPE_SOURCE) {
        return false
    }
    var cause: Throwable? = ex.sourceException
    while (cause != null) {
        if (cause is BehindLiveWindowException) {
            return true
        }
        cause = cause.cause
    }
    return false
}

fun ExoPlaybackException.isParserException(): Boolean {
    val ex = this
    if (ex.type != ExoPlaybackException.TYPE_SOURCE) {
        return false
    }
    var cause: Throwable? = ex.sourceException
    while (cause != null) {
        if (cause is ParserException) {
            return true
        }
        cause = cause.cause
    }
    return false
}

fun ExoPlaybackException.isInvalidResponseCode(): Boolean {
    val ex = this
    if (ex.type != ExoPlaybackException.TYPE_SOURCE) {
        return false
    }
    var cause: Throwable? = ex.sourceException
    while (cause != null) {
        if (cause is HttpDataSource.InvalidResponseCodeException) {
            return true
        }
        cause = cause.cause
    }
    return false
}