package com.kokoconnect.android.model.notification

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.kokoconnect.android.repo.Preferences
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Seconds
import timber.log.Timber
import java.util.*

class RatingManager(
    val ratingPrefs: Preferences.Rating
) : LifecycleObserver {
    var startDate: Date = Date()

    fun init(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    fun isFirstDialog(): Boolean = ratingPrefs.isFirstDialog()

    fun isNeedShowRatingDialog(): Boolean {
        return !ratingPrefs.getRatingDialogSendRating()
    }

    fun checkThreeWeekPassed(): Boolean {
        val last = ratingPrefs.getDateLastCheck()
        if (last == null) {
            setCurrentDateForLastCheck()
            return false
        }
        val days = Days.daysBetween(DateTime(last), DateTime(Date())).days
        return days >= 7 * 3
    }

    fun checkMoreThanHourOfUse(): Boolean {
        updateSecondsOfUse()
        return ratingPrefs.getSecondsOfUse() > 60 * 60
    }

    fun getSeconds() = Seconds.secondsBetween(DateTime(startDate), DateTime(Date())).seconds

    fun updateSecondsOfUse() {
        var secondsOfUse = ratingPrefs.getSecondsOfUse()
        Timber.d("updateSecondsOfUse() seconds of use before $secondsOfUse seconds of use current ${getSeconds()}")
        secondsOfUse += getSeconds()
        ratingPrefs.setSecondsOfUse(secondsOfUse)
    }

    fun clearSecondsOfUse() {
        ratingPrefs.clearSecondsOfUse()
    }

    fun setCurrentDateForLastCheck() {
        ratingPrefs.setDateLastCheck(Date())
    }

    fun setFirstDialog(isFirstDialog: Boolean) {
        ratingPrefs.setFirstDialog(isFirstDialog)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        startDate = Date()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        updateSecondsOfUse()
    }
}