package com.kokoconnect.android.adapter.tv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.model.tv.Program
import com.kokoconnect.android.util.DateUtils.parseIsoDate
import com.kokoconnect.android.vh.GuideItemViewHolder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class ProgramGuideAdapter : RecyclerView.Adapter<GuideItemViewHolder>() {
    private var selectedPosition: Int = -1
    val sdf = SimpleDateFormat("HH:mm:ss")

    var channelSelected: Boolean by Delegates.observable(false) { _: KProperty<*>, _: Boolean, _: Boolean ->
        notifyItemChanged(positionForProgress())
    }
    var listener: Listener? = null
    var items by Delegates.observable(listOf()) { _: KProperty<*>, _: List<Program>, newValue: List<Program>? ->
        if (newValue != null) {
            updateSelectedPosition()
        }
    }

    fun updateSelectedPosition() {
        selectedPosition = positionForProgress()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_guide_program, parent, false)
        return GuideItemViewHolder(itemView)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: GuideItemViewHolder, position: Int) {
        items.getOrNull(position)?.let {
            holder.listener = listener
            holder.bind(it, position, channelSelected, selectedPosition)
        }
    }

    private fun positionForProgress(): Int {
        val currentTime = calculateCurrentTime()
        return items.indexOfFirst {
            val time = parseIsoDate(it.realStartAtIso)
            val result = time <= currentTime && time + it.realDuration * 1000 > currentTime
            result
        }
    }

    private fun calculateCurrentTime() = Calendar.getInstance().apply {
//        set(Calendar.YEAR, 1970)
//        set(Calendar.MONTH, Calendar.JANUARY)
//        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis


    interface Listener {
        fun onClickListener(program: Program, position: Int)
    }
}

