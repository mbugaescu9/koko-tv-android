package com.kokoconnect.android.model.ads

import com.google.android.exoplayer2.ext.ima.CustomFields

data class Ads(
    val options: AdsOptions?,
    val priorities: Map<String, Ad>?,
    val giveaways: Map<String, Ad>?,
    val infomercial: Map<String, Infomercial>?,
    val ima: Map<String, Ima>?
) {
    fun isEnabled() = (options?.adsEnabled) ?: false
}

open class Ima(
    priority: Int?,
    payload: String?,
    var vast: String = ""
) : Ad(priority ?: 0, payload) {

    //vast xml

    constructor(name: String, ad: Ad) : this(ad.priority, ad.payload) {
        this.name = name
    }

    fun copy(): Ima {
        val ima = Ima(priority, payload, vast)
        ima.name = name
        return ima
    }

    fun getBridge(): CustomFields.ImaBridge {
        val ima = this
        return object : CustomFields.ImaBridge {
            override fun getVastXml(): String = ima.vast
            override fun getName(): String = ima.name.toString()
        }
    }

}

class Infomercial(
    priority: Int?,
    payload: String?
) : Ad(priority ?: 0, payload) {

    constructor(name: String, ad: Ad) : this(ad.priority, ad.payload) {
        this.name = name
    }

}


open class Ad(
    var priority: Int = 0,
    var payload: String?,
    var adPurposes: List<AdPurpose> = listOf(AdPurpose.MAIN, AdPurpose.GIVEAWAYS)
) : Comparable<Ad> {
    var name: String? = null
    var extra: String? = null

    override fun compareTo(other: Ad): Int {
        return (priority ?: 0) - (other.priority ?: 0)
    }

}

enum class AdPurpose {
    MAIN,
    GIVEAWAYS
}

data class AdsPoints(
    var points: Long?
)