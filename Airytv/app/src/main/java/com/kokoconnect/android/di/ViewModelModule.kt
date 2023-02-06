package com.kokoconnect.android.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kokoconnect.android.vm.*
import com.kokoconnect.android.vm.freegift.GiveawaysViewModel
import com.kokoconnect.android.vm.profile.AuthViewModel
import com.kokoconnect.android.vm.profile.ProfileViewModel
import com.kokoconnect.android.vm.tv.TvGuideViewModel
import com.kokoconnect.android.vm.tv.TvPlayersViewModel
import com.kokoconnect.android.vm.vod.*
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Suppress("unused")
@Module
abstract class ViewModelModule {
    @Binds
    @Singleton
    @IntoMap
    @ViewModelKey(AmsEventsViewModel::class)
    abstract fun bindAmsEventsViewModel(amsEventsViewModel: AmsEventsViewModel): ViewModel

    @Binds
    @Singleton
    @IntoMap
    @ViewModelKey(GiveawaysViewModel::class)
    abstract fun bindGiveawaysViewModel(giveawaysViewModel: GiveawaysViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AdsViewModel::class)
    abstract fun bindAdsViewModel(adsViewModel: AdsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TvGuideViewModel::class)
    abstract fun bindGuideViewModel(tvGuideViewModel: TvGuideViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationsViewModel::class)
    abstract fun bindNotificationsViewModel(notificationsViewModel: NotificationsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AuthViewModel::class)
    abstract fun bindAuthViewModel(authViewModel: AuthViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindProfileViewModel(profileViewModel: ProfileViewModel): ViewModel

    @Binds
    @Singleton
    @IntoMap
    @ViewModelKey(VodContentViewModel::class)
    abstract fun bindContentViewModel(collectionViewModel: VodContentViewModel): ViewModel

    @Binds
    @Singleton
    @IntoMap
    @ViewModelKey(VodViewModel::class)
    abstract fun bindTvShowsViewModel(vodViewModel: VodViewModel): ViewModel

    @Binds
    @Singleton
    @IntoMap
    @ViewModelKey(CollectionViewModel::class)
    abstract fun bindCollectionViewModel(collectionViewModel: CollectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SeriesViewModel::class)
    abstract fun bindSeriesViewModel(seriesViewModel: SeriesViewModel): ViewModel

    @Binds
    @Singleton
    @IntoMap
    @ViewModelKey(NavigationViewModel::class)
    abstract fun bindNavigationViewModel(navigationViewModel: NavigationViewModel): ViewModel

    @Binds
    @Singleton
    @IntoMap
    @ViewModelKey(TvPlayersViewModel::class)
    abstract fun bindPlayersViewModel(tvPlayersViewModel: TvPlayersViewModel): ViewModel

    @Binds
    @Singleton
    @IntoMap
    @ViewModelKey(VodPlayersViewModel::class)
    abstract fun bindVodPlayersViewModel(vodPlayersViewModel: VodPlayersViewModel): ViewModel

    @Binds
    @Singleton
    @IntoMap
    @ViewModelKey(PlayerViewModel::class)
    abstract fun bindPlayerViewModel(playerViewModel: PlayerViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
