package com.kokoconnect.android.model.event

class LandingEvent(var data: LandingEventData) : AmsEvent("landing_event") {
    companion object {
        const val URL_START = "start" //when first screen starts
        const val URL_LOADED = "main" //when first screen loaded
        const val URL_EMPTY = "empty" //when first screen loaded, but channels list is empty
        const val URL_INTERRUPT = "interrupt" //when first screen not loaded and user exits app
    }
}

class LandingEventData (
    var content_name: String? = null,
    var landing_url: String?,
    var loading_time: Long?
) : EventData()