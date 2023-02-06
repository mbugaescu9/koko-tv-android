package com.kokoconnect.android.api

import com.kokoconnect.android.model.vod.Content
import com.kokoconnect.android.util.AppParams
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object AiryServiceFactory {
    private val exclusionStrategies = listOf(Content.EXCLUSION_STRATEGY)
    private val serializerExclusionStrategy = object: ExclusionStrategy {
        override fun shouldSkipField(f: FieldAttributes?): Boolean {
            var shouldSkipField = false
            for (strategy in exclusionStrategies) {
                shouldSkipField = strategy.shouldSkipField(f)
                if (shouldSkipField) {
                    break
                }
            }
            return shouldSkipField
        }

        override fun shouldSkipClass(clazz: Class<*>?): Boolean {
            var shouldSkipClass = false
            for (strategy in exclusionStrategies) {
                shouldSkipClass = strategy.shouldSkipClass(clazz)
                if (shouldSkipClass) {
                    break
                }
            }
            return shouldSkipClass
        }
    }

    fun createAiryService(): AiryService {
        val gson = GsonBuilder()
            .registerTypeAdapterFactory(Content.TYPE_ADAPTER_FACTORY)
            .setExclusionStrategies(serializerExclusionStrategy)
            .create()
        return Retrofit.Builder()
            .baseUrl(AppParams.serverApiUrl)
//            .baseUrl("https://api.airy.tv/") // release
//            .baseUrl("https://api.demo.showfer.com/") // demo
            .client(OkHttpClient.Builder().apply {
                if (AppParams.isDebug) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }.build())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AiryService::class.java)
    }

    fun createSecurityService(): AirySecurityService {
        return Retrofit.Builder()
            .baseUrl(AppParams.serverApiUrl)
//            .baseUrl("https://api.airy.tv/") // release
//            .baseUrl("https://api.demo.showfer.com/") // demo
            .client(OkHttpClient.Builder().apply {
                if (AppParams.isDebug) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AirySecurityService::class.java)
    }

    fun createAmsEventsService(): AmsEventsService {
        return Retrofit.Builder()
            .baseUrl(AppParams.serverAmsApiUrl)
//            .baseUrl("https://apiams.airy.tv/") // release
//            .baseUrl("https://apiams.demo.showfer.com/") // demo
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AmsEventsService::class.java)
    }
}