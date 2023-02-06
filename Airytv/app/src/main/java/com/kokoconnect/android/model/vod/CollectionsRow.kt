package com.kokoconnect.android.model.vod

import com.kokoconnect.android.R

abstract class CollectionsRow (val rowType: CollectionsRowTypes)

class CollectionRow (
    rowType: CollectionsRowTypes,
    val collection: Collection) : CollectionsRow(rowType)

class CollectionBannerRow () : CollectionsRow(CollectionsRowTypes.BANNER_AD) {}

class CollectionLoadingProgressRow : CollectionsRow (CollectionsRowTypes.PROGRESS)

enum class CollectionsRowTypes(var layoutId: Int) {
    COLLECTION_GRID(R.layout.item_collection_grid),
    COLLECTION_LIST_HORIZONTAL(R.layout.item_collection_list_horizontal),
    BANNER_AD(0),
    PROGRESS(R.layout.item_loading_progress)
}
