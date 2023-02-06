package com.kokoconnect.android.vh

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.tv.*
import com.kokoconnect.android.databinding.ItemGuideChannelLayoutBinding
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider
import com.kokoconnect.android.model.item.*
import com.kokoconnect.android.model.tv.Channel
import com.kokoconnect.android.model.tv.Program
import kotlin.math.ceil

import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.dimenAttr


object GuideViewHolderBuilder {

    fun build(
        parent: ViewGroup,
        guideRow: GuideRow,
        adsObjectsProvider: AdsObjectsProvider? = null,
        attachToRoot: Boolean = false
    ): RecyclerView.ViewHolder {
        return when (guideRow) {
            is GuideRowBanner -> buildBannerRow(parent, adsObjectsProvider, attachToRoot)
            else -> buildGuideRow(guideRow, parent, attachToRoot)
        }
    }

    fun buildGuideRow(
        row: GuideRow,
        parent: ViewGroup,
        attachToRoot: Boolean = false
    ): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(row.rowType.layoutId, parent, attachToRoot)
        return when (row.rowType) {
            GuideRowEnum.GUIDE_ROW_CATEGORY -> GuideCategoryViewHolder(view)
            /*GuideRowEnum.GUIDE_ROW_CHANNEL*/ else -> GuideChannelViewHolder(parent.context, view)
        }
    }

    fun buildBannerRow(
        parent: ViewGroup,
        adsObjectsProvider: AdsObjectsProvider? = null,
        attachToRoot: Boolean = false
    ): RecyclerView.ViewHolder {
        return BannerViewHolder.buildFor(parent, attachToRoot, adsObjectsProvider)
    }
}

abstract class GuideViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    open fun bind(item: GuideRow) {}
}

class GuideChannelViewHolder(context: Context, view: View) : GuideViewHolder(view) {
    val binding = ItemGuideChannelLayoutBinding.bind(view)
    val recycler = binding.channelGuide
    var isSelectedChannel: Boolean = false
    var onItemClickListener: ((Channel) -> Unit)? = null
    val adapter: ProgramGuideAdapter = ProgramGuideAdapter()

    init {
        binding.channelGuide.layoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.channelGuide.adapter = adapter
        binding.channelGuide.itemAnimator = null
    }

    override fun bind(item: GuideRow) {
        item as GuideRowChannel
        val holder = this
        val channelName = item.channel.name.replace("_", " ")
        binding.channelName.text = channelName
        scaleChannelNameSize(binding.channelName, channelName)
        binding.channelNumber.text = item.channel.number.toString()

        if (isSelectedChannel) {
            val color = itemView.colorAttr(R.attr.colorOnPrimarySurface2)
            binding.channelNumber.setTextColor(color)
            binding.channelName.setTextColor(color)
            binding.isCurrentChannel.visibility = View.VISIBLE
        } else {
            val color = itemView.colorAttr(R.attr.colorOnPrimarySurface1)
            val colorNumber = itemView.colorAttr(R.attr.colorOnPrimarySurface3)
            binding.channelNumber.setTextColor(colorNumber)
            binding.channelName.setTextColor(color)
            binding.isCurrentChannel.visibility = View.GONE
        }

        if (item.channel.programs != null) {
            adapter.items = item.channel.programs
        }
        adapter.channelSelected = isSelectedChannel
        recyclers.add(binding.channelGuide)

        var acc = 0
        val index = item.channel.programs?.indexOfFirst {
            val durationPx = (holder.itemView.context.pixelsPerSecond() * it.duration).toInt()
            acc += durationPx
            if (acc > currentScroll) {
                acc -= durationPx
                true
            } else false
        } ?: 0
        binding.channelGuide.resetScroll(index, currentScroll - acc)
        binding.channelGuide.addOnScrollListener(onScrollListener)
        binding.channelGuide.isClickable = false
        adapter.listener = object : ProgramGuideAdapter.Listener {
            override fun onClickListener(program: Program, position: Int) {
                onItemClickListener?.invoke(item.channel)
            }
        }
        binding.root.setOnClickListener {
            onItemClickListener?.invoke(item.channel)
        }
    }

    private fun scaleChannelNameSize(textView: TextView, name: String) {
        val width = textView.dimenAttr(R.attr.guideChannelNameWidth).toFloat().dpToPx()
        val maxTextSize = textView.dimenAttr(R.attr.guideChannelTextSize).toFloat().pxToSp()
        val minTextSize = maxTextSize / 3
        val linesCount = 2
        val delta = 2f
        val nameWidthPx = maxTextSize.spToPx() * name.length

        val pxPerSymbol = if (nameWidthPx > width * linesCount) {
            delta + (width * linesCount) / name.length
        } else {
            val nameParts = name.split(" ")
            val maxPart = nameParts.maxByOrNull { it.length } ?: return
            delta + width / maxPart.length
        }
//        val scaledSize = pxPerSymbol.pxToSp()
        textView.textSize = pxPerSymbol.pxToSp().clamp(minTextSize, maxTextSize)
    }
}

class GuideCategoryViewHolder(view: View) : GuideViewHolder(view) {
    override fun bind(item: GuideRow) {
        item as GuideRowCategory
        val itemView = itemView as TextView
        itemView.text = item.name
    }
}

fun Float.dpToPx(): Float {
    val density = Resources.getSystem().displayMetrics.density
    val px = ceil(this * density)
    return px
}

fun Float.pxToDp(): Float {
    val density = Resources.getSystem().displayMetrics.density
    val dp = ceil(this / density)
    return dp
}

fun Float.spToPx(): Float {
    val density = Resources.getSystem().displayMetrics.scaledDensity
    val px = ceil(this * density)
    return px
}

fun Float.pxToSp(): Float {
    val density = Resources.getSystem().displayMetrics.scaledDensity
    val sp = ceil(this / density)
    return sp
}

fun Float.clamp(from: Float, to: Float): Float {
    return Math.max(Math.min(this, to), from)
}