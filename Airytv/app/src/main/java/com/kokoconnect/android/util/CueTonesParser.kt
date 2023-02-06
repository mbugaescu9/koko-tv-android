package com.kokoconnect.android.util

import timber.log.Timber
import java.lang.Exception
import kotlin.math.roundToInt

object CueTonesParser {
    private val TAG_PREFIX_CUE_OUT_CONT = "#EXT-X-CUE-OUT-CONT:"
    private val TAG_PREFIX_CUE_OUT = "#EXT-X-CUE-OUT:"
    private val TAG_PREFIX_CUE = "#EXT-X-CUE:"

    fun parseDuration(tag: String): Int? {
        var duration: Int? = null
        Timber.d("parseDuration() tag = ${tag}")
        val prefix = if (tag.startsWith(TAG_PREFIX_CUE, true)) {
            TAG_PREFIX_CUE
        } else if (tag.startsWith(TAG_PREFIX_CUE_OUT, true)) {
            TAG_PREFIX_CUE_OUT
        } else if (tag.startsWith(TAG_PREFIX_CUE_OUT_CONT, true)) {
            TAG_PREFIX_CUE_OUT_CONT
        } else {
            null
        }
        if (prefix == null) {
            Timber.d("parseDuration() tag = ${tag} prefix is not valid for cue tones")
            return duration
        }
        duration = parseDuration(tag, prefix)
        return duration
    }

    private fun parseDuration(tag: String, prefix: String): Int? {
        var duration: Int? = null
        Timber.d("parseDuration() tag = ${tag} prefix = ${prefix}")
        try {
            if (tag.contains("Duration", true)) {
                // format EXT-X-CUE:DURATION="201.467"
                // format EXT-X-CUE-OUT:DURATION=10.50
                // format EXT-X-CUE-OUT-CONT:ElapsedTime=5.939,Duration=201.467
                val rawData = tag.removePrefix(prefix).split("=", ",").chunked(2)
                val totalDuration = rawData.findLast { it.first().equals("Duration", true) }
                    ?.last()
                    ?.replace("\"", "")
                    ?.toDoubleOrNull()
                    ?.roundToInt() ?: -1
                val elapsedTime = rawData.findLast { it.first().equals("ElapsedTime", true) }
                    ?.last()
                    ?.replace("\"", "")
                    ?.toDoubleOrNull()
                    ?.roundToInt() ?: 0

                Timber.d("parseDuration() tag = ${tag} totalDuration = ${totalDuration} elapsedTime = ${elapsedTime}")
                if (totalDuration != -1) {
                    duration = totalDuration - elapsedTime
                }
            } else {
                // format EXT-X-CUE-OUT-CONT: 8.308/30
                // format EXT-X-CUE-OUT-CONT: 30
                val rawData = tag.removePrefix(prefix).split("/")
                val totalDuration = if (rawData.size == 1) {
                    rawData.firstOrNull()
                        ?.replace("\"", "")
                        ?.toDoubleOrNull()
                        ?.roundToInt() ?: -1
                } else if (rawData.size == 2) {
                    rawData.lastOrNull()
                        ?.replace("\"", "")
                        ?.toDoubleOrNull()
                        ?.roundToInt() ?: -1
                } else {
                    -1
                }
                val elapsedTime = if (rawData.size == 2) {
                    rawData.firstOrNull()
                        ?.replace("\"", "")
                        ?.toDoubleOrNull()
                        ?.roundToInt() ?: 0
                } else {
                    0
                }

                Timber.d("parseDuration() tag = ${tag} totalDuration = ${totalDuration} elapsedTime = ${elapsedTime}")
                if (totalDuration != -1) {
                    duration = totalDuration - elapsedTime
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        Timber.d("parseDuration() tag = ${tag} duration = ${duration}")
        return duration
    }
}