package com.kokoconnect.android.util.diffutli

import androidx.recyclerview.widget.DiffUtil
import com.kokoconnect.android.model.vod.*

class CollectionsRowDiffUtilCallback() : DiffUtil.ItemCallback<CollectionsRow>() {
    override fun areItemsTheSame(oldItem: CollectionsRow, newItem: CollectionsRow): Boolean {
        return when {
            oldItem is CollectionRow && newItem is CollectionRow -> {
                oldItem.collection.id == newItem.collection.id
            }
            oldItem is CollectionBannerRow && newItem is CollectionBannerRow -> {
                true
            }
            oldItem is CollectionLoadingProgressRow && newItem is CollectionLoadingProgressRow -> {
                true
            }
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: CollectionsRow, newItem: CollectionsRow): Boolean {
        return when {
            oldItem is CollectionRow && newItem is CollectionRow -> {
                oldItem.collection.description == newItem.collection.description
                        && oldItem.collection.name == newItem.collection.name
                        && oldItem.collection.type == newItem.collection.type
                        && oldItem.collection.contents.size == oldItem.collection.contents.size
                        && oldItem.collection.contents.let {
                    var isSameItems = true
                    for (index in newItem.collection.contents.indices) {
                        isSameItems = it[index].id == newItem.collection.contents[index].id
                        if (!isSameItems) break
                    }
                    isSameItems
                }
            }
            oldItem is CollectionBannerRow && newItem is CollectionBannerRow -> {
                true
            }
            oldItem is CollectionLoadingProgressRow && newItem is CollectionLoadingProgressRow -> {
                true
            }
            else -> false
        }
    }

}