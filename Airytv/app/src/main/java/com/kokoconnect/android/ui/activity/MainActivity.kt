package com.kokoconnect.android.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.MainFragmentPagerAdapter
import com.kokoconnect.android.databinding.ActivityMainBinding
import com.kokoconnect.android.model.AiryContentType
import com.kokoconnect.android.model.deeplink.ChannelDeeplinkData
import com.kokoconnect.android.model.event.LandingEvent
import com.kokoconnect.android.model.player.SecurityData
import com.kokoconnect.android.util.isOrientationLandscape
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.coroutines.launch
//import org.jetbrains.anko.contentView
//import org.jetbrains.anko.contentView

import org.jetbrains.anko.contentView
import timber.log.Timber
import javax.inject.Inject


class MainActivity : BaseActivity(), HasSupportFragmentInjector {

    private var binding: ActivityMainBinding? = null

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    var mainFragmentsAdapter: MainFragmentPagerAdapter? = null

    override fun getViewModelFactory(): ViewModelProvider.Factory = vmFactory
    override fun getLayoutId(): Int = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contentView?.let {
            binding = ActivityMainBinding.bind(it)
        }
//        binding = ActivityMainBinding.inflate(layoutInflater)
//
//        setContentView(binding?.root)
        isFocused = true

        setupToolbar()
        setSplashScreenVisible(!tvGuideViewModel.isGuideLoaded())

        eventsViewModel.sendLandingEvent(LandingEvent.URL_START)

        tvGuideViewModel.requestGuideFromServer()

        adsViewModel.adsStatus.observe(this, Observer {
            if (it != null) {
                adsViewModel.initAdsSDK(this@MainActivity, it.getAllAds())
                adsViewModel.initAdsAll(this@MainActivity)
            }
        })

        tvGuideViewModel.guideLiveData.observe(this, Observer {
            if (it != null) {
                onDataReady()
            }
        })

        playerViewModel.isFullscreenLiveData.observe(this, Observer {
            onFullscreen(playerViewModel.isFullscreen)
        })

        authViewModel.needOpenAuth.observe(this, Observer {
            if (it == true) {
                binding?.vpMain?.setCurrentItem(3, false)
            }
        })

        fetchDeeplink()
        updateContentType(0)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        fetchDeeplink()
    }

    private fun fetchDeeplink() {
        lifecycleScope.launch {
            navigationViewModel.fetchDeeplinkData(intent)?.let { deeplinkData ->
                if (deeplinkData is ChannelDeeplinkData) {
                    tvGuideViewModel.openChannel(deeplinkData.channelNumber)
                    Timber.d("fetchDeeplink() ChannelDeeplinkData channel number${deeplinkData.channelNumber}")
                }
            }
        }
    }

    private fun setupToolbar() {
        val viewPager = binding?.vpMain
        val bottomNavigation = binding?.bottomNavigation
        if (bottomNavigation != null && viewPager != null) {
            Timber.d("set viewpager ")
            mainFragmentsAdapter = MainFragmentPagerAdapter(this, this)
            viewPager.adapter = mainFragmentsAdapter
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateContentType(position)
                    updateBottomNavigation(position)
                    switchOrientation(position)
                }
            })
            viewPager.isUserInputEnabled = false
            bottomNavigation.rlTv.setOnClickListener {
                viewPager.setCurrentItem(0, false)
            }
            bottomNavigation.rlVod.setOnClickListener {
                viewPager.setCurrentItem(1, false)
            }
            bottomNavigation.rlFreeGift.setOnClickListener {
                viewPager.setCurrentItem(2, false)
            }
            bottomNavigation.rlProfile.setOnClickListener {
                viewPager.setCurrentItem(3, false)
            }
        }
        updateOrientation()
    }

    fun updateBottomNavigation(selectedItemPosition: Int) {
        val isTv = selectedItemPosition == 0
        val isVod = selectedItemPosition == 1
        val isFreeGift = selectedItemPosition == 2
        val isProfile = selectedItemPosition == 3
        binding?.bottomNavigation?.rlTv?.isSelected = isTv
        binding?.bottomNavigation?.ivTv?.isSelected = isTv
        binding?.bottomNavigation?.tvTv?.isSelected = isTv
        binding?.bottomNavigation?.rlVod?.isSelected = isVod
        binding?.bottomNavigation?.ivVod?.isSelected = isVod
        binding?.bottomNavigation?.tvVod?.isSelected = isVod
        binding?.bottomNavigation?.rlFreeGift?.isSelected = isFreeGift
        binding?.bottomNavigation?.ivFreeGift?.isSelected = isFreeGift
        binding?.bottomNavigation?.tvFreeGift?.isSelected = isFreeGift
        binding?.bottomNavigation?.rlProfile?.isSelected = isProfile
        binding?.bottomNavigation?.ivProfile?.isSelected = isProfile
        binding?.bottomNavigation?.tvProfile?.isSelected = isProfile
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        playerViewModel.screenConfiguration.postValue(newConfig)
        val currentPage = binding?.vpMain?.currentItem ?: 0
        when (currentPage) {
            0, 1 -> {
            }
            else -> {
                recreate()
            }
        }
        updateOrientation()
    }

    private fun updateContentType(pageNumber: Int) {
        Timber.d("updateContentType() pageNumber = ${pageNumber}")
        when(pageNumber) {
            0 -> playerViewModel.setCurrentContentType(AiryContentType.TV)
            1 -> playerViewModel.setCurrentContentType(AiryContentType.VOD)
        }
    }

    private fun updateOrientation(isLandscape: Boolean = isOrientationLandscape()) {
        if (isLandscape) {
            binding?.bottomNavigation?.root?.visibility = View.GONE
        } else {
            binding?.bottomNavigation?.root?.visibility = View.VISIBLE
        }
    }

    private fun switchOrientation(currentPage: Int) {
        when (currentPage) {
            0, 1 -> {
                if (!playerViewModel.isFullscreen) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                }
            }
            else -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }


    @SuppressLint("SourceLockedOrientationActivity")
    private fun onFullscreen(isFullScreen: Boolean) {
        Timber.d("onFullscreen() ${isFullScreen}")
        val newOrientation = if (isFullScreen) {
            hideSystemUI()
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            showSystemUI()
            ActivityInfo.SCREEN_ORIENTATION_USER
        }
        if (requestedOrientation != newOrientation) {
            requestedOrientation = newOrientation
        }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            val decorView = window.decorView
            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            binding?.rootLayout?.fitsSystemWindows = false
            binding?.rootLayout?.requestApplyInsets()
        }
    }

    @Suppress("DEPRECATION")
    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.let {
                it.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        } else {
            val decorView = window.decorView
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                decorView.systemUiVisibility = 0
            }
            binding?.rootLayout?.fitsSystemWindows = true
            binding?.rootLayout?.requestApplyInsets()
        }
    }

    private fun onDataReady() {
        if (isSplashScreenVisible()) {
            setSplashScreenVisible(false)
        }
        if (tvGuideViewModel.isGuideNotEmpty()) {
            eventsViewModel.sendLandingEvent(LandingEvent.URL_LOADED)
            if (isSplashScreenVisible()) {
                setSplashScreenVisible(false)
            }
        } else {
            eventsViewModel.sendLandingEvent(LandingEvent.URL_EMPTY)
        }
    }

    private fun isSplashScreenVisible(): Boolean {
        return binding?.splashScreenLayout?.root?.visibility == View.VISIBLE
    }

    private fun setSplashScreenVisible(visible: Boolean) {
        if (visible) {
            binding?.splashScreenLayout?.root?.visibility = View.VISIBLE
            binding?.splashScreenLayout?.splashProgress?.startProgress()
        } else if (isSplashScreenVisible()) {
            binding?.splashScreenLayout?.splashProgress?.stopProgress { }
            binding?.splashScreenLayout?.root?.visibility = View.GONE
            notificationsViewModel.checkNeedShowRatingDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        authViewModel.updateArchiveApiKey()
        val cookie = Preferences(this).ArchiveOrg().getCookies() ?: ""
        val authKey = authViewModel.archiveApiKey
        tvPlayersViewModel.setSecurityData(
            SecurityData(
                authKey = authKey,
                cookie = cookie
            )
        )
    }

    override fun onResume() {
        super.onResume()
        if (playerViewModel.isFullscreen) {
            hideSystemUI()
        }
        if (!isSplashScreenVisible()) {
            notificationsViewModel.checkNeedShowRatingDialog()
        }
    }

    override fun onBackPressed() {
        try {
            if (adsViewModel.onBackPressed()) {
                return
            } else if (playerViewModel.isFullscreen) {
                playerViewModel.switchFullscreen()
            } else {
                try {
                    supportFragmentManager.popBackStackImmediate()
                    if (!tvGuideViewModel.isGuideLoaded()) {
                        eventsViewModel.sendLandingEvent(LandingEvent.URL_INTERRUPT)
                    }
                    eventsViewModel.sendBrowserEvent("Exit")
                    super.onBackPressed()
                } catch (ex: IllegalStateException) {
                    ex.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // ignore
        }
    }

    override fun supportFragmentInjector() = dispatchingAndroidInjector

    override fun lockUi() {
        binding?.lockUiLayout?.visibility = View.VISIBLE
        binding?.lockUiLayout?.setOnClickListener {
            // ignore
        }
    }

    override fun unlockUi() {
        binding?.lockUiLayout?.visibility = View.GONE
    }


}
