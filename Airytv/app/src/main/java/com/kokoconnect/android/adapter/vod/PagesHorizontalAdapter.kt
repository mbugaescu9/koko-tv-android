package com.kokoconnect.android.adapter.vod

import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.databinding.ItemCollectionPagesHorizElementBinding
import com.kokoconnect.android.model.vod.Content
import com.kokoconnect.android.ui.view.PagerRecyclerView
import com.kokoconnect.android.util.GlideScaleResizeTransformation
import com.bumptech.glide.Glide
import kotlin.math.min

class PagesHorizontalAdapter(displayMetrics: DisplayMetrics) : PagerRecyclerView.PagerRecyclerViewAdapter<PagesHorizontalAdapter.ViewHolder>(displayMetrics) {
    companion object {
        const val IMAGE_WIDTH = 800
        const val IMAGE_HEIGHT = 600
        const val MAX_ELEMENTS = Int.MAX_VALUE
    }

    private var imageWidth = IMAGE_WIDTH
    private var imageHeight = IMAGE_WIDTH
    private var imageTransformation = GlideScaleResizeTransformation(IMAGE_WIDTH, IMAGE_HEIGHT)

    private var maxElements = MAX_ELEMENTS

    fun setMaxElementsToShow(count: Int) {
        maxElements = count
    }

    fun setImagesMaxSize(width: Int, height: Int) {
        imageWidth = width
        imageHeight = height
        imageTransformation.destWidth = width
        imageTransformation.destHeight = height
    }

    var items: List<Content> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCollectionPagesHorizElementBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return min(items.size, maxElements)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items.getOrNull(holder.adapterPosition)?.let {
            holder.bind(it)
        }
    }

    inner class ViewHolder(val binding: ItemCollectionPagesHorizElementBinding) : PagerRecyclerView.PagerViewHolder(binding.root) {
        val ivMoviePoster = binding.ivMoviePoster
        val tvMovieName = binding.tvMovieName

        fun bind(content: Content) {
            content.name?.let {
                tvMovieName.setText(it)
            }
            content.poster?.getUrl()?.let {
                Glide.with(binding.root)
                    .load(it)
                    .override(imageWidth, imageHeight)
//                .transform(imageTransformation)
                    .into(ivMoviePoster)
            }
        }
    }
}