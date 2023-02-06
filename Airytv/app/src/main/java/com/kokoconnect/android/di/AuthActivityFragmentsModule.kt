package com.kokoconnect.android.di

import com.kokoconnect.android.ui.fragment.profile.ResetPasswordFragment
import com.kokoconnect.android.ui.fragment.profile.SignInEmailFragment
import com.kokoconnect.android.ui.fragment.profile.SignInFragment
import com.kokoconnect.android.ui.fragment.profile.SignUpFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Suppress("unused")
@Module
abstract class AuthActivityFragmentsModule {
    @ContributesAndroidInjector
    abstract fun contributeSignInEmailFragment(): SignInEmailFragment
    @ContributesAndroidInjector
    abstract fun contributeSignInFragment(): SignInFragment
    @ContributesAndroidInjector
    abstract fun contributeSignUpFragment(): SignUpFragment
    @ContributesAndroidInjector
    abstract fun contributeResetPasswordFragment(): ResetPasswordFragment
}
