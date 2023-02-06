package com.kokoconnect.android.model.ads.infomercial

interface InfomercialEventsListener {
    fun onInfomercialEvent(event: InfomercialEvent, loader: InfomercialLoader)
}

enum class InfomercialEvent {
    LOADED,
    LOAD_FAILED,
    LOAD_STARTED
}

