package com.kokoconnect.android.util

import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class UpdateTimer(val onTimerTriggered: MutableLiveData<Boolean>) {
    companion object {
        val DEFAULT_UPDATE_TIME_SECONDS = TimeUnit.HOURS.toSeconds(2L)
    }

    var executorTimer: ScheduledExecutorService? = null

    fun executorIsNotNull() = executorTimer != null

    fun executorIsNull() = executorTimer == null

    fun start(
        forSeconds: Long = DEFAULT_UPDATE_TIME_SECONDS,
        repeat: Boolean = true
    ) {
        stop()
        executorTimer = Executors.newSingleThreadScheduledExecutor()
        val timeout = forSeconds
        executorTimer?.schedule({
            onTimerTriggered.postValue(true)
            if (repeat) {
                start(forSeconds)
            }
        }, timeout, TimeUnit.SECONDS)
    }

    fun stop() {
        executorTimer?.shutdownNow()
        executorTimer = null
    }
}