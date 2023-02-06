package com.kokoconnect.android.ui.fragment.vod

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.vod.SeriesAdapter
import com.kokoconnect.android.databinding.FragmentEpisodesBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.ads.banner.BannerManager
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.vod.Episode
import com.kokoconnect.android.model.vod.Season
import com.kokoconnect.android.model.vod.SeriesRow
import com.kokoconnect.android.util.ActivityUtils
import com.kokoconnect.android.vm.AdsViewModel
import com.kokoconnect.android.vm.AmsEventsViewModel
import com.kokoconnect.android.vm.vod.VodContentViewModel
import com.kokoconnect.android.vm.NavigationViewModel
import com.kokoconnect.android.vm.vod.SeriesViewModel
import timber.log.Timber
import javax.inject.Inject


class EpisodesFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private var binding: FragmentEpisodesBinding? = null

    val contentViewModel: VodContentViewModel by activityViewModels { viewModelFactory }
    val seriesViewModel: SeriesViewModel by activityViewModels { viewModelFactory }
    val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }
    val adsViewModel: AdsViewModel by activityViewModels { viewModelFactory }
    val eventsViewModel: AmsEventsViewModel by activityViewModels { viewModelFactory }

    private lateinit var adapter: SeriesAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEpisodesBinding.inflate(inflater)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        seriesViewModel.reset()

        setupViews()


        seriesViewModel.seriesRows.observe(viewLifecycleOwner, Observer {
            updateSeriesRows(it)
        })

        seriesViewModel.errorLiveData.observe(viewLifecycleOwner, Observer { apiError ->
            if (apiError != null) {
                Timber.e("ApiError ${apiError}")
                seriesViewModel.errorLiveData.postValue(null)
            }
        })

        seriesViewModel.loading.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter.progressVisible = it
                binding?.srlContainer?.isRefreshing = it
            }
        })


        contentViewModel.needShowSeries.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                contentViewModel.currentSeries?.let {
//                    Timber.d("currentSeries() ${it.name} seasons count ${it.seasons.size}")
                    seriesViewModel.setSeries(it)
                }
            }
        })
        contentViewModel.needShowSeason.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                contentViewModel.currentSeason?.seriesId?.let {
                    seriesViewModel.setSeriesId(it)
                }
            }
        })
        contentViewModel.needShowContent.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                contentViewModel.currentContent?.let {
                    when (it) {
                        is Episode -> {
                            it.seriesId?.let { seriesId ->
                                seriesViewModel.setSeriesId(seriesId)
                            }
                        }
                        is Season -> {
                            it.seriesId?.let { seriesId ->
                                seriesViewModel.setSeriesId(seriesId)
                            }
                        }
                        else -> {
                        }
                    }
                }
            }
        })

        seriesViewModel.needShowSeriesDescription.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter.headerEnabled = it
            }
        })

        seriesViewModel.needShowBanners.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter.bannersEnabled = it
            }
        })

    }

    private fun updateSeriesRows(seriesRows: PagingData<SeriesRow>) {
        adapter.submitData(lifecycle, seriesRows)
    }

    private fun setupViews() {
        val context = context ?: return
        adapter = SeriesAdapter(object : AdsObjectsProvider {
            override fun provideActivity(): Activity? {
                return activity
            }

            override fun provideBannerManager(): BannerManager? {
                return if (adsViewModel.adsEnabled()) {
                    adsViewModel.getBannerManager(eventsViewModel)
                } else {
                    null
                }
            }
        })
        adapter.listener = object : SeriesAdapter.Listener {
            override fun onEpisodeClick(episode: Episode?, position: Int) {
                Timber.d("Episode ${episode?.number} ${episode?.name}")
                seriesViewModel.needOpenEpisode.postValue(episode)
            }
        }
        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding?.rvEpisodesList?.adapter = adapter
        binding?.rvEpisodesList?.layoutManager = layoutManager

        val colorBackground = ActivityUtils.getColorFromAttr(context, R.attr.colorSurfaceVariant6)
        val progressColor = ActivityUtils.getColorFromAttr(context, R.attr.colorOnSurfaceVariant4)
        binding?.srlContainer?.setColorSchemeColors(progressColor)
        binding?.srlContainer?.setProgressBackgroundColorSchemeColor(colorBackground)
        binding?.srlContainer?.setOnRefreshListener {
            seriesViewModel.reloadEpisodes()
        }
    }

    fun getOrientation(): Int = resources.configuration.orientation
}