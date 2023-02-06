package com.kokoconnect.android.model.profile

import com.kokoconnect.android.model.giveaways.GiveawaysItem
import com.kokoconnect.android.model.response.ProfileResponse

class Profile() {
    var username: String? = null
    var avatar: String? = null
    var gifts: List<GiveawaysItem>? = null

    constructor(response: ProfileResponse): this() {
        update(response)
    }

    fun update(response: ProfileResponse) {
        this.username = response.username
        this.avatar = response.avatar
        this.gifts = response.gifts
    }

}