package com.kokoconnect.android.ui.fragment.vod

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.vod.CollectionsPagedAdapter
import com.kokoconnect.android.adapter.vod.ContentGridAdapter
import com.kokoconnect.android.adapter.vod.ContentGridPagedAdapter
import com.kokoconnect.android.databinding.FragmentContentAllBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.ads.banner.BannerManager
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.model.vod.Collection
import com.kokoconnect.android.model.vod.Series
import com.kokoconnect.android.util.ActivityUtils
import com.kokoconnect.android.util.navigateSafe
import com.kokoconnect.android.vm.AdsViewModel
import com.kokoconnect.android.vm.AmsEventsViewModel
import com.kokoconnect.android.vm.vod.VodContentViewModel
import com.kokoconnect.android.vm.NavigationViewModel
import com.kokoconnect.android.vm.vod.VodViewModel
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

class ContentAllFragment : Fragment(), Injectable {
    companion object {
        val screen = Screen().apply {
            this.name = ScreenType.VOD.defaultName
            this.type = ScreenType.VOD
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    var binding: FragmentContentAllBinding? = null
    lateinit var pagedAdapter: CollectionsPagedAdapter
    lateinit var searchPagedAdapter: ContentGridPagedAdapter
    var searchJob: Job? = null
    var lastQuery: String? = null

    val navigationViewModel: NavigationViewModel by activityViewModels{ viewModelFactory }
    val contentViewModel: VodContentViewModel by activityViewModels{ viewModelFactory }
    val vodViewModel: VodViewModel by activityViewModels{ viewModelFactory }
    val adsViewModel: AdsViewModel by activityViewModels{ viewModelFactory }
    val eventsViewModel: AmsEventsViewModel by activityViewModels{ viewModelFactory }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContentAllBinding.inflate(inflater)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()

        vodViewModel.collectionsRows.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showCollections(it)
            }
        })
        vodViewModel.searchRows.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showSearchResult(it)
            }
        })
        vodViewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                Timber.d("errorLiveData ${it}")
                vodViewModel.errorLiveData.postValue(null)
            }
        })
        vodViewModel.loading.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                pagedAdapter.progressVisible = it
                binding?.contentAll?.srlContainer?.isRefreshing = it
            }
        })
        vodViewModel.searchLoading.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                pagedAdapter.progressVisible = it
                binding?.contentSearch?.srlSearchContainer?.isRefreshing = it
            }
        })
        vodViewModel.searchQuery.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                onQueryChanged(it)
            }
        })
        vodViewModel.searchResultsVisible.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                binding?.contentAll?.root?.isVisible = !it
                binding?.contentSearch?.root?.isVisible = it
            }
        })

//        pagedAdapter.refresh()
//        searchPagedAdapter.refresh()
    }

    private fun setupViews() {
        val adsObjectsProvider = object : AdsObjectsProvider {
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
        }
        pagedAdapter = CollectionsPagedAdapter(adsObjectsProvider, vodViewModel.airyRepo)
        pagedAdapter.listener = object: CollectionsPagedAdapter.Listener {
            override fun onShowMore(collection: Collection, position: Int) {
                contentViewModel.reset()
                contentViewModel.setCollection(collection, true)
                findNavController().navigateSafe(R.id.action_fragmentContentAll_to_fragmentContentCollection)
            }

            override fun onContentClick(content: Content, collection: Collection, position: Int) {
                onContentClick(content, collection)
            }
        }
        searchPagedAdapter = ContentGridPagedAdapter(adsObjectsProvider)
        searchPagedAdapter.contentClickListener = { content, collection ->
            onContentClick(content, collection)
        }

        binding?.contentAll?.collectionsList?.adapter = pagedAdapter
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding?.contentAll?.collectionsList?.layoutManager = layoutManager

        binding?.contentSearch?.rvSearchResults?.adapter = searchPagedAdapter
        val searchLayoutManager = GridLayoutManager(context, ContentGridAdapter.GRID_SPAN_COUNT)
        binding?.contentSearch?.rvSearchResults?.layoutManager = searchLayoutManager

        val context = context ?: return
        val colorBackground = ActivityUtils.getColorFromAttr(context, R.attr.colorSurfaceVariant6)
        val progressColor = ActivityUtils.getColorFromAttr(context, R.attr.colorOnSurfaceVariant4)
        binding?.contentAll?.srlContainer?.setColorSchemeColors(progressColor)
        binding?.contentAll?.srlContainer?.setProgressBackgroundColorSchemeColor(colorBackground)
        binding?.contentAll?.srlContainer?.setOnRefreshListener {
            vodViewModel.reloadCollections()
        }

        binding?.contentSearch?.srlSearchContainer?.setColorSchemeColors(progressColor)
        binding?.contentSearch?.srlSearchContainer?.setProgressBackgroundColorSchemeColor(colorBackground)
        binding?.contentSearch?.srlSearchContainer?.setOnRefreshListener {
            vodViewModel.searchContent()
        }

        binding?.etSearchQuery?.addTextChangedListener { queryInput ->
            vodViewModel.setSearchQuery(queryInput?.toString() ?: "")
        }
    }

    private fun onQueryChanged(newQuery: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch (Dispatchers.Main) {
            delay(600)
            if (isActive && newQuery.isNotEmpty() && newQuery != lastQuery) {
                lastQuery = newQuery
                Timber.d("onQueryChanged() search")
                vodViewModel.searchContent()
            }
        }
    }

    private fun onContentClick(content: Content, collection: Collection?) {
        Timber.d("onContentClick() content = ${content.javaClass.simpleName}")
        contentViewModel.reset()
        contentViewModel.setCollection(collection)
        contentViewModel.openContent(content)
        when (content) {
            is Series -> {
                findNavController().navigateSafe(R.id.action_fragmentContentAll_to_fragmentContentSeries)
            }
            is Season -> {
                findNavController().navigateSafe(R.id.action_fragmentContentAll_to_fragmentContentSeries)
            }
            else -> {
                findNavController().navigateSafe(R.id.action_fragmentContentAll_to_fragmentContent)
            }
        }
    }

    override fun onResume() {
        navigationViewModel.setCurrentScreen(screen, this)
        super.onResume()
    }


    private fun showCollections(collections: PagingData<CollectionsRow>) {
        pagedAdapter.submitData(lifecycle, collections)
    }

    private fun showSearchResult(contents: PagingData<ContentRow>) {
        searchPagedAdapter.submitData(lifecycle, contents)
    }
}