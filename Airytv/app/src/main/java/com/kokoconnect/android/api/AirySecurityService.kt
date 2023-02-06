package com.kokoconnect.android.api

import com.kokoconnect.android.model.auth.ResetPasswordResponse
import com.kokoconnect.android.model.auth.TokenResponse
import com.kokoconnect.android.model.response.ResponseObject
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface AirySecurityService {
    @POST("/security/reset-password/send-email")
    fun resetPassword(@Body resetPasswordForm: RequestBody): Call<ResponseObject<ResetPasswordResponse>>

    @POST("/security/register")
    fun signUp(@Body resetPasswordForm: RequestBody): Call<ResponseObject<TokenResponse>>

    @POST("/security/token/create")
    fun signIn(@Body signInForm: RequestBody): Call<ResponseObject<TokenResponse>>

    @POST("/security/token/check")
    fun checkToken(@Header("Authorization") token: String): Call<ResponseObject<TokenResponse>>

    @POST("/security/social")
    fun social(@Body socialForm: RequestBody): Call<ResponseObject<TokenResponse>>
}