package com.kokoconnect.android.model.response

object ResponseStatus {
    const val SUCCESS = "success"
    const val ERROR = "errors"
}

class ApiErrorThrowable(
    val errorType: ApiError,
    val errorResponse: ErrorResponse? = null
) : Exception(errorType.name)

enum class ApiError(var code: Int = -1) {
    NONE, NETWORK_PROBLEM, SERVER_ERROR, SERVER_RESULT_NOT_200
}

data class Response<T>(
    val status: String,
    val response: SuccessfulResponse<T>?,
    val error: ErrorResponse?
)

data class SuccessfulResponse<T>(
    val items: Map<String, ItemHolder<T>>,
    val categories: List<T>
)

class ItemHolder<T>(val items: List<T>)

data class ErrorResponse(val description: String)

data class ResponseObject<T>(
    val status: String,
    val errors: List<ErrorResponse>?,
    val response: T?
)
