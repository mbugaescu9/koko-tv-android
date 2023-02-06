package com.kokoconnect.android.di

import com.kokoconnect.android.ui.activity.WebViewActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Suppress("unused")
@Module
abstract class WebViewActivityModule {
    @ContributesAndroidInjector
    abstract fun contributeWebViewActivity(): WebViewActivity
}