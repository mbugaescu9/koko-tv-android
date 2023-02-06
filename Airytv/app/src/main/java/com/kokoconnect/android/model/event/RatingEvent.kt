package com.kokoconnect.android.model.event

class RatingEvent(var data: RatingEventData) : AmsEvent("rating_event")

class RatingEventData (
    val content_name: String?,
    var payload: String?
) : EventData()