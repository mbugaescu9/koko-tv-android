package com.kokoconnect.android.vh.vod

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.vod.CollectionsPagedAdapter.Companion.POSTER_VERT_HEIGHT
import com.kokoconnect.android.adapter.vod.CollectionsPagedAdapter.Companion.POSTER_VERT_WIDTH
import com.kokoconnect.android.databinding.ItemCollectionGridElementBinding
import com.kokoconnect.android.databinding.ItemCollectionListHorizElementBinding
import com.kokoconnect.android.databinding.ItemLoadingProgressBinding
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.util.*
import com.kokoconnect.android.vh.BannerViewHolder
import com.kokoconnect.android.vh.LoadingProgressViewHolder
import com.bumptech.glide.Glide

object ContentViewHolderBuilder {
    fun build(
        parent: ViewGroup,
        rowType: ContentRowTypes,
        adsObjectsProvider: AdsObjectsProvider? = null
    ): RecyclerView.ViewHolder {
        return when (rowType) {
            ContentRowTypes.BANNER_AD -> buildBannerRow(rowType, parent, adsObjectsProvider)
            else -> buildContentRow(rowType, parent)//CollectionBannerRow
        }
    }

    private fun buildContentRow(
        rowType: ContentRowTypes,
        parent: ViewGroup
    ): RecyclerView.ViewHolder {
        val layoutId = rowType.layoutId
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return when (rowType) {
            ContentRowTypes.CONTENT_GRID -> {
                val binding = ItemCollectionGridElementBinding.bind(view)
                ContentDataGridViewHolder(binding)
            }
            ContentRowTypes.CONTENT_LIST_HORIZONTAL -> {
                val binding = ItemCollectionListHorizElementBinding.bind(view)
                ContentDataListHorizontalViewHolder(binding)
            }
            else -> {
                val binding = ItemLoadingProgressBinding.bind(view)
                LoadingProgressViewHolder(binding)
            }
        }
    }

    private fun buildBannerRow(
        rowType: ContentRowTypes,
        parent: ViewGroup,
        adsObjectsProvider: AdsObjectsProvider? = null
    ): BannerViewHolder {
        return BannerViewHolder.buildFor(parent, adsObjectsProvider = adsObjectsProvider)
    }
}

open class ContentViewHolder(view: View) : RecyclerView.ViewHolder(view) {}

class ContentDataGridViewHolder(val binding: ItemCollectionGridElementBinding) : ContentViewHolder(binding.root) {

    var onContentClickListener: OnContentClickListener? = null

    fun bind(row: ContentDataGridRow) {
        val content = row.content
        val collection = row.collection
        val posterUrl = content.poster?.getUrl()
        val context = binding.root.context
        Glide.with(context)
            .load(posterUrl)
            .placeholder(R.drawable.shape_poster_vertical_placeholder)
            .override(POSTER_VERT_WIDTH, POSTER_VERT_HEIGHT)
            .into(binding.ivMoviePoster)
        binding.tvPosterSource.visibility = View.GONE
//        val domainName = NetworkUtils.getDomainName(posterUrl)
//        if (domainName != null && domainName.isNotEmpty()) {
//            binding.tvPosterSource.visibility = View.VISIBLE
//            binding.tvPosterSource.setText(domainName ?: "")
//        }
        binding.ivMoviePoster.setOnClickListener {
            onContentClickListener?.invoke(content, collection)
        }
    }
}

class ContentDataListHorizontalViewHolder(
    val binding: ItemCollectionListHorizElementBinding
) : ContentViewHolder(binding.root) {

    var onContentClickListener: OnContentClickListener? = null

    fun bind(row: ContentDataListHorizontalRow) {
        val context = binding.root.context
        val content = row.content
        val collection = row.collection
        val posterUrl = content.poster?.getUrl()
        Glide.with(context)
            .load(posterUrl)
            .placeholder(R.drawable.shape_poster_vertical_placeholder)
            .override(POSTER_VERT_WIDTH, POSTER_VERT_HEIGHT)
            .into(binding.ivMoviePoster)
        binding.tvPosterSource.visibility = View.GONE
//        val domainName = NetworkUtils.getDomainName(posterUrl)
//        if (domainName != null && domainName.isNotEmpty()) {
//            binding.tvPosterSource.visibility = View.VISIBLE
//            binding.tvPosterSource.setText(domainName ?: "")
//        }
        binding.ivMoviePoster.setOnClickListener {
            onContentClickListener?.invoke(content, collection)
        }
    }
}