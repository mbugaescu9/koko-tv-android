package com.kokoconnect.android.model.ui


class Screen() {
    var name = ""
    var type = ScreenType.NONE

    constructor(newName: String) : this() {
        this.name = newName
    }

    fun isParentOf(other: Screen?): Boolean {
        return other != null && type.hierarchy < other.type.hierarchy
    }

    fun isChildOf(other: Screen?): Boolean {
        return other != null && type.hierarchy > other.type.hierarchy
    }
}

enum class ScreenType(
    val defaultName: String,
    val hierarchy: Int
) {
    NONE("NONE", -1),

    TV("tv", 1),

    VOD("vod", 1),
    COLLECTION("collection", 2),
    SERIES("series",3),
    CONTENT("content", 4),

    FREE_GIFT("free_gift", 1),

    PROFILE("profile",1),
    AUTH("auth", 2),
    TRANSACTIONS("transactions",2),
    PRIVACY_POLICY("privacy_policy", 2),
    SUGGESTION("suggestion", 2)
}