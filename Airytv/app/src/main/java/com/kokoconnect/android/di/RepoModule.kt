package com.kokoconnect.android.di

import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.api.AirySecurityService
import com.kokoconnect.android.api.AiryService
import com.kokoconnect.android.repo.AuthRepository
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.repo.ImageRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepoModule {
    @Provides
    @Singleton
    fun provideAiryRepository(airyService: AiryService): AiryRepository = AiryRepository(airyService)

    @Provides
    @Singleton
    fun provideAuthRepository(
        app: AiryTvApp,
        airySecurityService: AirySecurityService
    ): AuthRepository = AuthRepository(app, airySecurityService)


    @Provides
    @Singleton
    fun provideImageRepository(
        app: AiryTvApp
    ): ImageRepository = ImageRepository(app)
}