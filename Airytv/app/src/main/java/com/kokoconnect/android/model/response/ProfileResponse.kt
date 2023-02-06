package com.kokoconnect.android.model.response

import com.kokoconnect.android.model.giveaways.GiveawaysItem

class ProfileResponse {
    var username: String? = null
    var avatar: String? = null
    var gifts: List<GiveawaysItem>? = null
}