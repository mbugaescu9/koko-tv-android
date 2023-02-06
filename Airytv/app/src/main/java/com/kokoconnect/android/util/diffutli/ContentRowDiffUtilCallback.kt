package com.kokoconnect.android.util.diffutli

import androidx.recyclerview.widget.DiffUtil
import com.kokoconnect.android.model.vod.*

class ContentRowDiffUtilCallback() : DiffUtil.ItemCallback<ContentRow>() {
    override fun areItemsTheSame(oldItem: ContentRow, newItem: ContentRow): Boolean {
        return when {
            oldItem is ContentDataRow && newItem is ContentDataRow -> {
                oldItem.content.id == newItem.content.id
            }
            oldItem is ContentBannerRow && newItem is ContentBannerRow -> {
                true
            }
            oldItem is ContentLoadingProgressRow && newItem is ContentLoadingProgressRow -> {
                true
            }
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ContentRow, newItem: ContentRow): Boolean {
        return when {
            oldItem is ContentDataRow && newItem is ContentDataRow -> {
                oldItem.content.description == newItem.content.description
                        && oldItem.content.name == newItem.content.name
                        && oldItem.content.type == newItem.content.type
                        && oldItem.content.sourceUrl == newItem.content.sourceUrl
                        && oldItem.content.duration == newItem.content.duration
            }

            oldItem is ContentBannerRow && newItem is ContentBannerRow -> {
                true
            }
            oldItem is ContentLoadingProgressRow && newItem is ContentLoadingProgressRow -> {
                true
            }
            else -> false
        }
    }

}