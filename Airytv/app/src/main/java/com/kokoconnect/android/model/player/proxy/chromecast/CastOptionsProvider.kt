package com.kokoconnect.android.model.player.proxy.chromecast

import android.content.Context
import com.kokoconnect.android.util.AppParams
import com.google.android.gms.cast.framework.SessionProvider

// don't forget add meta data OPTIONS_PROVIDER_CLASS_NAME in AndroidManifest.xml
class CastOptionsProvider : com.google.android.gms.cast.framework.OptionsProvider {
    override fun getCastOptions(appContext: Context): com.google.android.gms.cast.framework.CastOptions {
        val receiverId = AppParams.firebaseId
        return com.google.android.gms.cast.framework.CastOptions.Builder()
            .setReceiverApplicationId(receiverId)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}
