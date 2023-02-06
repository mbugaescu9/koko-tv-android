package com.kokoconnect.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kokoconnect.android.R
import com.kokoconnect.android.ui.activity.MainActivity
import java.io.IOException
import java.net.URL

class NotificationsWorker(var context: Context, var workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        const val TITLE = "TITLE"
        const val TEXT = "TEXT"
        const val IMAGE_URL = "IMAGE_URL"
    }

    override suspend fun doWork(): Result {
        showNotifications()
        return Result.success()
    }

    private fun showNotifications() {
        val icon = BitmapFactory.decodeResource(applicationContext.resources, R.mipmap.ic_launcher)
        val title = workerParams.inputData.getString(TITLE);
        val text = workerParams.inputData.getString(TEXT);
        val image_url = workerParams.inputData.getString(IMAGE_URL);


        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, "channel_id")
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setContentInfo(title)
            .setLargeIcon(icon)
//            .setColor(Color.BLUE)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_splash_screen_logo)

        try {
            if (image_url != null && image_url.isNotEmpty()) {
                val url = URL(image_url)
                val bigPicture = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                notificationBuilder.setStyle(
                    NotificationCompat.BigPictureStyle().bigPicture(bigPicture).setSummaryText(text)
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }


        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification Channel is required for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channel_id", "channel_name", NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "channel description"
            channel.setShowBadge(true)
            channel.canShowBadge()
            channel.enableLights(true)
            channel.lightColor = Color.RED
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
