package com.kokoconnect.android.util

import android.os.Bundle
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.model.tv.ProgramDescription
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import timber.log.Timber

object FirebaseLogger{
    private const val AD_CLICK_EVENT = "ad_click_event"
    private const val WATCH_EVENT = "watch_event"
    private const val CHANNEL_ID = "channel_id"
    private const val CHANNEL_NAME = "channel_name"
    private const val DURATION = "duration"
    private const val CATEGORY = "category"

    private const val GIVEAWAYS_CLICK_EVENT = "giveaways_click_event"
    private const val USER_ID = "user_id"
    private const val ITEM_ID = "item_id"

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(AiryTvApp.instance.applicationContext)


    fun logWatchEvent(programDescription: ProgramDescription, duration: Int?){
        Timber.d("logWatchEvent(): duration=${duration} data = ${Gson().toJson(programDescription)}")
        val bundle = Bundle()
        bundle.putInt(CHANNEL_ID, programDescription.channelNumber)
        bundle.putString(CHANNEL_NAME, programDescription.channelName.underscoreToSpaces())
        bundle.putString(CATEGORY, programDescription.category)
        duration?.let{
            bundle.putInt(DURATION, it)
        }
        firebaseAnalytics.logEvent(WATCH_EVENT, bundle)
    }

    fun logAdClickEvent(){
        Timber.d("logAdClickEvent()")
        firebaseAnalytics.logEvent(AD_CLICK_EVENT, null)
    }

    fun logImaAdEvent(adKey: String?, adPayload: String?, adEvent: String?, adDescription: String? = null) {
        Timber.d("logAdClickEvent()")
        val bundle = Bundle()
        bundle.putString("ad_key", adKey)
        bundle.putString("ad_payload", adPayload)
        bundle.putString("ad_event", adEvent)
        bundle.putString("ad_description", adDescription)

        firebaseAnalytics.logEvent("ima_ad_event", bundle)
        Timber.d("Firebase Ima Ad Event")
    }

    fun logGiveawaysClickEvent(userAmsId: String, itemId: Int) {
        Timber.d("logGiveawaysClickEvent() amsId = ${userAmsId} itemId = ${itemId}")
        val bundle = Bundle()
        bundle.putString(USER_ID, userAmsId)
        bundle.putInt(ITEM_ID, itemId)
        firebaseAnalytics.logEvent(GIVEAWAYS_CLICK_EVENT, bundle)
    }
}

fun String.spacesToUnderscore(): String = this.replace(" ", "_")

fun String.underscoreToSpaces(): String = this.replace("_", " ")