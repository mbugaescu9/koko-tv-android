package com.kokoconnect.android.model.error

import java.lang.Exception

class GiveawaysError(
    val type: GiveawaysErrorType,
    var exception: Exception? = null
) {}

enum class GiveawaysErrorType() {
    ERROR_REQUEST_ACTIVE_LIST,
    ERROR_REQUEST_COMPLETED_LIST,
    ERROR_NOT_AUTHORIZED,
    ERROR_ENTRY_LIMIT_REACHED,
    ERROR_GET_TICKET,
    ERROR_AD_NOT_AVAILABLE,
    NONE
}