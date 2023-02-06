package com.kokoconnect.android.model.ads.interstitial.timer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kokoconnect.android.model.ads.AdsStatus
import com.kokoconnect.android.model.ads.interstitial.InterstitialTrigger
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class InterstitialTimer(
    val adsStatus: LiveData<AdsStatus>,
    val onNeedShowInterstitial: MutableLiveData<InterstitialTrigger?>,
) {

    var executorTimer: ScheduledExecutorService? = null

    fun executorIsNotNull() = executorTimer != null

    fun isShutdown(): Boolean {
        return if (executorTimer == null) {
            true
        } else {
            (executorTimer?.isShutdown == true)
        }
    }

    fun start() {
        stop()
        Timber.d("InterstitialTimer: start()")
        Timber.d("InterstitialTimer: executor is null == ${executorTimer == null}")
        if (adsStatus.value?.interstitialOnTimerEnabled == true && adsStatus.value?.enabled == true) {
            val timeout = adsStatus.value?.interstitialOnTimerInterval ?: return
            Timber.d("InterstitialTimer: init executor ads and refresh timer == true timeout == $timeout")
            executorTimer = Executors.newSingleThreadScheduledExecutor()
            executorTimer?.schedule({
                Timber.d("InterstitialTimer: start executor timer body")
                onNeedShowInterstitial.postValue(InterstitialTrigger.OnTimer)
            }, timeout, TimeUnit.SECONDS)
        } else {
            Timber.d("InterstitialTimer: init executor ads and refresh timer == false")
        }
    }

    fun stop() {
        if (executorTimer != null) {
            Timber.d("InterstitialTimer: stop()")
            executorTimer?.shutdownNow()
        }
    }

}