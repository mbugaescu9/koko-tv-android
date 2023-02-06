package com.kokoconnect.android.vm

import androidx.lifecycle.*
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.model.notification.Notification
import com.kokoconnect.android.model.notification.Notifications
import com.kokoconnect.android.model.notification.RatingManager
import com.kokoconnect.android.model.notification.UserAlert
import com.kokoconnect.android.repo.AiryRepository
import javax.inject.Inject

class NotificationsViewModel @Inject constructor(
    private val app: AiryTvApp,
    private val airyService: AiryRepository
) : ViewModel() {
    val ratingManager = RatingManager(Preferences(app).Rating())
    private var notifications = mutableListOf<Notification>()
    var needShowNotification = MutableLiveData<Notification?>(null)
    var needShowRatingDialog = MutableLiveData<Boolean>(false)

    var isUserAlertAlreadyShow = false
    var userAlertEnabled = false
    var userAlertData: UserAlert? = null

    fun init(lifecycle: Lifecycle) {
        ratingManager.init(lifecycle)
    }

    fun addNotifications(newNotifications: List<Notification>) {
//        Timber.d(Gson().toJson(newNotifications))
        if (newNotifications.isNotEmpty()) {
            notifications.addAll(newNotifications)
            needShowNotification.postValue(notifications.first())
        }
    }

    fun getNotifications(): LiveData<Notifications> {
        return airyService.getNotifications(Preferences(AiryTvApp.instance).Auth().getToken())
    }

    fun onNotificationClosed(notification: Notification) {
        airyService.sendNotificationShown(
            Preferences(AiryTvApp.instance).Auth().getToken(),
            notification
        )
        notifications.remove(notification)

        if (notifications.isNotEmpty()) {
            needShowNotification.postValue(notifications.first())
        }
    }

    fun getUserAlert(): LiveData<UserAlert> = airyService.userAlert()

    fun checkNeedShowRatingDialog() {
        if (ratingManager.isNeedShowRatingDialog()) {
            if (ratingManager.isFirstDialog()) {
                if (ratingManager.checkMoreThanHourOfUse()) {
                    needShowRatingDialog.postValue(true)
                }
            } else {
                if (ratingManager.checkThreeWeekPassed()) {
                    if (ratingManager.checkMoreThanHourOfUse()) {
                        needShowRatingDialog.postValue(true)
                    }
                }
            }
        }
    }
}