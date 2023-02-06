package com.kokoconnect.android.service

import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import androidx.work.*

class MessageReceiver : FirebaseMessagingService() {

//    override fun onNewToken(p0: String) {
//        Timber.d("FirebaseMessageReceiver : onNewToken() token = ${p0}")
//        super.onNewToken(p0)
//    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        launchNotificationsWorker(remoteMessage)
    }

    private fun launchNotificationsWorker(remoteMessage: RemoteMessage?) {
        val notification = remoteMessage?.notification
//        Log.d("MessageReceiver","launchNotificationsWorker() title = ${notification?.title}" +
//                "text = ${notification?.body}" +
//                "imageUrl = ${remoteMessage?.data?.get("picture_url")}")
        val workerData = Data.Builder()
            .putString(NotificationsWorker.TITLE, notification?.title)
            .putString(NotificationsWorker.TEXT, notification?.body)
            .putString(NotificationsWorker.IMAGE_URL, remoteMessage?.data?.get("picture_url"))
            .build()
        val work = OneTimeWorkRequest.Builder(NotificationsWorker::class.java)
            .setInputData(workerData)
            .build()
        WorkManager.getInstance(this).beginWith(work).enqueue()
    }

}