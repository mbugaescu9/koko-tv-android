package com.kokoconnect.android.di

import com.kokoconnect.android.ui.activity.MainActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Suppress("unused")
@Module
abstract class MainActivityModule {
    @ContributesAndroidInjector(modules = [MainActivityFragmentsModule::class])
    abstract fun contributeMainActivity(): MainActivity
}

