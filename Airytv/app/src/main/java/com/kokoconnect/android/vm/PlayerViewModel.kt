package com.kokoconnect.android.vm

import android.content.res.Configuration
import androidx.lifecycle.*
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.model.AiryContentType
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.util.VolumeManager
import com.kokoconnect.android.vm.tv.TvPlayersViewModel
import com.kokoconnect.android.vm.vod.VodPlayersViewModel
import javax.inject.Inject

class PlayerViewModel @Inject constructor(
    val app: AiryTvApp
) : AndroidViewModel(app), LifecycleObserver {
    private val ams: AmsEventsFacade
        get() {
            return app.ams
        }
    val onChangeAudioState = MediatorLiveData<Boolean>()
    var screenConfiguration = MutableLiveData<Configuration>()
    var isFullscreen: Boolean = false
        private set
    var isFullscreenLiveData = MutableLiveData<Boolean>()
    private val volumeManager: VolumeManager = VolumeManager(app,
        object : VolumeManager.Listener {
            override fun onVolumeChanged(volume: Int, maxVolume: Int) {
                this@PlayerViewModel.onVolumeChanged(volume, maxVolume)
            }
        }
    )
    val isVolumeExists = volumeManager.isMuted.map {
        !it
    }
    var currentContentType: AiryContentType? = null
    private set
    var vodPlayersViewModel: VodPlayersViewModel? = null
    var tvPlayersViewModel: TvPlayersViewModel? = null

    fun init(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    fun setCurrentContentType(currentContentType: AiryContentType?) {
        this.currentContentType = currentContentType
        ams.updateContentType(currentContentType)
        val isTvContent = currentContentType == AiryContentType.TV
        val isVodContent = currentContentType == AiryContentType.VOD
        tvPlayersViewModel?.setCurrent(isTvContent)
        vodPlayersViewModel?.setCurrent(isVodContent)
    }


    private fun onVolumeChanged(volume: Int, maxVolume: Int) {}

    fun switchAudioMode() {
        volumeManager.switchVolumeMuted()
    }

    fun switchFullscreen() {
        isFullscreen = !isFullscreen
        isFullscreenLiveData.postValue(isFullscreen)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        app.contentResolver.registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true,
            volumeManager
        )
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        app.contentResolver.unregisterContentObserver(volumeManager)
    }
}