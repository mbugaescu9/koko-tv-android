package com.kokoconnect.android.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kokoconnect.android.R
import com.kokoconnect.android.model.ads.interstitial.InterstitialTrigger
import com.kokoconnect.android.model.notification.Notification
import com.kokoconnect.android.model.notification.UserAlert
import com.kokoconnect.android.model.player.ArchiveApiKey
import com.kokoconnect.android.model.player.SecurityData
import com.kokoconnect.android.model.player.proxy.ChromecastConnectionListener
import com.kokoconnect.android.model.player.proxy.ChromecastProxyBuilder
import com.kokoconnect.android.model.player.proxy.chromecast.ChromecastProxyImplementation
import com.kokoconnect.android.model.settings.UiTheme
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.ui.dialog.RatingDialog
import com.kokoconnect.android.ui.dialog.RatingGooglePlayDialog
import com.kokoconnect.android.ui.dialog.ChooseThemeDialogFragment
import com.kokoconnect.android.ui.dialog.CustomDialog
import com.kokoconnect.android.util.ActivityUtils
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.util.GPSUtils
import com.kokoconnect.android.vm.*
import com.kokoconnect.android.vm.freegift.GiveawaysViewModel
import com.kokoconnect.android.vm.profile.AuthViewModel
import com.kokoconnect.android.vm.tv.TvGuideViewModel
import com.kokoconnect.android.vm.tv.TvPlayersViewModel
import com.kokoconnect.android.vm.vod.VodContentViewModel
import com.kokoconnect.android.vm.vod.VodPlayersViewModel
import com.google.gson.Gson
import org.jetbrains.anko.contentView
import org.jetbrains.anko.startActivity
import timber.log.Timber

interface ActivityWithAds {
    fun lockUi()
    fun unlockUi()
}

abstract class BaseActivity : AppCompatActivity(), ActivityWithAds, ChromecastProxyBuilder {

    val adsViewModel: AdsViewModel by viewModels{ getViewModelFactory() }
    val tvGuideViewModel: TvGuideViewModel by viewModels{ getViewModelFactory() }
    val navigationViewModel: NavigationViewModel by viewModels { getViewModelFactory() }
    val eventsViewModel: AmsEventsViewModel by viewModels { getViewModelFactory() }
    val giveawaysViewModel: GiveawaysViewModel by viewModels { getViewModelFactory() }
    val playerViewModel: PlayerViewModel by viewModels{ getViewModelFactory() }
    val tvPlayersViewModel: TvPlayersViewModel by viewModels{ getViewModelFactory() }
    val vodPlayersViewModel: VodPlayersViewModel by viewModels{ getViewModelFactory() }
    val vodContentViewModel: VodContentViewModel by viewModels { getViewModelFactory() }
    val notificationsViewModel: NotificationsViewModel by viewModels{ getViewModelFactory() }
    val authViewModel: AuthViewModel by viewModels{ getViewModelFactory() }
    var isFocused: Boolean = false

    abstract fun getViewModelFactory(): ViewModelProvider.Factory
    abstract fun getLayoutId(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateTheme()
        setContentView(getLayoutId())
        isFocused = true

        eventsViewModel.connectWithTvGuideInfo(tvGuideViewModel)
        eventsViewModel.connectWithVodContentInfo(vodContentViewModel)

        navigationViewModel.navigationPathChanged.observe(this, Observer {
            if (it != null && it) {
                val newUrl = navigationViewModel.createUrl()
                val lastDuration = navigationViewModel.lastScreenDurationSec
                Timber.d("navigationPathChanged ${newUrl}")
                eventsViewModel.sendBrowserEvent(newUrl, lastDuration)
                navigationViewModel.navigationPathChanged.value = null
            }
        })
        navigationViewModel.onNeedShowInterstitial.observe(this, Observer {
            if (it != null) {
                adsViewModel.needShowInterstitial.postValue(it)
                navigationViewModel.onNeedShowInterstitial.postValue(null)
            }
        })

        adsViewModel.needShowInterstitial.observe(this, Observer { trigger ->
            if (trigger != null) {
                if (isNeedShowUserAlert()) {
                    openUserAlert()
                } else {
                    val channelId = tvGuideViewModel.getCurrentDescription()?.channelId
                    showInterstitials(trigger, channelId)
                }
                adsViewModel.needShowInterstitial.value = null
            }
        })

        notificationsViewModel.getUserAlert().observe(this, Observer {
            it?.let {
                notificationsViewModel.userAlertEnabled = it.isShow ?: false
                notificationsViewModel.userAlertData = it
                if (it.showAlertOnStart == true && !notificationsViewModel.isUserAlertAlreadyShow) {
                    openUserAlert(it)
                }
            }
        })

        notificationsViewModel.needShowNotification.observe(this, Observer {
            if (it != null) {
                showNotification(it)
                notificationsViewModel.needShowNotification.value = null
            }
        })

        notificationsViewModel.needShowRatingDialog.observe(this, Observer {
            if (it == true) {
                showRatingDialog()
                notificationsViewModel.needShowRatingDialog.value = null
            }
        })

        authViewModel.isAuthorized.observe(this, Observer {
            invalidateOptionsMenu()
            if (!it) eventsViewModel.updateUser()
        })

        authViewModel.needUpdateArchiveApiKey.observe(this, Observer {
            tvPlayersViewModel.setSecurityData(SecurityData(
                authKey = it ?: ArchiveApiKey(),
                cookie = Preferences(this).ArchiveOrg().getCookies() ?: ""
            ))
        })

        if (AppParams.appType.hasGooglePlayServices) {
            setupGooglePlayServices()
        }
        notificationsViewModel.init(lifecycle)
        playerViewModel.init(lifecycle)
        tvPlayersViewModel.init(playerViewModel)
        vodPlayersViewModel.init(playerViewModel)
        tvPlayersViewModel.initChromecast(this)
        vodPlayersViewModel.initChromecast(this)
        eventsViewModel.startStaticTimerEvent()
    }

    fun updateTheme() {
        val currentThemeNumber = ActivityUtils.getIntFromAttr(this, R.attr.themeNumber)
        val selectedTheme = getSelectedTheme()
        Timber.d("currentThemeNumber = ${currentThemeNumber} selectedThemeNumber = ${selectedTheme.themeNumber}")
        Timber.d("updateTheme() ${selectedTheme.themeName}")
        if (contentView == null) {
            setTheme(selectedTheme.resId)
        } else if (currentThemeNumber != selectedTheme.themeNumber) {
            setTheme(selectedTheme.resId)
            Timber.d("setTheme ${selectedTheme.themeNumber}")
            recreate()
        }
    }

    fun getSelectedTheme(): UiTheme {
        return Preferences(this).UI().getTheme()
    }

    override fun onStart() {
        super.onStart()
        authViewModel.checkToken()
        notificationsViewModel.getNotifications().observe(this, Observer {
            notificationsViewModel.addNotifications(it.notifications)
        })
    }

    override fun onResume() {
        isFocused = true
        super.onResume()
        eventsViewModel.resumeStaticTimerEvent()
        updateTheme()
    }

    override fun onPause() {
        isFocused = false
        super.onPause()
        eventsViewModel.pauseStaticTimerEvent()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigationViewModel.onBackPressed()
    }

    private fun showNotification(notification: Notification) {
        val builder = CustomDialog.Builder()
            .setTitle(notification.name)
            .setMessage(notification.text)
            .setOnClickListener {
                notificationsViewModel.onNotificationClosed(notification)
            }
        if (notification.name.isNotBlank()) {
            builder.setTitle(notification.name)
        }
        builder.build(this).show()
    }

    private fun showInterstitials(
        trigger: InterstitialTrigger,
        channelId: Int?
    ) {
        adsViewModel.showInterstitials(this, lifecycleScope, trigger, channelId) {
//            if (trigger == AdsViewModel.InterstitialTrigger.SCREEN_CHANGE_BACKWARD) {
//                super.onBackPressed()
//            }
        }
    }

    fun openUserAlert(userAlertData: UserAlert? = notificationsViewModel.userAlertData) {
        Timber.d("openUserAlert ($userAlertData)")
        userAlertData?.let {
            if (it.isShow == true) {
                notificationsViewModel.isUserAlertAlreadyShow = true
                val data = Gson().toJson(it)
                startActivity<UserAlertActivity>(USER_ALERT_DATA to data)
                if (it.allowContinue == false) {
                    finish()
                }
            }
        }
    }

    private fun showRatingDialog() {
        val ratingDialogShown = supportFragmentManager.findFragmentByTag(RatingDialog.TAG) != null
                || supportFragmentManager.findFragmentByTag(RatingGooglePlayDialog.TAG) != null
        if (isFocused && !ratingDialogShown) {
            RatingDialog(
                notificationsViewModel.ratingManager,
                eventsViewModel
            ).show(supportFragmentManager, RatingDialog.TAG)
        }
    }

    private fun isNeedShowUserAlert(): Boolean =
        notificationsViewModel.userAlertEnabled && !notificationsViewModel.isUserAlertAlreadyShow

    fun openThemeDialog() {
        val dialog = ChooseThemeDialogFragment()
        dialog.setListener(object : ChooseThemeDialogFragment.Listener {
            override fun onChooseTheme(theme: UiTheme) {
                changeUiTheme(theme)
            }
        })
        dialog.show(supportFragmentManager, dialog.javaClass.name)
    }

    fun changeUiTheme(theme: UiTheme) {
        val uiPref = Preferences(this).UI()
        if (uiPref.getTheme().prefName != theme.prefName) {
            Preferences(this).UI().setTheme(theme)
            recreate()
        }
    }

    private fun setupGooglePlayServices() {
        GPSUtils.googlePlayServicesStarted.observe(this, Observer { isStart ->
            if (isStart == true) {
                tvPlayersViewModel.initChromecast(this)
                Timber.d("googlePlayServicesStarted = ${isStart}")
            } else {
                GPSUtils.checkGooglePlayServices(this)
            }
        })
    }

    override fun initChromecastProxy(listener: ChromecastConnectionListener) {
        try {
            ChromecastProxyImplementation.builder(this, listener)
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (AppParams.appType.hasGooglePlayServices) {
            GPSUtils.checkGooglePlayServices(this)
        }
    }
}