package com.kokoconnect.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.databinding.ItemMenuBinding
import com.kokoconnect.android.model.ui.MenuItem

class MenuListAdapter (var items: List<MenuItem>, var listener: Listener? = null) : RecyclerView.Adapter<MenuListAdapter.MenuItemsViewHolder>() {


    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMenuBinding.inflate(inflater, parent, false)
        return MenuItemsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuItemsViewHolder, position: Int) {
        items.getOrNull(position)?.let {
            holder.bind(it)
        }
    }

    inner class MenuItemsViewHolder(val binding: ItemMenuBinding): RecyclerView.ViewHolder(binding.root) {
        val iconImageView = binding.icon
        val nameTextView = binding.name
        val descriptionTextView = binding.descriptionFragment
        val container = binding.container

        fun bind(menuItem: MenuItem) {
            iconImageView.setImageResource(menuItem.icon)
            nameTextView.setText(menuItem.name)
            descriptionTextView.setText(menuItem.description)
            container.setOnClickListener {
                listener?.onClick(menuItem)
            }
        }
    }

    interface Listener {
        fun onClick(item: MenuItem)
    }

}