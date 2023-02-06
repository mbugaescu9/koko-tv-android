package com.kokoconnect.android.util.diffutli

import androidx.recyclerview.widget.DiffUtil
import com.kokoconnect.android.model.vod.Content
import com.kokoconnect.android.model.vod.Episode
import com.kokoconnect.android.model.vod.Series

class SeriesDiffUtilCallback(): DiffUtil.Callback() {
    var oldItems: List<Content> = emptyList()
    var newItems: List<Content> = emptyList()

    constructor(oldItems: List<Content>, newItems: List<Content>) : this() {
        this.oldItems = oldItems
        this.newItems = newItems
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems.getOrNull(oldItemPosition)
        val newItem = newItems.getOrNull(newItemPosition)
        return oldItem != null && newItem != null
                && (oldItem::class == newItem::class)
                && oldItem.id == newItem.id
    }

    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems.getOrNull(oldItemPosition)
        val newItem = newItems.getOrNull(newItemPosition)
        if (oldItem == null && newItem == null) return true
        oldItem ?: return false
        newItem ?: return false

        return when {
            oldItem is Episode && newItem is Episode -> {
                oldItem.number == newItem.number
                        && oldItem.name == newItem.name
                        && oldItem.season?.number == newItem.season?.number
                        && oldItem.publishedAt == newItem.publishedAt
                        && oldItem.poster?.getUrl() == newItem.poster?.getUrl()
                        && oldItem.duration == newItem.duration
            }
            oldItem is Series && newItem is Series -> {
                oldItem.name == newItem.name
                        && oldItem.description == newItem.description
                        && oldItem.poster?.getUrl() == newItem.poster?.getUrl()
            }
            else -> false
        }
    }

}