package com.kokoconnect.android.vh

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.databinding.ItemLoadingProgressBinding

class LoadingProgressViewHolder(val binding: ItemLoadingProgressBinding): RecyclerView.ViewHolder(binding.root) {

    fun setVisible(enabled: Boolean) {
        if (enabled){
            binding.root.visibility = View.VISIBLE
            binding.root.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            binding.root.visibility = View.GONE
            binding.root.layoutParams = RecyclerView.LayoutParams(0,0)
        }
    }
}