package com.kokoconnect.android.model.deeplink

abstract class DeeplinkData

class ChannelDeeplinkData(
    var channelName: String,
    var channelNumber: Int,
    var category: String
): DeeplinkData()

class ContentDeeplinkData(
    var contentId: Long,
    var contentName: String,
    var contentType: String
): DeeplinkData()
