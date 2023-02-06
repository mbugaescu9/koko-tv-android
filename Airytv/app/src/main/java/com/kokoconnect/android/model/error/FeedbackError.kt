package com.kokoconnect.android.model.error

import java.lang.Exception

data class FeedbackError (
    val errorType: FeedbackErrorType,
    var exception: Exception? = null
)

enum class FeedbackErrorType() {
    EMPTY_MESSAGE,
    SPAMMING_ERROR,
    API_ERROR,
    NOT_AUTHORIZED
}