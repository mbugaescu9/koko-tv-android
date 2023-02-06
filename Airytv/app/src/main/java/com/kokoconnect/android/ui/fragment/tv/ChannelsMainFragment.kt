package com.kokoconnect.android.ui.fragment.tv

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.kokoconnect.android.databinding.FragmentMainChannelsBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.ui.activity.BaseActivity
import com.kokoconnect.android.ui.fragment.BaseFragment
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.util.isOrientationLandscape
import com.kokoconnect.android.vm.AdsViewModel
import com.kokoconnect.android.vm.AmsEventsViewModel
import com.kokoconnect.android.vm.tv.TvGuideViewModel
import com.kokoconnect.android.vm.PlayerViewModel
import com.kokoconnect.android.vm.NavigationViewModel
import com.google.android.gms.cast.framework.CastButtonFactory
import javax.inject.Inject

class ChannelsMainFragment : BaseFragment(), Injectable {
    companion object {
        val tabId = 0
        val screen = Screen().apply {
            this.name = ScreenType.TV.defaultName
            this.type = ScreenType.TV
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    val playerViewModel: PlayerViewModel by activityViewModels { viewModelFactory }
    val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }
    val eventsViewModel: AmsEventsViewModel by activityViewModels { viewModelFactory }
    val tvGuideViewModel: TvGuideViewModel by activityViewModels { viewModelFactory }
    val adsViewModel: AdsViewModel by activityViewModels { viewModelFactory }

    private var binding: FragmentMainChannelsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainChannelsBinding.inflate(inflater)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: return

        setupViews()

        playerViewModel.isFullscreenLiveData.observe(viewLifecycleOwner, Observer {
            onFullscreen(playerViewModel.isFullscreen)
        })

        adsViewModel.adsStatus.observe(viewLifecycleOwner, Observer {
            initBanner()
        })
    }

    private fun setupViews() {
        val chromecastActivity = activity as? BaseActivity
        val btnMediaRoute = binding?.btnMediaRoute
        if (AppParams.isChromecastTvEnabled) {
            btnMediaRoute?.visibility = View.VISIBLE
            if (chromecastActivity != null && btnMediaRoute != null) {
                CastButtonFactory.setUpMediaRouteButton(chromecastActivity, btnMediaRoute)
            }
        } else {
            btnMediaRoute?.visibility = View.INVISIBLE
        }
        updateOrientation()
    }

    override fun onResume() {
        super.onResume()
        navigationViewModel.setCurrentScreen(screen, this)
        updateOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation()
    }

    private fun updateOrientation(isLandscape: Boolean = resources.configuration.isOrientationLandscape()) {
        val isFullscreen = playerViewModel.isFullscreen
        onFullscreen(isFullscreen)
        binding?.appbar?.isVisible = !isLandscape && !isFullscreen
        binding?.flGuideContainer?.isVisible = !isLandscape && !isFullscreen
        binding?.flGuideFullscreenContainer?.isVisible = isLandscape && !isFullscreen
        binding?.frameFullscreenDescription?.isVisible = isLandscape && !isFullscreen
        val guideFragment = binding?.guideFragment
        val guideFragmentParent = (guideFragment?.parent as? ViewGroup)
        if (guideFragment != null && guideFragmentParent != null) {
            guideFragmentParent?.removeView(guideFragment)
            if (isLandscape) {
                binding?.flGuideFullscreenContainer?.addView(guideFragment)
            } else {
                binding?.flGuideSecondContainer?.addView(guideFragment)
            }
        }
        if (isLandscape) {
            binding?.playerFragment?.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        } else {
            binding?.playerFragment?.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        initBanner()
    }

    private fun onFullscreen(isFullscreen: Boolean) {
        if (isFullscreen) {
            setUiVisible(false)
        } else {
            setUiVisible(true)
        }
    }

    private fun setUiVisible(isVisible: Boolean) {
        if (isVisible) {
            binding?.frameDescription?.visibility = View.VISIBLE
            binding?.frameFullscreenDescription?.visibility = View.VISIBLE
            binding?.flGuideContainer?.visibility = View.VISIBLE
            binding?.flGuideFullscreenContainer?.visibility = View.VISIBLE
        } else {
            binding?.frameDescription?.visibility = View.GONE
            binding?.frameFullscreenDescription?.visibility = View.GONE
            binding?.flGuideContainer?.visibility = View.GONE
            binding?.flGuideFullscreenContainer?.visibility = View.GONE
        }
    }

    private fun initBanner() {
        val activity = activity ?: return
        val container = binding?.bannerContainer ?: return
        val content = tvGuideViewModel.needOpenProgram
        val isEnabled = adsViewModel.getStatus()?.enabled == true
        if (isEnabled
            && resources.configuration.isOrientationLandscape()
            && !playerViewModel.isFullscreen
        ) {
            adsViewModel.loadBannerInContainer(
                activity,
                eventsViewModel,
                container,
                content
            )
        }
    }
}