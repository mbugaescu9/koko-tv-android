package com.kokoconnect.android.di

import com.kokoconnect.android.service.AmsService
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Suppress("unused")
@Module
abstract class AmsServiceModule {
    @ContributesAndroidInjector
    abstract fun contributeAmsService(): AmsService
}