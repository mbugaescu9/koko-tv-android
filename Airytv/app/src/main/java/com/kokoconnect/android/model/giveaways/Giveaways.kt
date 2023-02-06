package com.kokoconnect.android.model.giveaways

import com.google.gson.annotations.SerializedName

class Giveaways() {
    constructor(events: List<GiveawaysItem>, count: Int, limit: Int): this() {
        this.events = events
        this.count = count
        this.limit = limit
    }
    var events: List<GiveawaysItem> = emptyList()
    var count: Int = 0
    var limit: Int = 0
    var available: Int = 0
    var active: Boolean = false
}

class GiveawaysDate(
    var date: String?,
    var timezone_type: Int?,
    var timezone: String?
)

class GiveawaysItem() {
    var id: Int? = null
    @SerializedName("entry")
    var entries: Int? = null
    @SerializedName("user_entry")
    var userEntries: Int? = null
    @SerializedName("alias")
    var aliasWinner: String? = null
    var name: String? = null
    @SerializedName("picture")
    var imageUrl: String? = null
    var date: String? = null
    @SerializedName("code")
    var cardCode: String? = null

    var maxEntries: Int? = null
    var isActive: Boolean? = null
    var isWinnerVisible: Boolean? = null
    var isCardCodeVisible: Boolean? = null
}

data class GiveawaysEntry (
    var event: Int?,
    var entry: Int?
)

data class GiveawaysInfo (
    var count: Int = 0,
    var limit: Int = 0,
    var available: Int = 0
)

data class GiveawaysEntryRequest(
    val count: Int
)