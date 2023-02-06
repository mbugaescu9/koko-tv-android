package com.kokoconnect.android.di

import com.kokoconnect.android.AiryTvApp
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        AppModule::class
    ]
)
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(app: AiryTvApp): Builder

        fun build(): AppComponent
    }

    fun inject(airyApp: AiryTvApp)
}
