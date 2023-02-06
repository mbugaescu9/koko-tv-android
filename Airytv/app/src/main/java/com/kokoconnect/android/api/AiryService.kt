package com.kokoconnect.android.api

import com.kokoconnect.android.model.vod.CollectionResponse
import com.kokoconnect.android.model.vod.CollectionsResponse
import com.kokoconnect.android.model.vod.SeriesResponse
import com.kokoconnect.android.model.ads.Ads
import com.kokoconnect.android.model.giveaways.Giveaways
import com.kokoconnect.android.model.giveaways.GiveawaysEntryRequest
import com.kokoconnect.android.model.notification.NotificationMessage
import com.kokoconnect.android.model.notification.Notifications
import com.kokoconnect.android.model.notification.UserAlert
import com.kokoconnect.android.model.player.ArchiveApiKey
import com.kokoconnect.android.model.request.FeedbackBody
import com.kokoconnect.android.model.response.*
import com.kokoconnect.android.model.tv.ChannelsResponse
import com.kokoconnect.android.model.vod.SearchContentResponse
import com.kokoconnect.android.util.AppParams
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*
import retrofit2.http.Part

interface AiryService {

//    @GET("api/${AppParams.apiVersion}/channels?isIos=false")
    @GET("/api/${AppParams.apiVersion}/channels?device=fire_tv")
    fun getChannels(
/*        *//*@Query("device")*//* device: String = AppParams.platform,
        *//*@Query("version")*//* version: String = AppParams.versionName,
        *//*@Query("timezone")*//* timezone: String = DateUtils.getTimezoneString(),
        *//*@Query("type")*//* type: String = "mobile"*/
    ): Call<ResponseObject<ChannelsResponse>>

    @GET("/api/${AppParams.apiVersion}/content")
    fun getAllCollections(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Call<CollectionsResponse>

    @GET("/api/${AppParams.apiVersion}/collection/{id}")
    fun getCollection(
        @Path("id") id: Long,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Call<CollectionResponse>

    @GET("/api/${AppParams.apiVersion}/collection/{id}")
    suspend fun getCollectionSuspend(
        @Path("id") id: Long,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): CollectionResponse

    @GET("/api/${AppParams.apiVersion}/search")
    fun searchContent(
        @Query("keyword") keyword: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Call<SearchContentResponse>

    @GET("/api/${AppParams.apiVersion}/series/{id}")
    fun getSeries(
        @Path("id") id: Long,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Call<SeriesResponse>

    @GET("/api/${AppParams.apiVersion}/genres?isIos=false")
    fun getGenres(
        @Query("type") type: String = "mobile"
    ): Call<Map<String, Any>>
// http://api.youvu.tv/api/v2.1.7/ads/fire_tv
//    @GET("/api/${AppParams.apiVersion}/ads/{type}")
    @GET("/api/${AppParams.apiVersion}/ads/fire_tv")
    fun ads(/*@Path("type")*/ /*type: String = AppParams.adPlatform*/): Call<ResponseObject<Ads>>

    @GET("/api/${AppParams.apiVersion}/alert/{type}?build=${AppParams.versionCode}")
    fun userAlert(@Path("type") type: String = AppParams.platform): Call<ResponseObject<UserAlert>>

    @GET("/api/${AppParams.apiVersion}/notifications")
    fun getNotifications(@Header("Authorization") token: String): Call<ResponseObject<Notifications>>

    @POST("/api/${AppParams.apiVersion}/notification/show/{id}")
    fun sendNotificationShown(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<ResponseObject<NotificationMessage>>

    @GET("/api/${AppParams.apiVersion}/events/active")
    fun giveawaysActive(@Header("Authorization") token: String?): Call<ResponseObject<Giveaways>>

    @GET("/api/${AppParams.apiVersion}/events/completed")
    fun giveawaysCompleted(): Call<ResponseObject<Giveaways>>

    @POST("/api/${AppParams.apiVersion}/events/entry")
    fun getGiveawaysTicket(@Header("Authorization") token: String): Call<ResponseObject<GiveawaysEntryResponse>>

    @POST("/api/${AppParams.apiVersion}/events/{id}/entry")
    fun postGiveawaysEntry(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body entryRequest: GiveawaysEntryRequest
    ): Call<ResponseObject<GiveawaysEntryResponse>>

    @GET("/api/${AppParams.apiVersion}/transactions")
    fun getTransactions(
        @Header("Authorization") token: String
    ): Call<ResponseObject<TransactionsResponse>>

    @GET("/api/${AppParams.apiVersion}/profile")
    fun getProfile(
        @Header("Authorization") token: String
    ): Call<ResponseObject<ProfileResponse>>

    @Multipart
    @POST("/api/${AppParams.apiVersion}/avatar")
    fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part images: MultipartBody.Part
    ): Call<ResponseObject<ProfileResponse>>

    @POST("/api/${AppParams.apiVersion}/suggestion")
    fun sendSuggestion(
        @Header("Authorization") token: String,
        @Body body: FeedbackBody
    ): Call<ResponseObject<FeedbackResponse>>

    @POST("/api/${AppParams.apiVersion}/feedback")
    fun sendFeedback(
        @Header("Authorization") token: String,
        @Body body: FeedbackBody
    ): Call<ResponseObject<FeedbackResponse>>

    @GET("/api/${AppParams.apiVersion}/key")
    fun getArchiveApiKey(): Call<ResponseObject<ArchiveApiKey>>

    @GET
    fun downloadVastXml(@Url vastUrl: String): Call<String>
}
