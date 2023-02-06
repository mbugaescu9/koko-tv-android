package com.kokoconnect.android.model.ads.interstitial.placeholder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.kokoconnect.android.api.AiryPlaceholderAdService
import com.kokoconnect.android.api.InterstitialPlaceholders
import com.kokoconnect.android.model.response.ResponseObject
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.ui.activity.InterstitialPlaceholderActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.lang.IllegalArgumentException

class InterstitialAd {
    var currentPlaceholder: InterstitialPlaceholder? = null
    var adListener: InterstitialListener? = null
    var isLoaded = false

    private var airyPlaceholderService = Retrofit.Builder()
        .baseUrl(AppParams.serverApiUrl)
        .client(OkHttpClient.Builder().apply {
            if (AppParams.isDebug) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }.build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AiryPlaceholderAdService::class.java)

    fun loadAd(context: Context) {
        airyPlaceholderService.getInterstitialPlaceholders()
            .enqueue(object : Callback<ResponseObject<InterstitialPlaceholders>> {
                override fun onResponse(
                    call: Call<ResponseObject<InterstitialPlaceholders>>,
                    response: Response<ResponseObject<InterstitialPlaceholders>>
                ) {
                    response.body()?.response?.placeholders?.let {
                        if (it.isNotEmpty()) {
                            prepareAd(adListener, context, it.random())
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ResponseObject<InterstitialPlaceholders>>,
                    t: Throwable
                ) {
                    t.printStackTrace()
                }
            })
    }

    fun prepareAd(adListener: InterstitialListener?, context: Context, placeholder: InterstitialPlaceholder) {
        if (adListener == null) return
        currentPlaceholder = placeholder
        Timber.d("Picture: ${placeholder}")
        try {
            Glide.with(context)
                .asBitmap()
                .load(placeholder.picture)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        placeholder.imageBitmap = resource
                        isLoaded = true
                        adListener.onAdLoaded()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}

                })
        } catch(ex: IllegalArgumentException) {
            //if activity was destroyed (onDestroy), Glide throw exception
            ex.printStackTrace()
        }
    }

    fun show(activity: Activity?) {
        val intent = Intent(activity, InterstitialPlaceholderActivity::class.java)
        activity?.startActivity(intent)
    }

}

