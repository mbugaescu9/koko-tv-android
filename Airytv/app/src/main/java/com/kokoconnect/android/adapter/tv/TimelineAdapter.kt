package com.kokoconnect.android.adapter.tv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.vh.TimelineViewHolder
import java.text.SimpleDateFormat
import java.util.*

const val TIMELINE_INTERVAL = 30 * 60 * 1000

class TimelineAdapter : RecyclerView.Adapter<TimelineViewHolder>() {
    var fromTime: Long = 0
    var toTime: Long = 0
    private var oldProgressPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_timeline, parent, false)
        return TimelineViewHolder(itemView)
    }

    override fun getItemCount(): Int = ((toTime - fromTime) / TIMELINE_INTERVAL).toInt()

    private fun timeFromPosition(position: Int) = fromTime + TIMELINE_INTERVAL * position

    private fun positionForProgress() = ((currentTime - fromTime) / TIMELINE_INTERVAL).toInt()

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        val time = timeFromPosition(position)
        holder.time.text = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(time))
        if (position == positionForProgress()) {
            holder.time.progress = ((currentTime - time) / TIMELINE_INTERVAL.toFloat() * 100).toInt()
            oldProgressPosition = position
            holder.time.updateVisibleArea()
        } else {
            holder.time.progress = null
        }
    }

    fun timeTick() {
        val newPosition = positionForProgress()
        currentTime = calculateCurrentTime()
        if (oldProgressPosition != newPosition) notifyItemChanged(oldProgressPosition)
        notifyItemChanged(newPosition)
    }

    private var currentTime: Long = -1L
        get() {
            if (field == -1L) field = calculateCurrentTime()
            return field
        }

    private fun calculateCurrentTime() = Calendar.getInstance().timeInMillis

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
    }
}


