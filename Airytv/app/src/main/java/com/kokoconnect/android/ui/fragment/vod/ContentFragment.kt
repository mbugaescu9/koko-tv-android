package com.kokoconnect.android.ui.fragment.vod

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.FragmentContentBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.model.vod.Content
import com.kokoconnect.android.model.vod.Episode
import com.kokoconnect.android.model.vod.Movie
import com.kokoconnect.android.model.vod.Series
import com.kokoconnect.android.util.*
import com.kokoconnect.android.vm.AmsEventsViewModel
import com.kokoconnect.android.vm.NavigationViewModel
import com.kokoconnect.android.vm.PlayerViewModel
import com.kokoconnect.android.vm.vod.SeriesViewModel
import com.kokoconnect.android.vm.vod.VodContentViewModel
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max


class ContentFragment : Fragment(), Injectable {
    companion object {
        val screen = Screen().apply {
            this.name = ScreenType.CONTENT.defaultName
            this.type = ScreenType.CONTENT
        }

        private val EPISODES_LIST_MARGIN_TOP = 450.dp
        private val EPISODES_LIST_MIN_HEIGHT = 300.dp
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private var episodesListHeight = EPISODES_LIST_MIN_HEIGHT
    private var episodesButtonMarginEnable = false

    val seriesViewModel: SeriesViewModel by activityViewModels { viewModelFactory }
    val contentViewModel: VodContentViewModel by activityViewModels { viewModelFactory }
    val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }
    val eventsViewModel: AmsEventsViewModel by activityViewModels { viewModelFactory }
    val playerViewModel: PlayerViewModel by activityViewModels { viewModelFactory }

    private var binding: FragmentContentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        contentViewModel.needShowContent.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                contentViewModel.currentContent?.let { content ->
                    contentViewModel.openContent(content)
                }
                contentViewModel.needShowContent.postValue(false)
            }
        })
        seriesViewModel.needOpenEpisode.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                contentViewModel.openContent(it)
                seriesViewModel.needOpenEpisode.value = null
            }
        })
        contentViewModel.currentContentLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showDescription(it)
            }
        })
        contentViewModel.seriesVisible.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showEpisodes(it)
            }
        })
        seriesViewModel.seriesLiveData.observe(viewLifecycleOwner, object : Observer<Series?> {
            override fun onChanged(series: Series?) {
                if (contentViewModel.currentContent is Episode) {
                    showSeriesName(series)
                }
            }
        })
        contentViewModel.requestFullscreenControls()
        contentViewModel.showEpisodes(false)
    }

    fun getOrientation(): Int {
        return resources.configuration.orientation
    }

    private fun setupViews() {
        setupEpisodes()
        binding?.btnShare?.setOnClickListener {
            shareContent()
        }
        binding?.btnFavourite?.setOnClickListener {
            favouriteContent()
        }
        updateOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation()
    }

    private fun updateOrientation(isLandscape: Boolean = resources.configuration.isOrientationLandscape()) {
        setupPlayerFullscreen(isLandscape)
    }

    private fun setupPlayerFullscreen(isFullscreen: Boolean) {
        if (isFullscreen) {
            binding?.containerEpisodesButton?.visibility = View.GONE
            binding?.episodesMargin?.visibility = View.GONE
            binding?.containerDescription?.visibility = View.GONE
            binding?.containerEpisodes?.visibility = View.GONE
            binding?.playerFragment?.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            contentViewModel.requestFullscreenControls()
        } else {
            binding?.containerEpisodesButton?.visibility = View.VISIBLE
            binding?.episodesMargin?.visibility =
                if (episodesButtonMarginEnable) View.VISIBLE else View.GONE
            binding?.containerDescription?.visibility = View.VISIBLE
            binding?.containerEpisodes?.visibility = View.VISIBLE
            binding?.playerFragment?.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onResume() {
        super.onResume()
        navigationViewModel.setCurrentScreen(screen)
    }

    override fun onPause() {
        super.onPause()
    }

    private fun showDescription(content: Content?) {
        when (content) {
            is Episode -> {
                val episode = content
                val series = seriesViewModel.getSeries() ?: episode.season?.series
                showDescriptionEpisode(episode, series)
            }
            is Movie -> {
                val movie = content
                showDescriptionMovie(movie)
            }
        }
    }

    private fun showDescriptionEpisode(episode: Episode, series: Series?) {
        showSeriesName(series)
        val contentNameSecondary = String.format(
            "S%02d:E%02d",
            episode.season?.number ?: 1,
            episode.number ?: 1
        ) + if (episode.name?.isNotEmpty() == true) "\n${episode.name}" else ""
        val fullscreenDescription = String.format(
            "S%02dE%02d:",
            episode.season?.number ?: 1,
            episode.number ?: 1
        ) + episode.name

        binding?.tvContentNameSecondary?.isVisible = true
        binding?.tvContentNameSecondary?.setText(contentNameSecondary)
        binding?.tvContentDescription?.setText(episode.description)
        episodesButtonMarginEnable = true
        binding?.episodesMargin?.visibility = View.VISIBLE
        binding?.tvContentNamePrimary?.visibility = View.VISIBLE
        binding?.tvContentNameSecondary?.visibility = View.VISIBLE
        binding?.btnShowEpisodes?.visibility = View.VISIBLE
    }


    private fun showDescriptionMovie(movie: Movie) {
        binding?.tvContentNamePrimary?.setText(movie.name ?: "")
        binding?.tvContentNameSecondary?.isVisible = false
        binding?.tvContentDescription?.setText(movie.description)
        episodesButtonMarginEnable = false
        binding?.episodesMargin?.visibility = View.GONE
        binding?.tvContentNamePrimary?.visibility = View.VISIBLE
        binding?.tvContentNameSecondary?.visibility = View.GONE
        binding?.btnShowEpisodes?.visibility = View.GONE
        showEpisodes(false)
    }

    private fun showSeriesName(series: Series?) {
        binding?.tvContentNamePrimary?.setText(series?.name ?: "")
        binding?.tvSeriesName?.setText(series?.name ?: "")
    }

    private fun shareContent() {
        navigationViewModel.createShareContentLink(contentViewModel.currentContent)?.let { link ->
            Timber.d("share link ${link}")
            val shareLinkIntent = IntentUtils.getShareLinkIntent(link.toString())
            startActivity(Intent.createChooser(shareLinkIntent, "Share link"))
        }
    }

    private fun favouriteContent() {
        Toast.makeText(context, R.string.added_to_favourites, Toast.LENGTH_SHORT).show()
    }

    private fun setupEpisodes() {
        val context = context ?: return
        episodesListHeight =
            (DeviceUtils.getDisplaySize(context).y - EPISODES_LIST_MARGIN_TOP)
        episodesListHeight = max(
            episodesListHeight,
            EPISODES_LIST_MIN_HEIGHT
        )

        binding?.btnShowEpisodes?.setOnClickListener {
            contentViewModel.seriesVisible.value?.let {
                contentViewModel.showEpisodes(!it)
            }
        }
        seriesViewModel.needShowSeriesDescription.postValue(false)
        seriesViewModel.needShowBanners.postValue(false)
    }

    private fun showEpisodes(isVisible: Boolean) {
        if (getOrientation() != Configuration.ORIENTATION_PORTRAIT) return
        binding?.containerEpisodes ?: return
        binding?.ivShowEpisodes ?: return

        if (isVisible) {
            binding?.containerEpisodes?.visibility = View.VISIBLE
            val animation = ResizeAnimation(binding?.containerEpisodes!!, episodesListHeight, 0)
            animation.duration = ActivityUtils.ANIMATION_DURATION_MEDIUM
            animation.fillAfter = true
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {}
                override fun onAnimationStart(p0: Animation?) {}
            })
            binding?.containerEpisodes?.startAnimation(animation)

            binding?.ivShowEpisodes?.animate()?.rotation(-180f)?.setDuration(
                ActivityUtils.ANIMATION_DURATION_MEDIUM
            )?.start()
        } else {
            val episodesVisible = binding?.containerEpisodes?.isVisible == true
            if (episodesVisible) {
                val animation = ResizeAnimation(binding?.containerEpisodes!!, 0, episodesListHeight)
                animation.duration = ActivityUtils.ANIMATION_DURATION_MEDIUM
                animation.fillAfter = true
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(p0: Animation?) {}
                    override fun onAnimationEnd(p0: Animation?) {
                        binding?.containerEpisodes?.isVisible = false
                    }

                    override fun onAnimationStart(p0: Animation?) {}
                })
                binding?.containerEpisodes?.startAnimation(animation)
                binding?.ivShowEpisodes?.animate()?.rotation(0f)?.setDuration(
                    ActivityUtils.ANIMATION_DURATION_MEDIUM
                )?.start()
            } else {
                binding?.containerEpisodes?.isVisible = false
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }
}

val Int.dp: Int
    get() {
        return (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
    }