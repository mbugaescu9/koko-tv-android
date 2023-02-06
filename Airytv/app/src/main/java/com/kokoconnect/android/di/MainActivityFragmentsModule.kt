package com.kokoconnect.android.di

import com.kokoconnect.android.ui.fragment.freegift.GiveawaysMainFragment
import com.kokoconnect.android.ui.fragment.freegift.TransactionsFragment
import com.kokoconnect.android.ui.fragment.profile.*
import com.kokoconnect.android.ui.fragment.tv.*
import com.kokoconnect.android.ui.fragment.vod.*
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Suppress("unused")
@Module
abstract class MainActivityFragmentsModule {
    @ContributesAndroidInjector
    abstract fun contributeGuideFragment(): GuideFragment
    @ContributesAndroidInjector
    abstract fun contributePlayerFragment(): PlayerFragment
    @ContributesAndroidInjector
    abstract fun contributeDescriptionFragment(): DescriptionFragment
    @ContributesAndroidInjector
    abstract fun contributeDescriptionFullscreenFragment(): DescriptionFullscreenFragment
    @ContributesAndroidInjector
    abstract fun contributeChannelsFragment(): ChannelsMainFragment

    @ContributesAndroidInjector
    abstract fun contributeVodFragment(): VodMainFragment
    @ContributesAndroidInjector
    abstract fun contributeContentAllFragment(): ContentAllFragment
    @ContributesAndroidInjector
    abstract fun contributeContentCollectionFragment(): ContentCollectionFragment
    @ContributesAndroidInjector
    abstract fun contributeContentSeriesFragment(): ContentSeriesFragment
    @ContributesAndroidInjector
    abstract fun contributeCollectionFragment(): CollectionFragment
    @ContributesAndroidInjector
    abstract fun contributeSeriesFragment(): SeriesFragment
    @ContributesAndroidInjector
    abstract fun contributeEpisodesFragment(): EpisodesFragment
    @ContributesAndroidInjector
    abstract fun contributeWebViewFragment(): ContentFragment
    @ContributesAndroidInjector
    abstract fun contributeVodPlayerFragment(): VodPlayerFragment


    @ContributesAndroidInjector
    abstract fun contributeGiveawaysFragment(): GiveawaysMainFragment

    @ContributesAndroidInjector
    abstract fun contributeProfileMainFragment(): ProfileMainFragment
    @ContributesAndroidInjector
    abstract fun contributeProfileFragment(): ProfileFragment
    @ContributesAndroidInjector
    abstract fun contributePrivacyPolicyFragment(): PrivacyPolicyFragment
    @ContributesAndroidInjector
    abstract fun contributeSuggestionFeedbackSelectFragment(): SuggestionFeedbackSelectFragment
    @ContributesAndroidInjector
    abstract fun contributeTechnicalAssistanceFragment(): TechnicalAssistanceFragment
    @ContributesAndroidInjector
    abstract fun contributeContentSuggestionFragment(): ContentSuggestionFragment
    @ContributesAndroidInjector
    abstract fun contributeTransactionsFragment(): TransactionsFragment
    @ContributesAndroidInjector
    abstract fun contributeSignInEmailFragment(): SignInEmailFragment
    @ContributesAndroidInjector
    abstract fun contributeSignInFragment(): SignInFragment
    @ContributesAndroidInjector
    abstract fun contributeSignUpFragment(): SignUpFragment
    @ContributesAndroidInjector
    abstract fun contributeResetPasswordFragment(): ResetPasswordFragment
}
