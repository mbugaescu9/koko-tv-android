package com.kokoconnect.android.model.auth

data class SignInRequest(val email: String, val password: String)

data class SignUpRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val password: String,
    val repeatPassword: String
)

data class ResetPasswordRequest(val email: String)

data class CheckTokenRequest(val token: String?)
