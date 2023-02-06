package com.kokoconnect.android.repo

import androidx.lifecycle.MutableLiveData
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.api.AirySecurityService
import com.kokoconnect.android.model.auth.*
import com.kokoconnect.android.model.response.ApiError
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.model.response.ResponseObject
import com.kokoconnect.android.util.NetworkUtils
import com.kokoconnect.android.util.resumeWithSafe
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

enum class AuthError(var code: Int = -1, var message: String = "") {
    NONE, NETWORK_PROBLEM, SERVER_ERROR, SERVER_RESULT_NOT_200
}

class AuthRepository @Inject constructor(
    private val app: AiryTvApp,
    private val airySecurityService: AirySecurityService
) {
    private val gson = Gson()
    private val authPrefs = Preferences(app).Auth()

    fun signIn(
        signInRequest: SignInRequest,
        result: MutableLiveData<TokenResponse?>,
        errorLiveData: MutableLiveData<AuthError?>
    ) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("email", signInRequest.email)
                .addFormDataPart("password", signInRequest.password)
                .build()

            airySecurityService.signIn(requestBody)
                .enqueue(object : Callback<ResponseObject<TokenResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<TokenResponse>>,
                        response: Response<ResponseObject<TokenResponse>>
                    ) {
                        var errorBody: ResponseObject<*>? = null
                        try {
                            response.errorBody()?.let {
                                errorBody = gson.fromJson(it.string(), ResponseObject::class.java)
                            }
                        } catch (ex: JsonSyntaxException) {
                            ex.printStackTrace()
                        }
                        when {
                            response.isSuccessful && response.body() != null -> {
                                result.postValue(response.body()?.response)
                            }
                            response.isSuccessful -> {
                                errorLiveData.postValue(AuthError.SERVER_ERROR.apply {
                                    code = response.code()
                                    message = errorBody?.errors?.getOrNull(0)?.description ?: ""
                                })
                            }
                            else -> {
                                errorLiveData.postValue(AuthError.SERVER_RESULT_NOT_200.apply {
                                    code = response.code()
                                    message = errorBody?.errors?.getOrNull(0)?.description ?: ""
                                })
                            }
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<TokenResponse>>,
                        t: Throwable
                    ) {
                        try {
                            throw t
                        } catch (e: IOException) {
                            if (NetworkUtils.isInternetAvailable()) {
                                errorLiveData.postValue(AuthError.SERVER_ERROR)
                            } else {
                                errorLiveData.postValue(AuthError.NETWORK_PROBLEM)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorLiveData.postValue(AuthError.SERVER_ERROR)
                        }
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            errorLiveData.postValue(AuthError.SERVER_ERROR)
        }
    }


    fun signUp(
        signUpRequest: SignUpRequest,
        result: MutableLiveData<TokenResponse?>,
        errorLiveData: MutableLiveData<AuthError?>
    ) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("email", signUpRequest.email)
                .addFormDataPart("firstName", signUpRequest.firstName)
                .addFormDataPart("lastName", signUpRequest.lastName)
                .addFormDataPart("plainPassword[first]", signUpRequest.password)
                .addFormDataPart("plainPassword[second]", signUpRequest.repeatPassword)
                .build()

            airySecurityService.signUp(requestBody)
                .enqueue(object : Callback<ResponseObject<TokenResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<TokenResponse>>,
                        response: Response<ResponseObject<TokenResponse>>
                    ) {
                        var errorBody: ResponseObject<*>? = null
                        response.errorBody()?.let {
                            errorBody = gson.fromJson(it.string(), ResponseObject::class.java)
                        }
                        when {
                            response.isSuccessful && response.body() != null -> {
                                result.postValue(response.body()?.response)
                            }
                            response.isSuccessful -> {
                                errorLiveData.postValue(AuthError.SERVER_ERROR.apply {
                                    code = response.code()
                                    message = errorBody?.errors?.getOrNull(0)?.description ?: ""
                                })
                            }
                            else -> {
                                errorLiveData.postValue(AuthError.SERVER_RESULT_NOT_200.apply {
                                    code = response.code()
                                    message = errorBody?.errors?.getOrNull(0)?.description ?: ""
                                })
                            }
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<TokenResponse>>,
                        t: Throwable
                    ) {
                        try {
                            throw t
                        } catch (e: IOException) {
                            if (NetworkUtils.isInternetAvailable()) {
                                errorLiveData.postValue(AuthError.SERVER_ERROR)
                            } else {
                                errorLiveData.postValue(AuthError.NETWORK_PROBLEM)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorLiveData.postValue(AuthError.SERVER_ERROR)
                        }
                    }


                })
        } catch (e: Exception) {
            e.printStackTrace()
            errorLiveData.postValue(AuthError.SERVER_ERROR)
        }
    }

    fun social(
        socialType: String,
        token: String,
        result: MutableLiveData<TokenResponse?>,
        errorLiveData: MutableLiveData<AuthError?>
    ) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("type", socialType)
                .addFormDataPart("access_token", token)
                .build()

            airySecurityService.social(requestBody)
                .enqueue(object : Callback<ResponseObject<TokenResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<TokenResponse>>,
                        response: Response<ResponseObject<TokenResponse>>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            result.postValue(response.body()?.response)
                        } else {
                            val error = parseAuthError(response, socialType)
                            errorLiveData.postValue(error)
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<TokenResponse>>,
                        t: Throwable
                    ) {
                        try {
                            throw t
                        } catch (e: IOException) {
                            if (NetworkUtils.isInternetAvailable()) {
                                errorLiveData.postValue(AuthError.SERVER_ERROR)
                            } else {
                                errorLiveData.postValue(AuthError.NETWORK_PROBLEM)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorLiveData.postValue(AuthError.SERVER_ERROR)
                        }
                    }


                })
        } catch (e: Exception) {
            e.printStackTrace()
            errorLiveData.postValue(AuthError.SERVER_ERROR)
        }
    }

    fun resetPassword(
        resetPasswordRequest: ResetPasswordRequest,
        result: MutableLiveData<ResetPasswordResponse?>,
        errorLiveData: MutableLiveData<AuthError?>
    ) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("email", resetPasswordRequest.email)
                .build()

            airySecurityService.resetPassword(requestBody)
                .enqueue(object : Callback<ResponseObject<ResetPasswordResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<ResetPasswordResponse>>,
                        response: Response<ResponseObject<ResetPasswordResponse>>
                    ) {
                        var errorBody: ResponseObject<*>? = null
                        response.errorBody()?.let {
                            errorBody = gson.fromJson(it.string(), ResponseObject::class.java)
                        }
                        when {
                            response.isSuccessful && response.body() != null -> {
                                result.postValue(response.body()?.response)
                            }
                            response.isSuccessful -> {
                                errorLiveData.postValue(AuthError.SERVER_ERROR.apply {
                                    code = response.code()
                                    message = errorBody?.errors?.getOrNull(0)?.description ?: ""
                                })
                            }
                            else -> {
                                errorLiveData.postValue(AuthError.SERVER_RESULT_NOT_200.apply {
                                    code = response.code()
                                    message = errorBody?.errors?.getOrNull(0)?.description ?: ""
                                })
                            }
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<ResetPasswordResponse>>,
                        t: Throwable
                    ) {
                        try {
                            throw t
                        } catch (e: IOException) {
                            if (NetworkUtils.isInternetAvailable()) {
                                errorLiveData.postValue(AuthError.SERVER_ERROR)
                            } else {
                                errorLiveData.postValue(AuthError.NETWORK_PROBLEM)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorLiveData.postValue(AuthError.SERVER_ERROR)
                        }
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            errorLiveData.postValue(AuthError.SERVER_ERROR)
        }
    }

    fun checkToken(
        checkTokenRequest: CheckTokenRequest,
        result: MutableLiveData<TokenResponse?>,
        errorLiveData: MutableLiveData<AuthError?>
    ) {
        try {
            airySecurityService.checkToken("Bearer " + checkTokenRequest.token)
                .enqueue(object : Callback<ResponseObject<TokenResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<TokenResponse>>,
                        response: Response<ResponseObject<TokenResponse>>
                    ) {
                        val errorBody: ResponseObject<*>? = try {
                            gson.fromJson(
                                response.errorBody()?.string(),
                                ResponseObject::class.java
                            )
                        } catch (ex: JsonSyntaxException) {
                            null
                        }
                        when {
                            response.isSuccessful && response.body() != null -> {
                                result.postValue(response.body()?.response)
                            }
                            response.isSuccessful -> {
                                errorLiveData.postValue(AuthError.SERVER_ERROR.apply {
                                    code = response.code()
                                    message = errorBody?.errors?.getOrNull(0)?.description ?: ""
                                })
                            }
                            else -> {
                                errorLiveData.postValue(AuthError.SERVER_RESULT_NOT_200.apply {
                                    code = response.code()
                                    message = errorBody?.errors?.getOrNull(0)?.description ?: ""
                                })
                            }
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<TokenResponse>>,
                        t: Throwable
                    ) {
                        try {
                            throw t
                        } catch (e: IOException) {
                            if (NetworkUtils.isInternetAvailable()) {
                                errorLiveData.postValue(AuthError.SERVER_ERROR)
                            } else {
                                errorLiveData.postValue(AuthError.NETWORK_PROBLEM)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorLiveData.postValue(AuthError.SERVER_ERROR)
                        }
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            errorLiveData.postValue(AuthError.SERVER_ERROR)
        }
    }

    fun checkToken(token: String, ld: MutableLiveData<Boolean?>) {
        try {
            airySecurityService.checkToken("Bearer $token")
                .enqueue(object : Callback<ResponseObject<TokenResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<TokenResponse>>,
                        response: Response<ResponseObject<TokenResponse>>
                    ) {
                        val result = response.isSuccessful && response.body() != null
                        ld.postValue(result)
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<TokenResponse>>,
                        t: Throwable
                    ) {
                        t.printStackTrace()
                        ld.postValue(null)
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            ld.postValue(null)
        }
    }

    @Throws(ApiErrorThrowable::class)
    suspend fun checkToken(checkTokenRequest: CheckTokenRequest): TokenResponse =
        suspendCancellableCoroutine { continuation ->
            try {
                airySecurityService.checkToken("Bearer " + checkTokenRequest.token)
                    .enqueue(object : Callback<ResponseObject<TokenResponse>> {
                        override fun onResponse(
                            call: Call<ResponseObject<TokenResponse>>,
                            response: Response<ResponseObject<TokenResponse>>
                        ) {
                            val errorBody: ResponseObject<*>? = try {
                                gson.fromJson(
                                    response.errorBody()?.string(),
                                    ResponseObject::class.java
                                )
                            } catch (ex: JsonSyntaxException) {
                                null
                            }
                            val tokenResponse = response.body()?.response
                            when {
                                response.isSuccessful && tokenResponse != null -> {
                                    continuation.resumeWithSafe(Result.success(tokenResponse))
                                }
                                response.isSuccessful -> {
                                    continuation.resumeWithSafe(
                                        Result.failure(
                                            ApiErrorThrowable(
                                                errorType = ApiError.SERVER_ERROR,
                                                errorResponse = errorBody?.errors?.firstOrNull()
                                            )
                                        )
                                    )
                                }
                                else -> {
                                    continuation.resumeWithSafe(
                                        Result.failure(
                                            ApiErrorThrowable(
                                                errorType = ApiError.SERVER_RESULT_NOT_200,
                                                errorResponse = errorBody?.errors?.firstOrNull()
                                            )
                                        )
                                    )
                                }
                            }
                        }

                        override fun onFailure(
                            call: Call<ResponseObject<TokenResponse>>,
                            t: Throwable
                        ) {
                            continuation.resumeWithSafe(Result.failure(getApiErrorThrowable(t)))
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resumeWithSafe(Result.failure(ApiErrorThrowable(ApiError.SERVER_ERROR)))
            }
        }

    private fun parseAuthError(
        response: Response<ResponseObject<TokenResponse>>,
        reason: String
    ): AuthError {
        if (response.isSuccessful) {
            val error: AuthError = AuthError.SERVER_ERROR
            error.code = response.code()
            error.message = "Empty body"
            return error
        } else {
            val errorBodyRaw = response.errorBody()?.string()
//            val errorBodyRaw = "test errorBodyRaw"
            if (errorBodyRaw != null && errorBodyRaw.isNotEmpty()) {
                try {
                    var errorBody: ResponseObject<*>? = null
                    errorBody = gson.fromJson(errorBodyRaw, ResponseObject::class.java)
                    val error: AuthError = AuthError.SERVER_RESULT_NOT_200
                    error.code = response.code()
                    error.message =
                        errorBody?.errors?.getOrNull(0)?.description ?: "Empty description"
                    return error
                } catch (e: JsonSyntaxException) {
                    e.printStackTrace()
                    val error: AuthError = AuthError.SERVER_ERROR
                    error.code = response.code()
                    error.message = "Wrong json syntax: $errorBodyRaw"
                    val log = JsonSyntaxException("${error.message} for $reason")
                    FirebaseCrashlytics.getInstance().recordException(log)
                    return error
                } catch (e: Exception) {
                    e.printStackTrace()
                    val error: AuthError = AuthError.SERVER_ERROR
                    error.code = response.code()
                    error.message = "Unknown error: $errorBodyRaw"
                    val log = java.lang.Exception("${error.message} for $reason")
                    FirebaseCrashlytics.getInstance().recordException(log)
                    return error
                }
            } else {
                val error: AuthError = AuthError.SERVER_RESULT_NOT_200
                error.code = response.code()
                error.message = "Empty error body"
                return error
            }
        }
    }

    private fun getApiErrorThrowable(t: Throwable): ApiErrorThrowable {
        return try {
            throw t
        } catch (ex: IOException) {
            if (NetworkUtils.isInternetAvailable()) {
                ApiErrorThrowable(ApiError.SERVER_ERROR)
            } else {
                ApiErrorThrowable(ApiError.NETWORK_PROBLEM)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ApiErrorThrowable(ApiError.SERVER_ERROR)
        }
    }

    fun getToken(): String? {
        return authPrefs.getToken()
    }

    fun setToken(token: String?) {
        authPrefs.setToken(token)
    }

    fun getEmail(): String? {
        return authPrefs.getEmail()
    }

    fun setEmail(email: String?) {
        authPrefs.setEmail(email)
    }

}