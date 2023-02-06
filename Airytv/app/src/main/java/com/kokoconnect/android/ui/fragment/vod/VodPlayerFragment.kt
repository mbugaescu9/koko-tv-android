package com.kokoconnect.android.ui.fragment.vod

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.*
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.model.ads.interstitial.InterstitialTrigger
import com.kokoconnect.android.model.player.VideoOpeningReason
import com.kokoconnect.android.model.ads.video.VideoAdTrigger
import com.kokoconnect.android.model.error.PlayerError
import com.kokoconnect.android.model.error.PlayerErrorType
import com.kokoconnect.android.model.player.PlayerType
import com.kokoconnect.android.model.player.proxy.*
import com.kokoconnect.android.ui.view.DailymotionPlayerWebView
import com.kokoconnect.android.util.ActivityUtils
import com.kokoconnect.android.util.DeviceUtils
import com.kokoconnect.android.util.IntentUtils
import com.kokoconnect.android.util.isOrientationLandscape
import com.kokoconnect.android.vm.*
import com.kokoconnect.android.vm.vod.VodContentViewModel
import com.kokoconnect.android.vm.vod.VodPlayersViewModel
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.*
import com.google.android.exoplayer2.ui.PlayerView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt


class VodPlayerFragment : Fragment(), Injectable {
    companion object {
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val adsViewModel: AdsViewModel by activityViewModels { viewModelFactory }
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val contentViewModel: VodContentViewModel by activityViewModels { viewModelFactory }
    private val vodPlayersViewModel: VodPlayersViewModel by activityViewModels { viewModelFactory }

    private val touchInterceptorListener = object : View.OnTouchListener {
        override fun onTouch(view: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    contentViewModel.requestFullscreenControls()
                }
            }
            return false
        }
    }

    private var binding: FragmentVodPlayerBinding? = null
    private val youtubePlayerView: YouTubePlayerView?
        get() {
            return binding?.youtubePlayerContainer?.findViewById<YouTubePlayerView>(R.id.youtubePlayerView)
        }
    private val dailymotionPlayerView: DailymotionPlayerWebView?
        get() {
            return binding?.dailymotionPlayerContainer?.findViewById<DailymotionPlayerWebView>(R.id.dailymotionPlayerView)
        }
    private val webPlayerView: WebView?
        get() {
            return binding?.webPlayerContainer?.findViewById<WebView>(R.id.webPlayerView)
        }
    private val webPlayerFullscreenContainer: FrameLayout?
        get() {
            return binding?.webPlayerContainer?.findViewById<FrameLayout>(R.id.webPlayerFullscreenContainer)
        }
    private val exoplayerPlayerView: PlayerView?
        get() {
            return binding?.exoPlayerContainer?.findViewById<PlayerView>(R.id.exoplayerPlayerView)
        }
    private val exoplayerAdsPlayerView: PlayerView?
        get() {
            return binding?.exoPlayerAdsContainer?.findViewById<PlayerView>(R.id.exoplayerAdsPlayerView)
        }
    private val flAdNumberView: FrameLayout?
        get() {
            return binding?.exoPlayerAdsContainer?.findViewById<FrameLayout>(R.id.flAdNumber)
        }
    private val tvAdNumberView: TextView?
        get() {
            return binding?.exoPlayerAdsContainer?.findViewById<TextView>(R.id.tvAdNumber)
        }
    private val playerUi: PopupVodPlayerButtonsBinding?
        get() {
            return binding?.popupPlayerButtons
        }
    private val fullscreenPlayerUi: PopupVodPlayerButtonsFullscreenBinding?
        get() {
            return binding?.popupPlayerButtonsFullscreen
        }
    private val playerMessageUi: PopupPlayerMessageBinding?
        get() {
            return binding?.popupPlayerMessage
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val playerFragmentBinding = try {
            FragmentVodPlayerBinding.inflate(inflater, container, false)
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            return null
        }
        val youtubePlayerView = try {
            inflater.inflate(R.layout.include_youtube_player, container, false)
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            null
        }
        val exoplayerPlayerView = try {
            inflater.inflate(R.layout.include_exoplayer_player, container, false)
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            null
        }
        val dailymotionPlayerView = try {
            inflater.inflate(R.layout.include_dailymotion_player, container, false)
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            null
        }
        val webPlayerView = try {
            inflater.inflate(R.layout.include_web_player, container, false)
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            null
        }
        val exoplayerAdsPlayerView = try {
            inflater.inflate(R.layout.include_exoplayer_ads_player, container, false)
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            null
        }
        youtubePlayerView?.let { playerFragmentBinding.youtubePlayerContainer.addView(it) }
        exoplayerPlayerView?.let { playerFragmentBinding.exoPlayerContainer.addView(it) }
        dailymotionPlayerView?.let { playerFragmentBinding.dailymotionPlayerContainer.addView(it) }
        webPlayerView?.let { playerFragmentBinding.webPlayerContainer.addView(it) }
        exoplayerAdsPlayerView?.let { playerFragmentBinding.exoPlayerAdsContainer.addView(it) }
        binding = playerFragmentBinding
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        vodPlayersViewModel.onCurrentAdNumber.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                flAdNumberView?.visibility = View.VISIBLE
                tvAdNumberView?.text = getString(
                    R.string.player_ad_number,
                    it.currentAdNumber.toString(),
                    it.adsCount.toString()
                )
            } else {
                flAdNumberView?.visibility = View.GONE
            }
        })
        vodPlayersViewModel.onAdNotLoaded.observe(viewLifecycleOwner, Observer {
            it?.let { reason ->
                if (reason.type == VideoAdTrigger.Type.OnCueTones) {
                    adsViewModel.needShowInterstitial.postValue(InterstitialTrigger.OnVideoAdError)
                }
                vodPlayersViewModel.onAdNotLoaded.postValue(null)
            }
        })
        adsViewModel.adsStatus.observe(viewLifecycleOwner, Observer {
            if (it?.enabled == true) {
                vodPlayersViewModel.setAds(adsViewModel.getStatus())
            }
            vodPlayersViewModel.initVideoAd(activity, viewLifecycleOwner, contentViewModel)
        })
        contentViewModel.needOpenContent.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                val playerObject = it.getPlayerObject()
                Timber.d("needOpenVideo.observe() ${playerObject} ${playerObject?.getUrl()}")
                vodPlayersViewModel.openContent(
                    playerObject,
                    VideoOpeningReason.ON_VOD_CONTENT_SWITCH,
                    it
                )
                contentViewModel.needOpenContent.postValue(null)
            }
        })
        contentViewModel.needShowFullscreenDescription.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showControls(it)
                contentViewModel.needShowFullscreenDescription.value = null
            }
        })
        playerViewModel.isVolumeExists.observe(viewLifecycleOwner, Observer {
            playerUi?.apply {
                if (it == true) {
                    playerUi?.btnMute?.setImageResource(R.drawable.ic_volume_up_black_24dp)
                    fullscreenPlayerUi?.btnMute?.setImageResource(R.drawable.ic_volume_up_black_24dp)
                } else {
                    playerUi?.btnMute?.setImageResource(R.drawable.ic_volume_off_black_24dp)
                    fullscreenPlayerUi?.btnMute?.setImageResource(R.drawable.ic_volume_off_black_24dp)
                }
            }
        })

        vodPlayersViewModel.subtitlesEnabled.observe(viewLifecycleOwner, Observer {
            setSubtitlesButtonEnabled(it == true)
        })

        vodPlayersViewModel.subtitlesExists.observe(viewLifecycleOwner, Observer {
            setSubtitlesButtonVisible(it == true)
        })


        playerViewModel.isFullscreenLiveData.observe(viewLifecycleOwner, Observer {
            playerUi?.apply {
                if (playerViewModel.isFullscreen) {
                    playerUi?.btnFullscreen?.setImageResource(R.drawable.ic_fullscreen_exit_black_24dp)
                    fullscreenPlayerUi?.btnFullscreen?.setImageResource(R.drawable.ic_fullscreen_exit_black_24dp)
                } else {
                    playerUi?.btnFullscreen?.setImageResource(R.drawable.ic_fullscreen_black_24dp)
                    fullscreenPlayerUi?.btnFullscreen?.setImageResource(R.drawable.ic_fullscreen_black_24dp)
                }
            }
        })

        setupViews()

        vodPlayersViewModel.needSwitchPlayer.observe(viewLifecycleOwner, Observer {
            when (it) {
                PlayerType.YOUTUBE, PlayerType.DAILYMOTION, PlayerType.EXOPLAYER, PlayerType.WEB, PlayerType.AD -> {
                    Timber.d("onSwitchPlayer() ${it.name}")
                    switchOnPlayer(it)
                    vodPlayersViewModel.needSwitchPlayer.postValue(PlayerType.EMPTY)
                }
                else -> {
                    // ignore
                }
            }
        })

        vodPlayersViewModel.needOpenNextContent.observe(viewLifecycleOwner, Observer { isNeed ->
            if (isNeed != null && isNeed == true) {
                // open next video
                vodPlayersViewModel.needOpenNextContent.postValue(false)
            }
        })

        vodPlayersViewModel.needReopenCurrentContent.observe(
            viewLifecycleOwner,
            Observer { isNeed ->
                if (isNeed != null && isNeed == true) {
                    contentViewModel.refresh()
                    vodPlayersViewModel.needReopenCurrentContent.postValue(false)
                }
            })

        vodPlayersViewModel.needShowError.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showMessage(it)
            }
        })
        vodPlayersViewModel.isChromecastConnected.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                val isHide = it
                binding?.root?.visibility = if (isHide) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    View.GONE
                } else {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                    View.VISIBLE
                }
            }
        })

        vodPlayersViewModel.needShowError.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showMessage(it)
            }
        })

        vodPlayersViewModel.needShowBufferingProgress.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showBufferingProgress(it)
            }
        })

        vodPlayersViewModel.setupPlayers(
            YouTubeProxy.Params(
                youtubePlayerView,
                true
            ),
            ExoPlayerProxy.Params(
                exoplayerPlayerView,
                true
            ),
            DailymotionProxy.Params(
                dailymotionPlayerView,
                true
            ),
            WebPlayerProxy.Params(
                webPlayerView,
                webPlayerFullscreenContainer,
                true
            ),
            AdsProxy.Params(
                exoplayerAdsPlayerView,
                true
            ),
            lifecycle
        )
        lifecycle.addObserver(vodPlayersViewModel)
        // https://github.com/PierfrancescoSoffritti/android-youtube-player/issues/107
        youtubePlayerView?.let {
            lifecycle.addObserver(it)
        }
    }

    private fun setupViews() {
        playerUi?.apply {
            clickInterceptor.setOnTouchListener(touchInterceptorListener)
            btnFullscreen.setOnClickListener {
                playerViewModel.switchFullscreen()
            }
            btnMute.setOnClickListener {
                playerViewModel.switchAudioMode()
            }
            btnSubtitles.setOnClickListener {
                vodPlayersViewModel.switchSubtitles()
            }
            btnRefresh.setOnClickListener {
                contentViewModel.refresh()
            }
        }
        fullscreenPlayerUi?.apply {
            clickInterceptor.setOnTouchListener(touchInterceptorListener)
            btnFullscreen.setOnClickListener {
                playerViewModel.switchFullscreen()
            }
            btnMute.setOnClickListener {
                playerViewModel.switchAudioMode()
            }
            btnSubtitles.setOnClickListener {
                vodPlayersViewModel.switchSubtitles()
            }
            btnRefresh.setOnClickListener {
                contentViewModel.refresh()
            }
        }
        showControls(true)
        updateOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation()
    }

    private fun updateOrientation(isLandscape: Boolean = resources.configuration.isOrientationLandscape()) {
        showFullscreenControls(isLandscape)
        setupPlayersLayout(isLandscape)
        binding?.vMarginControls?.isVisible = !isLandscape
    }

    private fun setupPlayersLayout(isLandscape: Boolean) {
        val context = context ?: return
        val displaySize = DeviceUtils.getDisplaySize(context)
        if (isLandscape) {
            exoplayerPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            exoplayerPlayerView?.resizeMode = RESIZE_MODE_FIT
            exoplayerAdsPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            exoplayerAdsPlayerView?.resizeMode = RESIZE_MODE_FIT
            youtubePlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            dailymotionPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            webPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        } else {
            val height = (DeviceUtils.getDisplaySize(context).y * 0.5).roundToInt()
            val minimumHeight = 180.dp
            exoplayerPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            exoplayerPlayerView?.resizeMode = RESIZE_MODE_FIXED_WIDTH
            exoplayerPlayerView?.minimumHeight = minimumHeight
            exoplayerAdsPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            exoplayerAdsPlayerView?.resizeMode = RESIZE_MODE_FIXED_WIDTH
            exoplayerAdsPlayerView?.minimumHeight = minimumHeight
            youtubePlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            youtubePlayerView?.minimumHeight = minimumHeight
            dailymotionPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            dailymotionPlayerView?.minimumHeight = minimumHeight
            webPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                height
            )
            webPlayerView?.minimumHeight = minimumHeight
        }
    }

    private fun showFullscreenControls(isVisible: Boolean) {
        if (isVisible) {
            fullscreenPlayerUi?.root?.isVisible = true
            playerUi?.root?.isVisible = false
        } else {
            fullscreenPlayerUi?.root?.isVisible = false
            playerUi?.root?.isVisible = true
        }
    }

    private fun showControls(needShow: Boolean) {
        val isLandscape = resources.configuration.isOrientationLandscape()
        val isVisible = fullscreenPlayerUi?.containerControls?.isVisible == true
        if (needShow) {
            if (!isVisible) {
                fullscreenPlayerUi?.containerControls?.isVisible = true
                val animation = AlphaAnimation(0f, 1f)
                animation.duration = ActivityUtils.ANIMATION_DURATION_SHORT
                animation.fillAfter = true
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(p0: Animation?) {}
                    override fun onAnimationEnd(p0: Animation?) {}
                    override fun onAnimationStart(p0: Animation?) {}
                })
                fullscreenPlayerUi?.containerControls?.startAnimation(animation)
            } else {
                fullscreenPlayerUi?.containerControls?.isVisible = true
            }
        } else if (isLandscape) {
            if (isVisible) {
                val animation = AlphaAnimation(1f, 0f)
                animation.duration = ActivityUtils.ANIMATION_DURATION_SHORT
                animation.fillAfter = true
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(p0: Animation?) {}
                    override fun onAnimationEnd(p0: Animation?) {
                        fullscreenPlayerUi?.containerControls?.isVisible = false
                    }

                    override fun onAnimationStart(p0: Animation?) {}
                })
                fullscreenPlayerUi?.containerControls?.startAnimation(animation)
            } else {
                fullscreenPlayerUi?.containerControls?.isVisible = false
            }
        }
    }

    private fun setSubtitlesButtonEnabled(enabled: Boolean) {
        if (enabled) {
            playerUi?.btnSubtitles?.setImageResource(R.drawable.ic_closed_capture)
        } else {
            playerUi?.btnSubtitles?.setImageResource(R.drawable.ic_closed_capture_disabled)
        }
    }

    private fun setSubtitlesButtonVisible(visible: Boolean) {
        if (visible) {
            playerUi?.btnSubtitles?.visibility = View.VISIBLE
        } else {
            playerUi?.btnSubtitles?.visibility = View.GONE
        }
    }

    private fun showBufferingProgress(enabled: Boolean) {
        playerUi?.pbBuffering?.isVisible = enabled
        fullscreenPlayerUi?.pbBuffering?.isVisible = enabled
    }

    private fun switchOnPlayer(playerType: PlayerType) {
        val isYoutube = playerType == PlayerType.YOUTUBE
        val isExoplayer = playerType == PlayerType.EXOPLAYER
        val isDailymotion = playerType == PlayerType.DAILYMOTION
        val isWeb = playerType == PlayerType.WEB
        val isAds = playerType == PlayerType.AD

        binding?.webPlayerContainer?.visibility = if (isWeb) View.VISIBLE else View.GONE
        webPlayerView?.visibility = if (isWeb) View.VISIBLE else View.GONE

        binding?.youtubePlayerContainer?.visibility = if (isYoutube) View.VISIBLE else View.GONE
        youtubePlayerView?.visibility = if (isYoutube) View.VISIBLE else View.GONE

        binding?.dailymotionPlayerContainer?.visibility =
            if (isDailymotion) View.VISIBLE else View.GONE
        dailymotionPlayerView?.visibility = if (isDailymotion) View.VISIBLE else View.GONE

        binding?.exoPlayerContainer?.visibility = if (isExoplayer) View.VISIBLE else View.GONE
        exoplayerPlayerView?.visibility = if (isExoplayer) View.VISIBLE else View.GONE

        binding?.exoPlayerAdsContainer?.visibility = if (isAds) View.VISIBLE else View.GONE
        exoplayerAdsPlayerView?.visibility = if (isAds) View.VISIBLE else View.GONE

//        val clickInterceptorEnabled =
//            isExoplayer && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
//        playerUi?.clickInterceptor?.visibility =
//            if (clickInterceptorEnabled) View.VISIBLE else View.GONE
    }

    private fun showMessage(error: PlayerError?) {
        val ctx = context ?: return
        Timber.d("showMessage() error ${error?.type}")
        when (error?.type) {
            PlayerErrorType.PRIVATE_ARCHIVE_CONTENT -> {
                playerMessageUi?.messageText?.setText(ctx.getString(R.string.player_error_private_unavailable))
                playerMessageUi?.messageButton?.visibility = View.GONE
                playerMessageUi?.root?.visibility = View.VISIBLE
            }
            PlayerErrorType.YOUTUBE_PLAYER_UNAVAILABLE -> {
                playerMessageUi?.messageText?.setText(R.string.player_error_youtube_unavailable)
                playerMessageUi?.messageButton?.visibility = View.GONE
                playerMessageUi?.root?.visibility = View.VISIBLE
            }
            PlayerErrorType.DAILYMOTION_PLAYER_UNAVAILABLE -> {
                playerMessageUi?.messageText?.setText(R.string.player_error_dailymotion_unavailable)
                playerMessageUi?.messageButton?.visibility = View.GONE
                playerMessageUi?.root?.visibility = View.VISIBLE
            }
            PlayerErrorType.EXOPLAYER_PLAYER_UNAVAILABLE -> {
                playerMessageUi?.messageText?.setText(R.string.player_error_exoplayer_unavailable)
                playerMessageUi?.messageButton?.visibility = View.GONE
                playerMessageUi?.root?.visibility = View.VISIBLE
            }
            else -> {
                playerMessageUi?.messageText?.setText("")
                playerMessageUi?.messageButton?.setText("")
                playerMessageUi?.root?.visibility = View.GONE
            }
        }
    }

    private fun openCastSettings() {
        try {
            startActivity(IntentUtils.getCastSettingsIntent())
        } catch (exception: Exception) {
            Toast.makeText(
                context,
                getString(R.string.device_not_support_casting),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val bundle = Bundle()
        webPlayerView?.saveState(bundle)
        outState.putBundle("webViewState", bundle)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        webPlayerFullscreenContainer?.removeAllViews()
        webPlayerView?.destroy()
    }
}
