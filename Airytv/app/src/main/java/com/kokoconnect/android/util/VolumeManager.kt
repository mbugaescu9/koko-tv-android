package com.kokoconnect.android.util

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData


class VolumeManager(
    val context: Context,
    private var listener: Listener?,
    private val streamType: Int = AudioManager.STREAM_MUSIC
) : ContentObserver(Handler(Looper.getMainLooper())) {
    private var audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val isMuted: MutableLiveData<Boolean> = MutableLiveData(isVolumeMuted())

    override fun deliverSelfNotifications(): Boolean {
        return false
    }

    override fun onChange(selfChange: Boolean) {
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val volume = audioManager.getStreamVolume(streamType)
        listener?.onVolumeChanged(volume, maxVolume)
        if (isVolumeMuted()) {
            isMuted.postValue(true)
        } else {
            isMuted.postValue(false)
        }
    }

    fun isVolumeMuted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.getStreamVolume(streamType) == 0 || audioManager.isStreamMute(streamType)
        } else {
            audioManager.getStreamVolume(streamType) == 0
        }
    }

    fun getVolume(): Double {
        return audioManager.getStreamVolume(streamType).toDouble() /
                audioManager.getStreamMaxVolume(streamType).toDouble()
    }

    fun getVolumeRaw(): Int {
        return audioManager.getStreamVolume(streamType)
    }

    fun switchVolumeMuted() {
        val isMuted = audioManager.getStreamVolume(streamType) == 0
        if (isMuted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(
                    streamType,
                    AudioManager.ADJUST_UNMUTE,
                    AudioManager.FLAG_SHOW_UI
                )
            } else {
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(streamType, false)
            }
            this.isMuted.postValue(false)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(
                    streamType,
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
            } else {
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(streamType, true)
            }
            this.isMuted.postValue(true)
        }
    }

    interface Listener {
        fun onVolumeChanged(volume: Int, maxVolume: Int)
    }
}