package com.kokoconnect.android.vh

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.tv.ProgramGuideAdapter
import com.kokoconnect.android.adapter.tv.pixelsPerSecond
import com.kokoconnect.android.databinding.ItemGuideProgramBinding
import com.kokoconnect.android.model.tv.Program
import com.kokoconnect.android.util.getAttrColor

class GuideItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val binding = ItemGuideProgramBinding.bind(view)
    var listener: ProgramGuideAdapter.Listener? = null

    fun bind(
        program: Program,
        position: Int,
        channelSelected: Boolean = false,
        selectedPosition: Int = -1
    ) {
        val context = binding.root.context
        binding.tvName.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                listener?.onClickListener(program, position)
            }
        }
        binding.tvName.layoutParams.width =
            (itemView.context.pixelsPerSecond() * program.duration).toInt()
        binding.tvName.text = program.name

        val needHighLight = channelSelected && position == selectedPosition
        binding.tvName.setBackgroundColor(
            if (needHighLight) {
                context.getAttrColor(R.attr.colorSurfaceVariant18)
            } else {
                context.getAttrColor(R.attr.colorSecondarySurface)
            }
        )
    }
}