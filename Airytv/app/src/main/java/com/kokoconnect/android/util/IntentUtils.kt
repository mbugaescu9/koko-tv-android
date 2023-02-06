package com.kokoconnect.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

object IntentUtils {


    fun hasAppForIntent(context: Context, intent: Intent): Boolean {
        return try {
            intent.resolveActivity(context.packageManager) != null
        } catch (ex: Exception) {
            false
        }
    }

    fun getGalleryIntent(): Intent {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        return galleryIntent
    }

    fun getCameraIntent(outputUri: Uri?): Intent {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        return cameraIntent
    }

    fun getShareLinkIntent(link: String, title: String = "Share link"): Intent {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_SUBJECT, title)
        i.putExtra(Intent.EXTRA_TEXT, link)
        return i
    }

    fun getCastSettingsIntent(): Intent {
        return Intent("android.settings.CAST_SETTINGS")
    }

    fun getWifiDisplaySettingsIntent(): Intent {
        return Intent("android.settings.WIFI_DISPLAY_SETTINGS")
    }

    fun isActivityExists(context: Context?, intent: Intent): Boolean {
        return context?.packageManager?.queryIntentActivities(intent, 0)?.isNotEmpty() ?: false
    }
}