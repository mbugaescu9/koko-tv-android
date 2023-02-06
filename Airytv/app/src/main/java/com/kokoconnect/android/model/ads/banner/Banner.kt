package com.kokoconnect.android.model.ads.banner

data class Banner(val priorityValue: Int, val tag: String, val payload: String?) {
    fun getPriority(): Int = priorityValue ?: -1
}