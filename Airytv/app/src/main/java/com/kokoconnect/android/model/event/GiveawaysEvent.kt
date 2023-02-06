package com.kokoconnect.android.model.event

class GiveawaysEvent(var data: GiveawaysEventData) : AmsEvent("giveaways_event")

class GiveawaysEventData (
    var action_name: String?,
    var payload: String?,
    val content_name: String?
) : EventData()