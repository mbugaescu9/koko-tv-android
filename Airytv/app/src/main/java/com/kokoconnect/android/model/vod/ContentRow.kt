package com.kokoconnect.android.model.vod

import com.kokoconnect.android.R

abstract class ContentRow (val rowType: ContentRowTypes)

abstract class ContentDataRow(
    val content: Content,
    val collection: Collection? = null,
    type: ContentRowTypes
): ContentRow(type)

class ContentDataGridRow (
    content: Content,
    collection: Collection? = null
) : ContentDataRow(
    content,
    collection,
    ContentRowTypes.CONTENT_GRID
)

class ContentDataListHorizontalRow (
    content: Content,
    collection: Collection? = null
) : ContentDataRow(
    content,
    collection,
    ContentRowTypes.CONTENT_LIST_HORIZONTAL
)

class ContentBannerRow() : ContentRow(ContentRowTypes.BANNER_AD) {}

class ContentLoadingProgressRow: ContentRow(ContentRowTypes.PROGRESS)

enum class ContentRowTypes(var layoutId: Int) {
    CONTENT_GRID(R.layout.item_collection_grid_element),
    CONTENT_LIST_HORIZONTAL(R.layout.item_collection_list_horiz_element),
    BANNER_AD(0),
    PROGRESS(R.layout.item_loading_progress)
}
