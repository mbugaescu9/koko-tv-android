package com.kokoconnect.android.ui.fragment.tv

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.FragmentPlayerBinding
import com.kokoconnect.android.databinding.PopupPlayerButtonsBinding
import com.kokoconnect.android.databinding.PopupPlayerButtonsFullscreenBinding
import com.kokoconnect.android.databinding.PopupPlayerMessageBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.model.ads.interstitial.InterstitialTrigger
import com.kokoconnect.android.model.ads.video.VideoAdTrigger
import com.kokoconnect.android.model.error.PlayerError
import com.kokoconnect.android.model.error.PlayerErrorType
import com.kokoconnect.android.model.player.PlayerType
import com.kokoconnect.android.model.player.proxy.*
import com.kokoconnect.android.ui.fragment.vod.dp
import com.kokoconnect.android.ui.view.DailymotionPlayerWebView
import com.kokoconnect.android.ui.view.InterceptTouchFrameLayout
import com.kokoconnect.android.util.DeviceUtils
import com.kokoconnect.android.util.isOrientationLandscape
import com.kokoconnect.android.vm.*
import com.kokoconnect.android.vm.freegift.GiveawaysViewModel
import com.kokoconnect.android.vm.tv.TvGuideViewModel
import com.kokoconnect.android.vm.tv.TvPlayersViewModel
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt


class PlayerFragment : Fragment(), Injectable {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val adsViewModel: AdsViewModel by activityViewModels { viewModelFactory }
    private val eventsModel: AmsEventsViewModel by activityViewModels { viewModelFactory }
    private val playerViewModel: PlayerViewModel by activityViewModels { viewModelFactory }
    private val tvPlayersViewModel: TvPlayersViewModel by activityViewModels { viewModelFactory }
    private val tvGuideViewModel: TvGuideViewModel by activityViewModels { viewModelFactory }
    private val giveawaysViewModel: GiveawaysViewModel by activityViewModels { viewModelFactory }

    private val touchInterceptListener = object :
        InterceptTouchFrameLayout.OnInterceptTouchEventListener {
        override fun onInterceptTouchEvent(
            view: InterceptTouchFrameLayout?,
            ev: MotionEvent?,
            disallowIntercept: Boolean
        ): Boolean {
            return false
        }

        override fun onTouchEvent(
            view: InterceptTouchFrameLayout?,
            event: MotionEvent?
        ): Boolean {
            if (event?.actionMasked == MotionEvent.ACTION_DOWN) {
                switchPlayerPopupButtons()
            }
            return false
        }
    }


    private var binding: FragmentPlayerBinding? = null
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
            return null//binding?.dailymotionPlayerContainer?.findViewById<WebView>(R.id.webPlayerView)
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
    private val playerUi: PopupPlayerButtonsBinding?
        get() {
            return binding?.popupPlayerButtons
        }
    private val fullscreenPlayerUi: PopupPlayerButtonsFullscreenBinding?
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
            FragmentPlayerBinding.inflate(inflater, container, false)
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
        val exoplayerAdsPlayerView = try {
            inflater.inflate(R.layout.include_exoplayer_ads_player, container, false)
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            null
        }
        youtubePlayerView?.let { playerFragmentBinding.youtubePlayerContainer.addView(it) }
        exoplayerPlayerView?.let { playerFragmentBinding.exoPlayerContainer.addView(it) }
        dailymotionPlayerView?.let { playerFragmentBinding.dailymotionPlayerContainer.addView(it) }
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

        tvPlayersViewModel.onCurrentAdNumber.observe(viewLifecycleOwner, Observer {
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
        tvPlayersViewModel.onAdNotLoaded.observe(viewLifecycleOwner, Observer {
            it?.let { reason ->
                if (reason.type == VideoAdTrigger.Type.OnCueTones) {
                    adsViewModel.needShowInterstitial.postValue(InterstitialTrigger.OnVideoAdError)
                }
                tvPlayersViewModel.onAdNotLoaded.postValue(null)
            }
        })
        adsViewModel.adsStatus.observe(viewLifecycleOwner, Observer {
            if (it?.enabled == true) {
                tvPlayersViewModel.setAds(it)
            }
            tvPlayersViewModel.initVideoAd(activity, viewLifecycleOwner, tvGuideViewModel)
        })

        tvGuideViewModel.needOpenProgram.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                Timber.d("PlayerFragment: needOpenVideo.observe() ${it} ${it.video?.getUrl()}")
                Timber.d("PlayerFragment: needOpenVideo.observe() currentAds")
                Timber.d("PlayerFragment: needOpenVideo.observe() videoOpeningReason == ${it.videoOpeningReason}")
                val reason = it.videoOpeningReason ?: return@Observer
                Timber.d("Who called setPlayerObject: guideViewModel.needOpenProgram.observe")
                tvPlayersViewModel.openContent(it.video, reason, it)
                tvPlayersViewModel.setProgramAds(tvGuideViewModel.getCurrentImaProgramAds())
                tvGuideViewModel.needOpenProgram.postValue(null)
            }
        })
        playerViewModel.isVolumeExists.observe(viewLifecycleOwner, Observer {
            playerUi?.apply {
                if (it == true) {
                    playerUi?.buttonMute?.setImageResource(R.drawable.ic_volume_up_black_24dp)
                    fullscreenPlayerUi?.buttonMute?.setImageResource(R.drawable.ic_volume_up_black_24dp)
                } else {
                    playerUi?.buttonMute?.setImageResource(R.drawable.ic_volume_off_black_24dp)
                    fullscreenPlayerUi?.buttonMute?.setImageResource(R.drawable.ic_volume_off_black_24dp)
                }
            }
        })

        tvPlayersViewModel.subtitlesEnabled.observe(viewLifecycleOwner, Observer {
            setSubtitlesButtonEnabled(it == true)
        })

        tvPlayersViewModel.subtitlesExists.observe(viewLifecycleOwner, Observer {
            setSubtitlesButtonVisible(it == true)
        })


        playerViewModel.isFullscreenLiveData.observe(viewLifecycleOwner, Observer {
            playerUi?.apply {
                if (playerViewModel.isFullscreen) {
                    fullscreenPlayerUi?.buttonSwitchMode?.isVisible = false
                    playerUi?.buttonFullscreen?.setImageResource(R.drawable.ic_fullscreen_exit_black_24dp)
                    fullscreenPlayerUi?.buttonFullscreen?.setImageResource(R.drawable.ic_fullscreen_exit_black_24dp)
                    tvGuideViewModel.setPopupDescriptionVisible(false)
                    view?.postInvalidate()
                } else {
                    fullscreenPlayerUi?.buttonSwitchMode?.isVisible = true
                    playerUi?.buttonFullscreen?.setImageResource(R.drawable.ic_fullscreen_black_24dp)
                    fullscreenPlayerUi?.buttonFullscreen?.setImageResource(R.drawable.ic_fullscreen_black_24dp)
                    view?.postInvalidate()
                }
            }
        })

        setupViews()

        tvPlayersViewModel.needSwitchPlayer.observe(viewLifecycleOwner, Observer {
            when (it) {
                PlayerType.YOUTUBE, PlayerType.DAILYMOTION, PlayerType.EXOPLAYER, PlayerType.WEB, PlayerType.AD -> {
                    Timber.d("onSwitchPlayer() ${it.name}")
                    switchOnPlayer(it)
                    tvPlayersViewModel.needSwitchPlayer.postValue(PlayerType.EMPTY)
                }
                else -> {
                    // ignore
                }
            }
        })

        tvPlayersViewModel.needOpenNextContent.observe(viewLifecycleOwner, Observer { isNeed ->
            if (isNeed != null && isNeed == true) {
                tvGuideViewModel.getNextChannelVideo()
                tvPlayersViewModel.needOpenNextContent.postValue(false)
            }
        })

        tvPlayersViewModel.needReopenCurrentContent.observe(
            viewLifecycleOwner,
            Observer { isNeed ->
                if (isNeed != null && isNeed == true) {
                    tvGuideViewModel.getCurrentChannelVideo()
                    tvPlayersViewModel.needReopenCurrentContent.postValue(false)
                }
            })

        tvPlayersViewModel.needShowError.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showMessage(it)
            }
        })

        tvPlayersViewModel.isChromecastConnected.observe(viewLifecycleOwner, Observer {
            it?.let { isConnected ->
                binding?.root?.visibility = if (isConnected) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    View.GONE
                } else {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    View.VISIBLE
                }
            }
        })

        tvPlayersViewModel.needShowBufferingProgress.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showBufferingProgress(it)
            }
        })

        tvPlayersViewModel.setupPlayers(
            YouTubeProxy.Params(
                youtubePlayerView,
                true
            ),
            ExoPlayerProxy.Params(
                exoplayerPlayerView,
                false
            ),
            DailymotionProxy.Params(
                dailymotionPlayerView,
                false
            ),
            WebPlayerProxy.Params(
                webPlayerView,
                null,
                false
            ),
            AdsProxy.Params(
                exoplayerAdsPlayerView,
                false
            ),
            lifecycle
        )
        lifecycle.addObserver(tvPlayersViewModel)
        // https://github.com/PierfrancescoSoffritti/android-youtube-player/issues/107
        youtubePlayerView?.let {
            lifecycle.addObserver(it)
        }
    }

    private fun setupViews() {
        playerUi?.apply {
            clickInterceptor?.setOnInterceptTouchEventListener(touchInterceptListener)
            buttonFullscreen?.setOnClickListener {
                playerViewModel.switchFullscreen()
            }
            buttonMute?.setOnClickListener {
                playerViewModel.switchAudioMode()
            }
            buttonSubtitles?.setOnClickListener {
                tvPlayersViewModel.switchSubtitles()
            }
        }
        fullscreenPlayerUi?.apply {
            buttonSwitchMode?.setOnClickListener {
                tvGuideViewModel.switchPopupDescription()
            }
            buttonFullscreen?.setOnClickListener {
                playerViewModel.switchFullscreen()
            }
            buttonMute?.setOnClickListener {
                playerViewModel.switchAudioMode()
            }
            buttonSubtitles?.setOnClickListener {
                tvPlayersViewModel.switchSubtitles()
            }
            CastButtonFactory.setUpMediaRouteButton(
                /*activity*/ requireContext(),
                btnMediaRoute
            )
        }
        updateOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation()
    }

    private fun updateOrientation(isLandscape: Boolean = resources.configuration.isOrientationLandscape()) {
        setupPlayersLayout(isLandscape)
        showFullscreenControls(isLandscape)
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

    private fun setupPlayersLayout(isLandscape: Boolean) {
        val context = context ?: return
        if (isLandscape) {
            exoplayerPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            exoplayerPlayerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            exoplayerAdsPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            exoplayerAdsPlayerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
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
            exoplayerPlayerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            exoplayerPlayerView?.minimumHeight = minimumHeight
            exoplayerAdsPlayerView?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            exoplayerAdsPlayerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
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

    private fun switchPlayerPopupButtons() {
        if (playerUi?.overlayLayout?.visibility == View.VISIBLE) {
            playerUi?.overlayLayout?.visibility = View.INVISIBLE
            tvGuideViewModel.setPopupDescriptionVisible(false)
        } else {
            playerUi?.overlayLayout?.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        tvGuideViewModel.setPaused(true)
    }

    override fun onResume() {
        super.onResume()
        tvGuideViewModel.setPaused(false)
        if (playerViewModel.isFullscreen) {
            fullscreenPlayerUi?.buttonSwitchMode?.visibility = View.GONE
            fullscreenPlayerUi?.buttonFullscreen?.setImageResource(R.drawable.ic_fullscreen_exit_black_24dp)
        }
    }

    private fun setSubtitlesButtonEnabled(enabled: Boolean) {
        if (enabled) {
            playerUi?.buttonSubtitles?.setImageResource(R.drawable.ic_closed_capture)
        } else {
            playerUi?.buttonSubtitles?.setImageResource(R.drawable.ic_closed_capture_disabled)
        }
    }

    private fun setSubtitlesButtonVisible(visible: Boolean) {
        if (visible) {
            playerUi?.buttonSubtitles?.visibility = View.VISIBLE
        } else {
            playerUi?.buttonSubtitles?.visibility = View.GONE
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
        val isAds = playerType == PlayerType.AD
        val isWeb = playerType == PlayerType.WEB

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

        playerUi?.overlayLayout?.visibility = if (isExoplayer) View.VISIBLE else View.GONE
        val clickInterceptorEnabled =
            isExoplayer && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        playerUi?.clickInterceptor?.visibility =
            if (clickInterceptorEnabled) View.VISIBLE else View.GONE
    }

    private fun showMessage(error: PlayerError?) {
        val ctx = context ?: return
        Timber.d("showMessage() error ${error?.type}")
        when (error?.type) {
            PlayerErrorType.PRIVATE_ARCHIVE_CONTENT -> {
                playerMessageUi?.messageText?.setText(ctx.getString(R.string.player_error_private_unavailable))
                playerMessageUi?.messageButton?.setText(ctx.getString(R.string.player_error_private_unavailable_button))
                playerMessageUi?.messageButton?.setOnClickListener {
                    tvGuideViewModel.openPreviousChannel()
                }
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
            PlayerErrorType.EXOPLAYER_PLAYER_STATE_IDLE -> {
                playerMessageUi?.messageText?.setText(R.string.error_video_idle)
                playerMessageUi?.root?.visibility = View.VISIBLE
                playerMessageUi?.messageButton?.apply {
                    visibility = View.VISIBLE
                    setText(ctx.getString(R.string.player_error_private_unavailable_button))
                    setOnClickListener {
                        tvGuideViewModel.openPreviousChannel()
                    }
                }
            }
            PlayerErrorType.NO_INTERNET_CONNECTION -> {
                playerMessageUi?.messageText?.setText(R.string.error_network_problem)
                playerMessageUi?.messageButton?.visibility = View.GONE
                playerMessageUi?.root?.visibility = View.VISIBLE
                playerMessageUi?.messageButton?.apply {
                    visibility = View.VISIBLE
                    setText(ctx.getString(R.string.player_error_no_internet_connection_button))
                    setOnClickListener {
                        tvGuideViewModel.apply {
                            val channel = getChannel() ?: return@setOnClickListener
                            openChannel(
                                channel,
                                adsViewModel,
                                true
                            )
                        }
                    }
                }
            }
            else -> {
                playerMessageUi?.messageText?.setText("")
                playerMessageUi?.messageButton?.setText("")
                playerMessageUi?.root?.visibility = View.GONE
            }
        }
    }
}
