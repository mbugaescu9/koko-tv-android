package com.kokoconnect.android.ui.fragment.tv

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.*
import com.kokoconnect.android.adapter.tv.*
import com.kokoconnect.android.databinding.FragmentGuideBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.model.*
import com.kokoconnect.android.model.ads.AdsStatus
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.ads.banner.BannerManager
import com.kokoconnect.android.model.item.GuideRow
import com.kokoconnect.android.model.item.GuideRowBanner
import com.kokoconnect.android.model.item.GuideRowCategory
import com.kokoconnect.android.model.item.GuideRowChannel
import com.kokoconnect.android.model.player.YouTube
import com.kokoconnect.android.model.response.ApiError
import com.kokoconnect.android.model.player.VideoOpeningReason
import com.kokoconnect.android.model.tv.Category
import com.kokoconnect.android.model.tv.Channel
import com.kokoconnect.android.ui.dialog.CategoryPopup
import com.kokoconnect.android.ui.view.GlobalTimelineView
import com.kokoconnect.android.util.DateUtils.parseIsoDate
import com.kokoconnect.android.util.isOrientationLandscape
import com.kokoconnect.android.vh.GuideChannelViewHolder
import com.kokoconnect.android.vm.AdsViewModel
import com.kokoconnect.android.vm.AmsEventsViewModel
import com.kokoconnect.android.vm.tv.TvGuideViewModel
import com.kokoconnect.android.vm.PlayerViewModel
import javax.inject.Inject


class GuideFragment : Fragment(), Injectable {
    companion object {
        var globalTimeline: GlobalTimelineView? = null
    }

    private var adsStatus: AdsStatus? = null
    private lateinit var adsViewModel: AdsViewModel
    private val timelineAdapter: TimelineAdapter = TimelineAdapter()
    private lateinit var eventsModel: AmsEventsViewModel
    private lateinit var tvGuideViewModel: TvGuideViewModel
    private lateinit var playerViewModel: PlayerViewModel
    private var adapter: GuideListAdapter? = null
    private val onItemClickListener: (Channel) -> Unit = { channel ->
        channel.openingReason = VideoOpeningReason.ON_TV_CHANNEL_CHANGE
        tvGuideViewModel.channelLiveData.postValue(channel)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var binding: FragmentGuideBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGuideBinding.inflate(inflater)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        globalTimeline = null
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val activityVMP = ViewModelProvider(activity)
        val activityInjectedVMP = ViewModelProvider(activity, viewModelFactory)

        eventsModel = activityVMP.get(AmsEventsViewModel::class.java)
        adsViewModel = activityInjectedVMP.get(AdsViewModel::class.java)
        tvGuideViewModel = activityInjectedVMP.get(TvGuideViewModel::class.java)
        playerViewModel = activityVMP.get(PlayerViewModel::class.java)

        setupViews()

        tvGuideViewModel.init(activity)
        tvGuideViewModel.guideLiveData.observe(viewLifecycleOwner, Observer {
            val guide = tvGuideViewModel.getGuide()
            val ads = adsStatus
            if (guide != null) {
                showGuide(guide, ads)
            }
        })
        adsViewModel.adsStatus.observe(viewLifecycleOwner, Observer {
            adsStatus = it
            val guide = tvGuideViewModel.getGuide()
            val ads = adsStatus
            if (guide != null && ads != null) {
                showGuide(guide, ads)
            }
        })
        adsViewModel.bannersEnabled.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter?.bannersEnabled = it
            }
        })
        tvGuideViewModel.timeTickLiveData.observe(viewLifecycleOwner, Observer {
            globalTimeline?.timeTick()
            val pos = adapter?.timeTick()
            val children =
                binding?.recyclerView?.children?.find {
                    binding?.recyclerView?.getChildAdapterPosition(
                        it
                    ) == pos
                }
            if (children != null) {
                val holder = binding?.recyclerView?.getChildViewHolder(children) as? GuideChannelViewHolder
                (holder?.recycler?.adapter as? ProgramGuideAdapter)?.updateSelectedPosition()
            }
        })
        tvGuideViewModel.channelLiveData.observe(viewLifecycleOwner, Observer { channel ->
            if (channel != null) {
                showError(false)
                tvGuideViewModel.openChannel(channel, adsViewModel)
                adapter?.selectedChannel = channel
                tvGuideViewModel.channelLiveData.value = null
            }
        })
        tvGuideViewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                tvGuideViewModel.requestGuideFromServer()
                val message: String = when (it) {
                    ApiError.NETWORK_PROBLEM -> {
                        tvGuideViewModel.errorLiveData.value = ApiError.NONE
                        getString(R.string.error_network_problem)
                    }
                    ApiError.SERVER_ERROR -> {
                        tvGuideViewModel.errorLiveData.value = ApiError.NONE
                        getString(R.string.error_server_error)
                    }
                    ApiError.SERVER_RESULT_NOT_200 -> {
                        tvGuideViewModel.errorLiveData.value = ApiError.NONE
                        "${getString(R.string.error_server_error_code)}, ${it.code}"
                    }
                    else -> return@Observer
                }
                showError(tvGuideViewModel.getGuide() == null)
            }
        })
        tvGuideViewModel.needOpenProgram.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showBanners(it.video !is YouTube)
            }
        })
    }

    private fun setupViews() {
        globalTimeline = binding?.timeline
        adapter = GuideListAdapter(object : AdsObjectsProvider {
            override fun provideActivity(): Activity? = activity
            override fun provideBannerManager(): BannerManager? {
                return if (adsViewModel.adsEnabled()) {
                    adsViewModel.getBannerManager(eventsModel)
                } else {
                    null
                }
            }
        })
        binding?.timelineContainer?.timelineRecycler?.let { timelineRecycler ->
            timelineRecycler.layoutManager =
                LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            timelineRecycler.adapter = timelineAdapter
            timelineRecycler.itemAnimator = null
            timelineRecycler.isTouchable = false
            recyclers.add(timelineRecycler)
        }
        binding?.timelineContainer?.genreLayout?.setOnClickListener {
            showPopup()
        }
        showBanners(tvGuideViewModel.needOpenProgram.value?.video is YouTube)

        binding?.recyclerView?.layoutManager =
            SnappingLinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        binding?.recyclerView?.adapter = adapter
        adapter?.onItemClickListener = onItemClickListener
        updateOrientation()
    }

    private fun showBanners(visible: Boolean = true) {
        adapter?.bannersEnabled = visible
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation()
    }

    private fun updateOrientation(isLandscape: Boolean = resources.configuration.isOrientationLandscape()) {
        val bannersEnabled = !isLandscape && tvGuideViewModel.needOpenProgram.value?.video !is YouTube
        showBanners(bannersEnabled)
    }

    override fun onResume() {
        super.onResume()
        tvGuideViewModel.requestGuideFromServer()
    }

    private fun showError(enabled: Boolean, message: String? = null) {
        if (tvGuideViewModel.getGuide() == null && enabled) {
            binding?.errorMessage?.textViewMessage?.text = message
            binding?.errorMessage?.root?.visibility = View.VISIBLE
            binding?.errorMessage?.buttonRetry?.setOnClickListener {
                binding?.errorMessage?.root?.visibility = View.GONE
                tvGuideViewModel.requestGuideFromServer()
            }
        } else {
            binding?.errorMessage?.root?.visibility = View.GONE
        }
    }

    private fun showGuide(
        guide: List<Category>,
        ads: AdsStatus? = null
    ) {
        val adapter = adapter ?: return
        showError(false)
        guide.forEach {
            it.name = it.name?.replace("_", " ")
        }
        adapter.items = guide.flatMap {
            val list = if (ads != null && ads.isBannersEnabled(requireContext())) {
                val bannerManager = adsViewModel.getBannerManager(eventsModel)
                if (bannerManager != null) {
                    listOf<GuideRow>(GuideRowBanner(bannerManager))
                } else {
                    emptyList<GuideRow>()
                }
            } else {
                emptyList<GuideRow>()
            }
            list + listOf(GuideRowCategory(it.name)) + it.channels().map { channel ->
                GuideRowChannel(channel)
            }
        }
        var maxFirstTime = 0L
        var minEndTime = 0L
        val program = guide.getOrNull(0)?.channels()?.getOrNull(0)?.programs?.getOrNull(0)
        program?.let {
            maxFirstTime =
                parseIsoDate(program.realStartAtIso) + (program.realDuration - program.duration) * 1000
            minEndTime = maxFirstTime + (8 * 60 + 30) * 60 * 1000
        }
        timelineAdapter.fromTime = maxFirstTime.drop(TIMELINE_INTERVAL)
        timelineAdapter.toTime = minEndTime.drop(TIMELINE_INTERVAL)
        globalTimeline?.fromTime = maxFirstTime.drop(TIMELINE_INTERVAL)
        globalTimeline?.toTime = minEndTime.drop(TIMELINE_INTERVAL)
        timelineAdapter.notifyDataSetChanged()
        tvGuideViewModel.openChannelIfNecessary()
    }


    private fun showPopup() {
        val adapter = adapter ?: return
        val genreLayout = binding?.timelineContainer?.genreLayout ?: return
        val activity = activity ?: return
        val items = adapter.items.filter { it is GuideRowCategory }
            .map { (it as GuideRowCategory).name.orEmpty() }.toMutableList()
        val positionInGenre = items.indexOfFirst {
            it == adapter.currentCategory
        }

        CategoryPopup(activity)
            .setCategories(items, positionInGenre)
            .setOnCategorySelected {
                adapter.navigateToCategory(it)
            }.showUnder(genreLayout)
    }
}

fun Long.drop(value: Long): Long = this - this % value
fun Long.drop(value: Int): Long = this - this % value
