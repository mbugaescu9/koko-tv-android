package com.kokoconnect.android.vm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kokoconnect.android.R
import com.kokoconnect.android.model.ads.interstitial.InterstitialTrigger
import com.kokoconnect.android.model.tv.Channel
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.deeplink.DeeplinkData
import com.kokoconnect.android.model.deeplink.DeeplinkManager
import com.kokoconnect.android.model.vod.Content
import com.kokoconnect.android.ui.activity.*
import org.jetbrains.anko.internals.AnkoInternals
import org.joda.time.Duration
import org.joda.time.Instant
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class NavigationViewModel @Inject constructor() : ViewModel() {
    companion object {
        const val URL_PATH_SEPARATOR = "/"
    }
    private val deeplinkManager = DeeplinkManager()

    val navigationPath: LinkedList<Screen> = LinkedList()
    var navigationPathChanged = MutableLiveData<Boolean>()
    var lastScreenSwitchTime: Instant? = null
    var lastScreenDurationSec: Int = 0
    var onNeedShowInterstitial = MutableLiveData<InterstitialTrigger?>()

    val currentScreen: Screen?
        get() {
            return navigationPath.lastOrNull()
        }
    val previousScreen: Screen?
        get() {
            return navigationPath.getOrNull(navigationPath.size - 2)
        }

    fun setCurrentScreen(screen: Screen, fragment: Fragment? = null) {
        if (screen.type != currentScreen?.type && screen.name.isNotEmpty()) {
            if (screen.isParentOf(currentScreen)) {
                if (navigationPath.isNotEmpty()) {
                    navigationPath.removeLast()
                }
            } else if (screen.isChildOf(currentScreen)) {
                navigationPath.addLast(screen)
            } else {
                if (navigationPath.isNotEmpty()) {
                    navigationPath.removeLast()
                }
                navigationPath.addLast(screen)
            }

            val thisTime = Instant.now()
            lastScreenSwitchTime?.let {
                lastScreenDurationSec = Duration.millis(thisTime.millis - it.millis).toSeconds()
            }
            lastScreenSwitchTime = Instant.now()

            AdsViewModel.onScreenChange()
            navigationPathChanged.postValue(true)
            onNeedShowInterstitial.postValue(InterstitialTrigger.OnScreenChange)
        }
    }

    fun createUrl() : String {
        val navigationPathString = navigationPath.map { it.name }.joinToString(URL_PATH_SEPARATOR)
        return "airy://$navigationPathString"
    }

    fun onFragmentChanged() {
        Timber.d("NavigationViewModel: onFragmentChanged()")
        onNeedShowInterstitial.postValue(InterstitialTrigger.OnScreenChange)
    }

    fun onBackPressed() {
        Timber.d("NavigationViewModel: backPressed()")
        onNeedShowInterstitial.postValue(InterstitialTrigger.OnScreenChange)
    }

    private inline fun <reified T: Activity> openActivity(currentActivity: BaseActivity,
                                                          isScreenChange: Boolean = true,
                                                          vararg params: Pair<String, Any?>) {
        currentActivity.startActivity(AnkoInternals.createIntent(currentActivity, T::class.java, params))
        currentActivity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        if (isScreenChange) {
            Timber.d("NavigationViewModel: openActivity() ${T::class.java}")
            onNeedShowInterstitial?.postValue(InterstitialTrigger.OnScreenChange)
        } else {
            Timber.d("NavigationViewModel: openActivity() ${T::class.java} without interstitial()")
        }
    }

    suspend fun fetchDeeplinkData(intent: Intent?): DeeplinkData? {
        return deeplinkManager.fetchDeeplinkData(intent)
    }

    fun createShareChannelLink(channel: Channel?): Uri? {
        return deeplinkManager.createShareChannelLink(channel = channel)
    }

    fun createShareContentLink(content: Content?): Uri? {
        return deeplinkManager.createShareContentLink(content = content)
    }
}

private fun Duration.toSeconds(): Int = this.toStandardSeconds().seconds