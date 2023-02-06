package com.kokoconnect.android.repo

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.jetbrains.anko.doAsync
import retrofit2.Call
import retrofit2.Callback
import java.io.IOException
import javax.inject.Inject
import com.kokoconnect.android.api.*
import com.kokoconnect.android.model.vod.CollectionResponse
import com.kokoconnect.android.model.vod.CollectionsResponse
import com.kokoconnect.android.model.vod.SeriesResponse
import com.kokoconnect.android.model.ads.AdsStatus
import com.kokoconnect.android.model.ads.video.VastDownloadError
import com.kokoconnect.android.model.giveaways.Giveaways
import com.kokoconnect.android.model.giveaways.GiveawaysEntryRequest
import com.kokoconnect.android.model.notification.Notification
import com.kokoconnect.android.model.notification.NotificationMessage
import com.kokoconnect.android.model.notification.Notifications
import com.kokoconnect.android.model.notification.UserAlert
import com.kokoconnect.android.model.player.ArchiveApiKey
import com.kokoconnect.android.model.request.FeedbackBody
import com.kokoconnect.android.model.response.*
import com.kokoconnect.android.model.tv.ChannelsResponse
import com.kokoconnect.android.model.vod.SearchContentResponse
import com.kokoconnect.android.util.NetworkUtils
import com.kokoconnect.android.util.resumeWithSafe
import com.google.gson.Gson
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.joda.time.Instant
import retrofit2.Response
import timber.log.Timber
import java.io.File
import kotlin.Exception
import kotlin.math.absoluteValue

class AiryRepository @Inject constructor(private val airyService: AiryService) {
    companion object {
        const val REPEAT_GUIDE_REQUEST_PERIOD = 10000L //10 sec
    }

    private val gson = Gson()
    private val requestHandler = Handler(Looper.getMainLooper())
    var guideRequestInstant: Instant? = null

    fun getGuide(
        result: MutableLiveData<ChannelsResponse?>,
        error: MutableLiveData<ApiError>
    ) {
        val lastRequestTime = guideRequestInstant
        if (lastRequestTime != null) {
            val lastRequestDuration = (Instant.now().millis - lastRequestTime.millis).absoluteValue
            if (lastRequestDuration < REPEAT_GUIDE_REQUEST_PERIOD) {
                requestHandler.removeCallbacksAndMessages(null)
                requestHandler.postDelayed({
                    getGuideNow(result, error)
                }, REPEAT_GUIDE_REQUEST_PERIOD - lastRequestDuration)
            } else {
                getGuideNow(result, error)
            }
        } else {
            getGuideNow(result, error)
        }
    }

    private fun getGuideNow(
        result: MutableLiveData<ChannelsResponse?>,
        errorLiveData: MutableLiveData<ApiError>
    ) {
        doAsync {
            try {
                guideRequestInstant = Instant.now()
                airyService.getChannels()
                    .enqueue(object : Callback<ResponseObject<ChannelsResponse>> {
                        override fun onResponse(
                            call: Call<ResponseObject<ChannelsResponse>>,
                            response: retrofit2.Response<ResponseObject<ChannelsResponse>>
                        ) {
                            if (response.isSuccessful) {
                                result.value = response.body()?.response
                            } else {
                                errorLiveData.postValue(ApiError.SERVER_RESULT_NOT_200.apply {
                                    code = response.code()
                                })
                            }
                        }

                        override fun onFailure(
                            call: Call<ResponseObject<ChannelsResponse>>,
                            t: Throwable
                        ) {
                            checkApiFailureThrowable(t, errorLiveData)
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
                errorLiveData.postValue(ApiError.SERVER_ERROR)
            }
        }
    }

    fun getAllCollections(
        result: MutableLiveData<CollectionsResponse>,
        error: MutableLiveData<ApiError>? = null,
        page: Int
    ) {
        airyService.getAllCollections(page = page).enqueue(object : Callback<CollectionsResponse> {
            override fun onResponse(
                call: Call<CollectionsResponse>,
                response: retrofit2.Response<CollectionsResponse>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        result.postValue(responseBody)
                    } else {
                        error?.postValue(ApiError.SERVER_ERROR)
                    }
                } else {
                    error?.postValue(ApiError.SERVER_RESULT_NOT_200)
                }

            }

            override fun onFailure(call: Call<CollectionsResponse>, t: Throwable) {
                error?.let {
                    checkApiFailureThrowable(t, it)
                }
            }
        })
    }

    suspend fun getAllCollections(
        page: Int
    ): CollectionsResponse = suspendCancellableCoroutine { continuaton ->
        airyService.getAllCollections(page = page).enqueue(object : Callback<CollectionsResponse> {
            override fun onResponse(
                call: Call<CollectionsResponse>,
                response: retrofit2.Response<CollectionsResponse>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        continuaton.resumeWithSafe(Result.success(responseBody))
                    } else {
                        continuaton.resumeWithSafe(Result.failure(ApiErrorThrowable(ApiError.SERVER_ERROR)))
                    }
                } else {
                    continuaton.resumeWithSafe(Result.failure(ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200)))
                }
            }

            override fun onFailure(call: Call<CollectionsResponse>, t: Throwable) {
                continuaton.resumeWithSafe(Result.failure(getApiErrorThrowable(t)))
            }
        })
    }

    fun getCollectionContent(
        result: MutableLiveData<CollectionResponse>,
        error: MutableLiveData<ApiError>? = null,
        id: Long,
        page: Int
    ) {
        airyService.getCollection(id = id, page = page)
            .enqueue(object : Callback<CollectionResponse> {
                override fun onResponse(
                    call: Call<CollectionResponse>,
                    response: retrofit2.Response<CollectionResponse>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            result.postValue(responseBody)
                        } else {
                            error?.postValue(ApiError.SERVER_ERROR)
                        }
                    } else {
                        error?.postValue(ApiError.SERVER_RESULT_NOT_200)
                    }
                }

                override fun onFailure(call: Call<CollectionResponse>, t: Throwable) {
                    error?.let {
                        checkApiFailureThrowable(t, it)
                    }
                }
            })
    }

    suspend fun getCollectionContentForHorizontalAdapter(
        id: Long,
        page: Int
    ) = airyService.getCollectionSuspend(id, page, 6)

    suspend fun getCollectionContent(
        id: Long,
        page: Int
    ): CollectionResponse = suspendCancellableCoroutine { continuation ->
        airyService.getCollection(id = id, page = page)
            .enqueue(object : Callback<CollectionResponse> {
                override fun onResponse(
                    call: Call<CollectionResponse>,
                    response: retrofit2.Response<CollectionResponse>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            continuation.resumeWithSafe(Result.success(responseBody))
                        } else {
                            continuation.resumeWithSafe(Result.failure(ApiErrorThrowable(ApiError.SERVER_ERROR)))
                        }
                    } else {
                        continuation.resumeWithSafe(Result.failure(ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200)))
                    }
                }

                override fun onFailure(call: Call<CollectionResponse>, t: Throwable) {
                    continuation.resumeWith(Result.failure(getApiErrorThrowable(t)))
                }
            })
    }

    suspend fun searchContent(
        query: String,
        page: Int
    ): SearchContentResponse = suspendCancellableCoroutine { continuation ->
        airyService.searchContent(query, page = page)
            .enqueue(object : Callback<SearchContentResponse> {
                override fun onResponse(
                    call: Call<SearchContentResponse>,
                    response: retrofit2.Response<SearchContentResponse>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            continuation.resumeWithSafe(Result.success(responseBody))
                        } else {
                            continuation.resumeWithSafe(Result.failure(ApiErrorThrowable(ApiError.SERVER_ERROR)))
                        }
                    } else {
                        continuation.resumeWithSafe(Result.failure(ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200)))
                    }
                }

                override fun onFailure(call: Call<SearchContentResponse>, t: Throwable) {
                    continuation.resumeWith(Result.failure(getApiErrorThrowable(t)))
                }
            })
    }


    suspend fun getSeries(id: Long, page: Int, limit: Int = 9): SeriesResponse =
        suspendCancellableCoroutine { continuation ->
            airyService.getSeries(id = id, page = page, limit = limit)
                .enqueue(object : Callback<SeriesResponse> {
                    override fun onResponse(
                        call: Call<SeriesResponse>,
                        response: retrofit2.Response<SeriesResponse>
                    ) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            if (responseBody != null) {
                                continuation.resumeWithSafe(Result.success(responseBody))
                            } else {
                                continuation.resumeWithSafe(
                                    Result.failure(
                                        ApiErrorThrowable(
                                            ApiError.SERVER_ERROR
                                        )
                                    )
                                )
                            }
                        } else {
                            continuation.resumeWithSafe(Result.failure(ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200)))
                        }
                    }

                    override fun onFailure(call: Call<SeriesResponse>, t: Throwable) {
                        val apiErrorThrowable = getApiErrorThrowable(t)
                        continuation.resumeWithSafe(Result.failure(apiErrorThrowable))
                    }
                })
        }

    fun getAdsStatus(): LiveData<AdsStatus> {
        val ld = MutableLiveData<AdsStatus>()
        getAdsStatus(ld)
        return ld
    }

    fun getAdsStatus(liveData: MutableLiveData<AdsStatus>) {
        doAsync {
            try {
                val adsRequest = airyService.ads().execute()
                if (adsRequest.isSuccessful) {
                    val adsResponse = adsRequest.body()?.response
                    val adsEnabled =
//                        if (AppParams.isDebug) false else
                        adsResponse?.isEnabled() ?: false
                    liveData.postValue(AdsStatus(adsEnabled, adsResponse))
                } else {
                    liveData.postValue(AdsStatus(false))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                liveData.postValue(AdsStatus(false))
            }
        }
    }

    fun userAlert(): LiveData<UserAlert> {
        val ld = MutableLiveData<UserAlert>()
        doAsync {
            try {
                val adsRequest = airyService.userAlert().execute()
                if (adsRequest.isSuccessful) {
                    val result = adsRequest?.body()?.response ?: return@doAsync
                    ld.postValue(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return ld
    }

    fun getNotifications(token: String?): LiveData<Notifications> {
        val ld = MutableLiveData<Notifications>()
        token ?: return ld
        airyService.getNotifications("Bearer $token")
            .enqueue(object : Callback<ResponseObject<Notifications>> {
                override fun onResponse(
                    call: Call<ResponseObject<Notifications>>,
                    response: retrofit2.Response<ResponseObject<Notifications>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.response?.let {
                            ld.postValue(it)
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseObject<Notifications>>, t: Throwable) {
                    System.err.println()
                }
            })
        return ld
    }

    fun sendNotificationShown(
        token: String?,
        notification: Notification
    ): LiveData<NotificationMessage> {
        val ld = MutableLiveData<NotificationMessage>()
        token ?: return ld
        airyService.sendNotificationShown("Bearer $token", notification.id)
            .enqueue(object : Callback<ResponseObject<NotificationMessage>> {
                override fun onResponse(
                    call: Call<ResponseObject<NotificationMessage>>,
                    response: retrofit2.Response<ResponseObject<NotificationMessage>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.response?.let {
                            ld.postValue(it)
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ResponseObject<NotificationMessage>>,
                    t: Throwable
                ) {
                    System.err.println()
                }
            })
        return ld
    }

    suspend fun getGiveawaysActive(
        token: String?
    ): Giveaways = suspendCancellableCoroutine { continuation ->
        airyService.giveawaysActive(if (token == null) null else "Bearer $token")
            .enqueue(object : Callback<ResponseObject<Giveaways>> {
                override fun onResponse(
                    call: Call<ResponseObject<Giveaways>>,
                    response: retrofit2.Response<ResponseObject<Giveaways>>
                ) {
                    try {
                        val giveaways = getGiveawaysResponseData(response)
                        continuation.resumeWithSafe(Result.success(giveaways))
                    } catch (ex: ApiErrorThrowable) {
                        continuation.resumeWithSafe(Result.failure(ex))
                    }
                }

                override fun onFailure(call: Call<ResponseObject<Giveaways>>, t: Throwable) {
                    continuation.resumeWithSafe(Result.failure(getApiErrorThrowable(t)))
                }
            })
    }

    suspend fun getGiveawaysCompleted(): Giveaways = suspendCancellableCoroutine { continuation ->
        airyService.giveawaysCompleted()
            .enqueue(object : Callback<ResponseObject<Giveaways>> {
                override fun onResponse(
                    call: Call<ResponseObject<Giveaways>>,
                    response: retrofit2.Response<ResponseObject<Giveaways>>
                ) {
                    try {
                        val giveaways = getGiveawaysResponseData(response)
                        continuation.resumeWithSafe(Result.success(giveaways))
                    } catch (ex: ApiErrorThrowable) {
                        continuation.resumeWithSafe(Result.failure(ex))
                    }
                }

                override fun onFailure(call: Call<ResponseObject<Giveaways>>, t: Throwable) {
                    continuation.resumeWithSafe(Result.failure(getApiErrorThrowable(t)))
                }
            })
    }

    @Throws(ApiErrorThrowable::class)
    private fun getGiveawaysResponseData(
        response: retrofit2.Response<ResponseObject<Giveaways>>
    ): Giveaways {
        if (response.isSuccessful) {
            val body = response.body()
            if (body == null) {
                throw(ApiErrorThrowable(ApiError.SERVER_ERROR))
            } else {
                if (body.status == ResponseStatus.SUCCESS) {
                    return body.response ?: Giveaways()
                } else {
                    throw(ApiErrorThrowable(ApiError.SERVER_ERROR))
                }
            }
        } else {
            throw(ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200.apply {
                code = response.code()
            }))
        }
    }

    suspend fun checkActiveGiveaways(token: String?): Boolean =
        suspendCancellableCoroutine { continuation ->
            Timber.d("checkActiveGiveaways() ${token}")
            airyService.giveawaysActive(if (token == null) null else "Bearer $token")
                .enqueue(object : Callback<ResponseObject<Giveaways>> {
                    override fun onResponse(
                        call: Call<ResponseObject<Giveaways>>,
                        response: retrofit2.Response<ResponseObject<Giveaways>>
                    ) {
                        val responseData = response.body()?.response
                        val result = responseData != null
                                && responseData.active
                                && responseData.events.isNotEmpty()
                        continuation.resumeWithSafe(Result.success(result))
                    }

                    override fun onFailure(call: Call<ResponseObject<Giveaways>>, t: Throwable) {
                        continuation.resumeWithSafe(Result.failure(getApiErrorThrowable(t)))
                    }
                })
        }

    suspend fun getGiveawaysTicket(token: String): GiveawaysEntryResponse =
        suspendCancellableCoroutine { continuation ->
            airyService.getGiveawaysTicket("Bearer $token")
                .enqueue(object : Callback<ResponseObject<GiveawaysEntryResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<GiveawaysEntryResponse>>,
                        response: retrofit2.Response<ResponseObject<GiveawaysEntryResponse>>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val result = response.body()?.response
                            if (result != null) {
                                continuation.resumeWithSafe(Result.success(result))
                            } else {
                                continuation.resumeWithSafe(
                                    Result.failure(
                                        ApiErrorThrowable(
                                            ApiError.SERVER_ERROR
                                        )
                                    )
                                )
                            }
                        } else {
                            val errorResponse = response.body()?.errors?.firstOrNull()
                            continuation.resumeWithSafe(
                                Result.failure(
                                    ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200, errorResponse)
                                )
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<GiveawaysEntryResponse>>,
                        t: Throwable
                    ) {
                        continuation.resumeWithSafe(Result.failure(getApiErrorThrowable(t)))
                    }
                })
        }


    suspend fun postGiveawaysEntry(
        token: String,
        id: Int,
        ticketCount: Int = 1
    ): GiveawaysEntryResponse = suspendCancellableCoroutine { continuation ->
        val entryRequest = GiveawaysEntryRequest(ticketCount)
        airyService.postGiveawaysEntry("Bearer $token", id, entryRequest)
            .enqueue(object : Callback<ResponseObject<GiveawaysEntryResponse>> {
                override fun onResponse(
                    call: Call<ResponseObject<GiveawaysEntryResponse>>,
                    response: retrofit2.Response<ResponseObject<GiveawaysEntryResponse>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val result = response.body()?.response
                        if (result != null) {
                            continuation.resumeWithSafe(Result.success(result))
                        } else {
                            continuation.resumeWithSafe(
                                Result.failure(
                                    ApiErrorThrowable(ApiError.SERVER_ERROR)
                                )
                            )
                        }
                    } else {
                        val errorResponse = response.body()?.errors?.firstOrNull()
                        continuation.resumeWithSafe(
                            Result.failure(
                                ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200, errorResponse)
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<ResponseObject<GiveawaysEntryResponse>>,
                    t: Throwable
                ) {
                    continuation.resumeWithSafe(Result.failure(getApiErrorThrowable(t)))
                }
            })
    }

    suspend fun getGiveawaysTransactions(token: String): TransactionsResponse =
        suspendCancellableCoroutine { continuation ->
            airyService.getTransactions("Bearer $token")
                .enqueue(object : Callback<ResponseObject<TransactionsResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<TransactionsResponse>>,
                        response: retrofit2.Response<ResponseObject<TransactionsResponse>>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val result = response.body()?.response
                            if (result != null) {
                                continuation.resumeWithSafe(Result.success(result))
                            } else {
                                continuation.resumeWithSafe(
                                    Result.failure(
                                        ApiErrorThrowable(
                                            ApiError.SERVER_ERROR
                                        )
                                    )
                                )
                            }
                        } else {
                            val errorResponse = response.body()?.errors?.firstOrNull()
                            continuation.resumeWithSafe(
                                Result.failure(
                                    ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200, errorResponse)
                                )
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<TransactionsResponse>>,
                        t: Throwable
                    ) {
                        continuation.resumeWithSafe(Result.failure(getApiErrorThrowable(t)))
                    }
                })
        }

    suspend fun getProfile(token: String): ProfileResponse =
        suspendCancellableCoroutine { continuation ->
            airyService.getProfile("Bearer $token")
                .enqueue(object : Callback<ResponseObject<ProfileResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<ProfileResponse>>,
                        response: retrofit2.Response<ResponseObject<ProfileResponse>>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val result = response.body()?.response
                            if (result != null) {
                                continuation.resumeWithSafe(Result.success(result))
                            } else {
                                continuation.resumeWithSafe(
                                    Result.failure(
                                        ApiErrorThrowable(
                                            ApiError.SERVER_ERROR
                                        )
                                    )
                                )
                            }
                        } else {
                            val errorResponse = response.body()?.errors?.firstOrNull()
                            continuation.resumeWithSafe(
                                Result.failure(
                                    ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200, errorResponse)
                                )
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<ProfileResponse>>,
                        t: Throwable
                    ) {
                        continuation.resumeWithSafe(Result.failure(getApiErrorThrowable(t)))
                    }
                })
        }


    @Throws(ApiErrorThrowable::class)
    suspend fun uploadImage(
        token: String,
        file: File,
        mimeType: String
    ): ProfileResponse {
        val fileReqBody: RequestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData(
            "avatar",
            file.name,
            fileReqBody
        )
        val authHeader = "Bearer ${token}"
        return suspendCancellableCoroutine<ProfileResponse> { continuation ->
            airyService.uploadAvatar(authHeader, part)
                .enqueue(object : Callback<ResponseObject<ProfileResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<ProfileResponse>>,
                        response: Response<ResponseObject<ProfileResponse>>
                    ) {
                        val responseBody = response.body()
                        val responseValue = responseBody?.response
                        if (response.isSuccessful && responseValue != null) {
                            continuation.resumeWith(Result.success(responseValue))
                        } else if (!response.isSuccessful) {
                            continuation.resumeWithSafe(Result.failure(ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200)))
                        } else {
                            continuation.resumeWithSafe(Result.failure(ApiErrorThrowable(ApiError.SERVER_ERROR)))
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<ProfileResponse>>,
                        t: Throwable
                    ) {
                        continuation.resumeWithSafe(Result.failure(getApiErrorThrowable(t)))
                    }
                })
        }
    }

    @Throws(ApiErrorThrowable::class)
    suspend fun sendFeedback(
        token: String,
        text: String
    ): Boolean {
        val requestBody = FeedbackBody(
            body = text
        )
        val authHeader = "Bearer ${token}"
        return suspendCancellableCoroutine<Boolean> { continuation ->
            airyService.sendFeedback(authHeader, requestBody)
                .enqueue(object : Callback<ResponseObject<FeedbackResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<FeedbackResponse>>,
                        response: Response<ResponseObject<FeedbackResponse>>
                    ) {
                        if (response.isSuccessful) {
                            continuation.resumeWith(Result.success(true))
                        } else {
                            continuation.resumeWith(Result.failure(ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200)))
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<FeedbackResponse>>,
                        t: Throwable
                    ) {
                        continuation.resumeWith(Result.failure(getApiErrorThrowable(t)))
                    }
                })
        }
    }

    @Throws(ApiErrorThrowable::class)
    suspend fun sendContentSuggestion(
        token: String,
        text: String
    ): Boolean {
        val requestBody = FeedbackBody(
            body = text
        )
        val authHeader = "Bearer ${token}"
        return suspendCancellableCoroutine<Boolean> { continuation ->
            airyService.sendSuggestion(authHeader, requestBody)
                .enqueue(object : Callback<ResponseObject<FeedbackResponse>> {
                    override fun onResponse(
                        call: Call<ResponseObject<FeedbackResponse>>,
                        response: Response<ResponseObject<FeedbackResponse>>
                    ) {
                        if (response.isSuccessful) {
                            continuation.resumeWith(Result.success(true))
                        } else {
                            continuation.resumeWith(Result.failure(ApiErrorThrowable(ApiError.SERVER_RESULT_NOT_200)))
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseObject<FeedbackResponse>>,
                        t: Throwable
                    ) {
                        continuation.resumeWith(Result.failure(getApiErrorThrowable(t)))
                    }
                })
        }
    }

    fun getArchiveApiKey(ld: MutableLiveData<ArchiveApiKey?>) {
        airyService.getArchiveApiKey().enqueue(object : Callback<ResponseObject<ArchiveApiKey>> {
            override fun onResponse(
                call: Call<ResponseObject<ArchiveApiKey>>,
                response: retrofit2.Response<ResponseObject<ArchiveApiKey>>
            ) {
                ld.postValue(response.body()?.response)
            }

            override fun onFailure(call: Call<ResponseObject<ArchiveApiKey>>, t: Throwable) {
                t.printStackTrace()
            }
        })

    }

    fun downloadVastXml(
        url: String,
        onSuccess: ((String) -> Unit)?,
        onError: ((VastDownloadError) -> Unit)?
    ) {
        airyService.downloadVastXml(url).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: retrofit2.Response<String>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.isNotEmpty()) {
                        onSuccess?.invoke(responseBody)
                    } else {
                        onError?.invoke(VastDownloadError.EMPTY)
                    }
                } else {
                    onError?.invoke(VastDownloadError.SERVER_ERROR)
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                onError?.invoke(VastDownloadError.NETWORK_PROBLEM)
            }
        })
    }

    private fun checkApiFailureThrowable(t: Throwable, errorLiveData: MutableLiveData<ApiError>) {
        val apiErrorThrowable = getApiErrorThrowable(t)
        errorLiveData.postValue(apiErrorThrowable.errorType)
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
}