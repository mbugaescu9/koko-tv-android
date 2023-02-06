package com.kokoconnect.android.api

import com.kokoconnect.android.model.event.AmsBody
import com.kokoconnect.android.model.event.AmsResponse
import com.kokoconnect.android.model.event.AmsUserInfo
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AmsEventsService {
    @POST("app_ams.php/ams/event")
    fun sendEvents(
        @Body body: AmsBody
    ): Call<AmsResponse>

    @GET("user-info")
    fun getUserInfo(
        @Query("email") email: String?,
        @Query("ams_id") ams_id: String?
    ): Call<AmsUserInfo>
}