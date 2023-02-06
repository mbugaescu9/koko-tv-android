package com.kokoconnect.android.api

import com.kokoconnect.android.model.ads.interstitial.placeholder.InterstitialPlaceholder
import com.kokoconnect.android.model.response.ResponseObject
import com.kokoconnect.android.util.AppParams
import retrofit2.Call
import retrofit2.http.GET

class InterstitialPlaceholders{
    var placeholders: List<InterstitialPlaceholder> = emptyList()
}

interface AiryPlaceholderAdService {
    @GET("/api/${AppParams.apiVersion}/placeholders")
    fun getInterstitialPlaceholders(): Call<ResponseObject<InterstitialPlaceholders>>
}