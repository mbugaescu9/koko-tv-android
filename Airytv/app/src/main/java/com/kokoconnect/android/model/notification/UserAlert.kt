package com.kokoconnect.android.model.notification

import com.google.gson.annotations.SerializedName

data class UserAlert (
    @SerializedName("image")
    val imageUrl: String?,
    @SerializedName("text")
    var textOnScreen: String?,
    @SerializedName("button")
    var textOnButton: String?,
    @SerializedName("url")
    var linkUrl: String?,
    @SerializedName("show_alert")
    val isShow: Boolean?,
    @SerializedName("show_alert_on_start")
    val showAlertOnStart: Boolean?,
    @SerializedName("allow_continue")
    val allowContinue: Boolean?,
    @SerializedName("show_x")
    val showX: Boolean?
)