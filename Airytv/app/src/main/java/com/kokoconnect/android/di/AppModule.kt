package com.kokoconnect.android.di

import dagger.Module
import dagger.android.AndroidInjectionModule

@Module(includes = [
    AndroidInjectionModule::class,
    MainActivityModule::class,
    WebViewActivityModule::class,
    AmsServiceModule::class,
    RepoModule::class,
    ServicesModule::class,
    ViewModelModule::class])
class AppModule {

}
