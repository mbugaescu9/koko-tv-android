package com.kokoconnect.android.di

import com.kokoconnect.android.api.AirySecurityService
import com.kokoconnect.android.api.AiryService
import com.kokoconnect.android.api.AiryServiceFactory
import com.kokoconnect.android.api.AmsEventsService
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module(includes = [ViewModelModule::class])
class ServicesModule {
    @Singleton
    @Provides
    fun provideAiryService(): AiryService {
        return AiryServiceFactory.createAiryService()
    }

    @Singleton
    @Provides
    fun provideAirySecurityService(): AirySecurityService {
        return AiryServiceFactory.createSecurityService()
    }

    @Singleton
    @Provides
    fun provideAmsEventsService(): AmsEventsService {
        return AiryServiceFactory.createAmsEventsService()
    }
}
