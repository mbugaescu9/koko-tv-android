package com.kokoconnect.android.adapter.tv

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.model.tv.Channel
import com.kokoconnect.android.model.item.GuideRow
import com.kokoconnect.android.model.item.GuideRowCategory
import com.kokoconnect.android.model.item.GuideRowChannel
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.ui.fragment.tv.GuideFragment
import com.kokoconnect.android.vh.BannerViewHolder
import com.kokoconnect.android.vh.GuideChannelViewHolder
import com.kokoconnect.android.vh.GuideViewHolder
import com.kokoconnect.android.vh.GuideViewHolderBuilder
import org.jetbrains.anko.dimenAttr
import timber.log.Timber
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

val recyclers = mutableListOf<RecyclerView>()
var currentScroll = 0
fun Context.pixelsPerSecond(): Float {
    return (dimenAttr(R.attr.guideItem30Min).toFloat() * 2) / 3600
}

val onScrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        currentScroll += dx
        GuideFragment.globalTimeline?.scrollOffsetX = currentScroll
        recyclers.forEach {
            if (it != recyclerView) {
                it.removeOnScrollListener(this)
                it.scrollBy(dx, dy)
                it.addOnScrollListener(this)
            }
        }
    }
}

fun RecyclerView.snapToPosition(position: Int) {
    this.postDelayed({
        (this.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, 0)
    }, 100)
}

fun RecyclerView.smoothSnapToPosition(position: Int) {
    this.postDelayed({
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int {
                return LinearSmoothScroller.SNAP_TO_START
            }

            override fun getHorizontalSnapPreference(): Int {
                return LinearSmoothScroller.SNAP_TO_START
            }
        }
        smoothScroller.targetPosition = position
        (this.layoutManager as? LinearLayoutManager)?.startSmoothScroll(smoothScroller)

        //(this.layoutManager as? LinearLayoutManager)?.smoothScrollToPosition(this, RecyclerView.State(), position)
    }, 500)
}


class GuideListAdapter(
    var adsObjectsProvider: AdsObjectsProvider?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var bannersEnabled = true
        set(value) {
            Timber.d("bannersEnabled = ${value}")
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    var selectedChannel: Channel? by Delegates.observable(null) { _: KProperty<*>, oldChannel: Channel?, newChannel: Channel? ->
        val oldIndex = items.indexOfFirst { (it as? GuideRowChannel)?.channel == oldChannel }
        val newIndex = items.indexOfFirst { (it as? GuideRowChannel)?.channel == newChannel }
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }
    var currentCategory: String? = ""

    var onItemClickListener: ((Channel) -> Unit)? = null
    var items: List<GuideRow> by Delegates.observable(listOf()) { _: KProperty<*>, _: List<GuideRow>, newValue: List<GuideRow> ->
        notifyDataSetChanged()
    }
    var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView.apply {
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    (recyclerView.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                        (items.getOrNull(firstVisiblePosition) as? GuideRowCategory)?.let { categoryRow ->
                            currentCategory = categoryRow.name
                        }
                        (items.getOrNull(firstVisiblePosition) as? GuideRowChannel)?.let { channelRow ->
                            currentCategory = channelRow.channel.category
                        }
                    }
                }
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val row = items.first { it.rowType.ordinal == viewType }
        return GuideViewHolderBuilder.build(parent, row, adsObjectsProvider)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is BannerViewHolder -> {
                Timber.d("onBindViewHolder() bannersEnabled = ${bannersEnabled}")
                holder.setVisible(bannersEnabled)
            }
            is GuideChannelViewHolder -> {
                item as GuideRowChannel
                holder.isSelectedChannel = item.channel.id == selectedChannel?.id
                holder.onItemClickListener = onItemClickListener
            }
        }
        (holder as? GuideViewHolder)?.bind(item)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is GuideChannelViewHolder) {
            recyclers.remove(holder.recycler)
            holder.recycler.removeOnScrollListener(onScrollListener)
        }
    }

    override fun getItemViewType(position: Int): Int = items[position].rowType.ordinal

    fun timeTick(): Int {
        val position =
            items.indexOfFirst { (it as? GuideRowChannel)?.channel?.id == selectedChannel?.id }
        return position
    }

    fun navigateToCategory(category: String) {
        val index = items.indexOfFirst { (it is GuideRowCategory) && it.name == category }
        if (index > -1) recyclerView?.snapToPosition(index)
        currentCategory = category
    }
}