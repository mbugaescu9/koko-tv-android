package com.kokoconnect.android.vh.vod

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.adapter.vod.*
import com.kokoconnect.android.adapter.vod.CollectionsPagedAdapter.Companion.POSTER_HORZ_HEIGHT
import com.kokoconnect.android.adapter.vod.CollectionsPagedAdapter.Companion.POSTER_HORZ_WIDTH
import com.kokoconnect.android.databinding.ItemCollectionGridBinding
import com.kokoconnect.android.databinding.ItemCollectionListHorizontalBinding
import com.kokoconnect.android.databinding.ItemCollectionPagesHorizontalBinding
import com.kokoconnect.android.databinding.ItemLoadingProgressBinding
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.model.vod.Collection
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.repo.pagingsource.ContentListHorizontalPagingSource
import com.kokoconnect.android.util.*
import com.kokoconnect.android.vh.BannerViewHolder
import com.kokoconnect.android.vh.LoadingProgressViewHolder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import org.jetbrains.anko.displayMetrics

typealias OnContentClickListener = ((content: Content, collection: Collection?) -> Unit)

object CollectionsViewHolderBuilder {
    fun build(
        parent: ViewGroup,
        rowType: CollectionsRowTypes,
        adsObjectsProvider: AdsObjectsProvider? = null
    ): RecyclerView.ViewHolder {
        return when (rowType) {
            CollectionsRowTypes.BANNER_AD -> buildBannerRow(rowType, parent, adsObjectsProvider) //CollectionBannerRow
            else -> buildCollectionRow(rowType, parent, adsObjectsProvider)
        }
    }

    private fun buildCollectionRow(
        rowType: CollectionsRowTypes,
        parent: ViewGroup,
        adsObjectsProvider: AdsObjectsProvider? = null
    ): RecyclerView.ViewHolder {
        val layoutId = rowType.layoutId
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return when (rowType) {
            CollectionsRowTypes.COLLECTION_GRID -> {
                val binding = ItemCollectionGridBinding.bind(view)
                CollectionsGridViewHolder(binding, adsObjectsProvider)
            }
            CollectionsRowTypes.COLLECTION_LIST_HORIZONTAL -> {
                val binding = ItemCollectionListHorizontalBinding.bind(view)
                CollectionsListHorizontalViewHolder(binding, adsObjectsProvider)
            }
            else -> {
                val binding = ItemLoadingProgressBinding.bind(view)
                LoadingProgressViewHolder(binding)
            }
        }
    }

    private fun buildBannerRow(
        rowType: CollectionsRowTypes,
        parent: ViewGroup,
        adsObjectsProvider: AdsObjectsProvider? = null
    ): BannerViewHolder {
        return BannerViewHolder.buildFor(parent, adsObjectsProvider = adsObjectsProvider)
    }
}

open class CollectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {}

abstract class CollectionDataViewHolder(view: View) : CollectionViewHolder(view) {
    var listener: CollectionsPagedAdapter.Listener? = null

    open fun bind(row: CollectionRow, position: Int) {}
}

class CollectionsGridViewHolder(
    val binding: ItemCollectionGridBinding,
    adsObjectsProvider: AdsObjectsProvider? = null
) : CollectionDataViewHolder(binding.root) {
    val adapter = ContentGridAdapter(adsObjectsProvider)
    var layoutManager: RecyclerView.LayoutManager? = null
    var gridItemSpacingDecoration: RecyclerView.ItemDecoration? = null

    override fun bind(row: CollectionRow, position: Int) {
        val collection = row.collection
        binding.tvCollectionName.setText(collection.name)
        val context = binding.root.context
        layoutManager = GridLayoutManager(context, 3)

        binding.btnCollectionMore.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                listener?.onShowMore(collection, position)
            }
        }
        adapter.contentClickListener = { content, itemPosition ->
            listener?.onContentClick(content, collection, position)
        }
        if (gridItemSpacingDecoration == null) {
            gridItemSpacingDecoration = GridSpacingItemDecoration(
                spanCount = 3,
                spacing = ContentGridAdapter.SPACING_RECT,
                includeEdge = false,
                headerNum = 0
            )
            gridItemSpacingDecoration?.let {
                binding.rvCollectionContentList.addItemDecoration(it)
            }
        }
        adapter.itemCountToShow = 3
        adapter.items = collection.contents.map {
            ContentDataGridRow(it, collection)
        }.toMutableList()
        binding.rvCollectionContentList.layoutManager = layoutManager
        binding.rvCollectionContentList.adapter = adapter
    }
}

class CollectionsListHorizontalViewHolder(
    val binding: ItemCollectionListHorizontalBinding,
    adsObjectsProvider: AdsObjectsProvider? = null,
) : CollectionDataViewHolder(binding.root) {
    var layoutManager: RecyclerView.LayoutManager? = null
    lateinit var airyRepo: AiryRepository
    private var adapterJob: Job? = null

    override fun bind(row: CollectionRow, position: Int) {
        val collection = row.collection
        val context = binding.root.context
        binding.tvCollectionName.setText(collection.name)
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.btnCollectionMore.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                listener?.onShowMore(collection, position)
            }
        }
        binding.tvCollectionName.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                listener?.onShowMore(collection, position)
            }
        }
        collection.id?.let { collectionId ->
            val pagingSource = ContentListHorizontalPagingSource(
                airyRepo,
                collectionId,
                collection.contents.map {
                    ContentDataListHorizontalRow(it, row.collection)
                }.toMutableList()
            )
            binding.rvCollectionContentList.adapter = null
            val adapter = ContentListHorizontalAdapter()
            adapter.contentClickListener = { content, itemPosition ->
                listener?.onContentClick(content, collection, position)
            }
            adapterJob?.cancel()
            adapterJob = GlobalScope.launch {
                try {
                    adapter.submitData(PagingData.empty())
                    Pager(
                        PagingConfig(6)
                    ) {
                        pagingSource
                    }.flow.cachedIn(GlobalScope)
                        .collectLatest { pagingData ->
                            adapter.submitData(pagingData)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            binding.rvCollectionContentList.layoutManager = layoutManager
            binding.rvCollectionContentList.adapter = adapter
        }
    }
}

class PagesHorizontalViewHolder(val binding: ItemCollectionPagesHorizontalBinding) : CollectionDataViewHolder(binding.root) {
    val adapter = PagesHorizontalAdapter(binding.root.context.displayMetrics)

    override fun bind(row: CollectionRow, position: Int) {
        val collection = row.collection
        binding.tvCollectionName.setText(collection.name)
        binding.btnCollectionMore.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                listener?.onShowMore(collection, position)
            }
        }

        adapter.setImagesMaxSize(POSTER_HORZ_WIDTH, POSTER_HORZ_HEIGHT)
        adapter.setMaxElementsToShow(10)

        adapter.items = collection.contents

        binding.collectionContentPagerIndicator.createIndicators(adapter.itemCount, 0)
        binding.collectionContentPager.setOnPageChangedListener {
            binding.collectionContentPagerIndicator.animatePageSelected(it)
        }
        binding.collectionContentPager.adapter = adapter
    }
}
