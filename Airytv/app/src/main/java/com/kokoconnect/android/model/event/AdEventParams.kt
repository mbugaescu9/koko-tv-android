package com.kokoconnect.android.model.event

data class AdEventParams(
    val adKey: String,
    val adType: String,
    var adTitle: String? = null,
    val adClick: String? = null,
    val adTrigger: String? = null,
    val adDescription: String? = null
)
