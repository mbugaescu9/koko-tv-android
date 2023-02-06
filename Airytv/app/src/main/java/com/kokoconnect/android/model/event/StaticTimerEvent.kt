package com.kokoconnect.android.model.event

class StaticTimerEvent(var data: StaticTimerEventData) : AmsEvent("static_timer_event")

class StaticTimerEventData (
    var content_name: String?,
    var is_active_player: Boolean?,
    var current_url: String?
) : EventData() {

    fun copy(): StaticTimerEventData {
        //copy data fields, but not creating time
        return StaticTimerEventData(content_name, is_active_player, current_url)
    }
}