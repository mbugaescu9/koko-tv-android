package com.kokoconnect.android

import android.app.Activity
import android.app.Application
import android.app.Service
import com.kokoconnect.android.di.AppInjector
import com.kokoconnect.android.model.event.AmsEventsManager
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.util.NetworkUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import org.joda.time.Instant
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

class AiryTvApp : Application(), HasActivityInjector, HasServiceInjector {
    companion object {
        var instance by Delegates.notNull<AiryTvApp>()
    }

    var startedAt: Instant? = null
    var ams: AmsEventsManager by Delegates.notNull()

    @Inject
    lateinit var dispatchingAndroidActivityInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var dispatchingAndroidServiceInjector: DispatchingAndroidInjector<Service>

    override fun activityInjector() = dispatchingAndroidActivityInjector

    override fun serviceInjector() = dispatchingAndroidServiceInjector

    override fun onCreate() {
        super.onCreate()
        instance = this
        startedAt = Instant.now()

        AppInjector.init(this)

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!AppParams.isDebug)

//        if (AppParams.isDebug) {
            Timber.plant(Timber.DebugTree())
//        }
        NetworkUtils.setupSSLConnection(this)
        ams = AmsEventsManager(this)
    }

    fun getDurationFromStart() : Long {
        val currentTime = Instant.now().millis
        val startedAt = startedAt?.millis ?: currentTime
        return currentTime - startedAt
    }
}