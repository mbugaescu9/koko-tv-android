package com.kokoconnect.android.model.auth

import com.google.gson.annotations.SerializedName

class ResetPasswordResponse{
    var message = ""
}

class TokenResponse{
    @SerializedName("registration_name")
    var registrationDate: String? = null
    var token: String? = null
    var user: User? = null
    var paid: Boolean? = null
}