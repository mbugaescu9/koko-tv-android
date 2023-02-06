package com.kokoconnect.android.model.event

import com.kokoconnect.android.util.AppParams

class AmsBody (
    val application: String? = AppParams.versionName,
    val browser: String? = "mobile-app",
    val device: String? = AppParams.amsPlatform,
    val language: String?,
    val country: String?,
    val city: String?,
    val region: String?,
    val ga: String? = null,
    val os: String?,
    val ip: String?,
    val timezone: Int,
    val email: String? = null,
    val ams_id: String?,
    val user_type: String? = null,
    val cms_id: Int? = null,
    val first_access: String?,
    val registration_date: String? = null,
    val events: List<AmsEvent>
)

class AmsDeviceInfo {
    var os: String? = null
    var language: String? = null
    var timezone: Int = 0
    var screen_resolution: String? = null
    var mobile_model: String? = null
    var ram: String? = null
    var manufacturer: String? = null
    var cpu_make: String? = null
    var cpu_model: String? = null
    var dpi: String? = null
}

class AmsResponse

class AmsUserInfo (
    val status: String?,
    val response: AmsUserInfoResponse?
)

class AmsUserInfoResponse (
    val user_type: String,
    val first_access: String,
    val ams_id: String,
    val push_token: String,
    val nf_status: String,
    val country: String,
    val city: String,
    val region: String,
    val ip: String,
    val server_time: String
)