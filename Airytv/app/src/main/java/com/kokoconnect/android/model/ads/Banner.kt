package com.kokoconnect.android.model.ads

data class Banner(val priorityValue: Int, val tag: String, val payload: String?) {
    fun getPriority(): Int = priorityValue ?: -1
}