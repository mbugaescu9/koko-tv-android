package com.kokoconnect.android.ui.fragment.vod

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.vod.ContentGridAdapter
import com.kokoconnect.android.adapter.vod.ContentGridPagedAdapter
import com.kokoconnect.android.databinding.FragmentCollectionBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.ads.banner.BannerManager
import com.kokoconnect.android.model.vod.ContentRow
import com.kokoconnect.android.model.vod.Series
import com.kokoconnect.android.util.ActivityUtils
import com.kokoconnect.android.util.navigateSafe
import com.kokoconnect.android.vm.AdsViewModel
import com.kokoconnect.android.vm.AmsEventsViewModel
import com.kokoconnect.android.vm.vod.CollectionViewModel
import com.kokoconnect.android.vm.vod.VodContentViewModel
import timber.log.Timber
import javax.inject.Inject

class CollectionFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    lateinit var adapter: ContentGridPagedAdapter
    lateinit var layoutManager: GridLayoutManager

    val contentViewModel: VodContentViewModel by activityViewModels { viewModelFactory }
    val collectionViewModel: CollectionViewModel by activityViewModels { viewModelFactory }
    val eventsViewModel: AmsEventsViewModel by activityViewModels { viewModelFactory }
    val adsViewModel: AdsViewModel by activityViewModels { viewModelFactory }

    var gridListItemDecoration: RecyclerView.ItemDecoration? = null

    var binding: FragmentCollectionBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("CollectionFragment: onCreateView()")
        binding = FragmentCollectionBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        collectionViewModel.reset()
        collectionViewModel.contentRows.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showCollection(it)
            }
        })
        collectionViewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {

                collectionViewModel.errorLiveData.postValue(null)
            }
        })
        collectionViewModel.loading.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter.progressVisible = it
                binding?.srlContainer?.isRefreshing = it
            }
        })
        contentViewModel.needShowCollection.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                contentViewModel.currentCollection?.let { collection ->
                    collectionViewModel.setCollection(collection)
                }
                collectionViewModel.reloadCollection()
            }
        })
    }

    private fun setupViews() {
        val context = context ?: return
        adapter = ContentGridPagedAdapter(object : AdsObjectsProvider {
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
        adapter.contentClickListener = { content, collection ->
            contentViewModel.openContent(content)
            when (content) {
                is Series -> {
                    findNavController().navigateSafe(R.id.action_fragmentContentCollection_to_fragmentContentSeries)
                }
                else -> {
                    findNavController().navigateSafe(R.id.action_fragmentContentCollection_to_fragmentContent)
                }
            }
        }

        binding?.collectionList?.adapter = adapter
        layoutManager = GridLayoutManager(context, ContentGridAdapter.GRID_SPAN_COUNT)
        layoutManager.spanSizeLookup = adapter.spanSizeLookup
        binding?.collectionList?.layoutManager = layoutManager

        val colorBackground = ActivityUtils.getColorFromAttr(context, R.attr.colorSurfaceVariant6)
        val progressColor = ActivityUtils.getColorFromAttr(context, R.attr.colorOnSurfaceVariant4)
        binding?.srlContainer?.setColorSchemeColors(progressColor)
        binding?.srlContainer?.setProgressBackgroundColorSchemeColor(colorBackground)
        binding?.srlContainer?.setOnRefreshListener {
            collectionViewModel.reloadCollection()
        }
//        gridListItemDecoration?.let {
//            binding?.collectionList?.removeItemDecoration(it)
//        }
//        gridListItemDecoration = adapter.gridListItemDecoration
//        gridListItemDecoration?.let {
//            binding?.collectionList?.addItemDecoration(it)
//        }
    }

    private fun showCollection(collection: PagingData<ContentRow>) {
        adapter.submitData(lifecycle, collection)
    }

    override fun onResume() {
        collectionViewModel.reloadCollection()
        super.onResume()
    }
}