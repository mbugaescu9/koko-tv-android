package com.kokoconnect.android.model.ads.banner

import android.app.Activity

interface AdsObjectsProvider {
    fun provideActivity(): Activity?
    fun provideBannerManager(): BannerManager?
}