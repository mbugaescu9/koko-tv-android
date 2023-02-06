package com.kokoconnect.android.model.event

class AdvertisementEvent(var data: AdvertisementEventData) : AmsEvent("advertisement_event")

class AdvertisementEventData (
    var ad_title: String?,
    var ad_type: String?,
    var ad_banner_type: String?,
    var ad_index: String? = null,
    var ad_trigger: String? = null,
    var click: String? = null,
    var ad_id: String? = null,
    var ad_url: String? = null,
    var ad_country: String? = null,
    var ad_region: String? = null,
    var ad_description: String? = null,

    var content_id: Long? = null,
    var content_name: String? = null,
    var content_type: String? = null,

    var channel_num: Int? = null,
    var channel_name: String? = null
) : EventData()