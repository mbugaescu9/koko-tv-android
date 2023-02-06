package com.kokoconnect.android.model.vod

import com.google.gson.annotations.SerializedName

open class Collection() {
    companion object {
        const val COLLECTION_TYPE_TVSHOWS = "TV Shows"
    }

    var id: Long? = null
    var name: String? = null
    var contents: MutableList<Content> = mutableListOf()
    var description: String? = null
    var type: String? = null

    fun addContent(newContent: List<Content>) {
        for (content in newContent) {
            val idx = contents.indexOfFirst { it.id == content.id }
            if (idx !in contents.indices) {
                contents.add(content)
            }
//            else {
//                contents[idx] = content
//            }
        }
    }
}

open class CollectionsResponse {
    var collections: List<Collection> = emptyList()

    var total: Int = 0
    var count: Int = 0
    var page: Int = 0
    var limit: Int = 0
    @SerializedName("total_pages")
    var totalPages: Int = 0
}

class CollectionResponse: Collection() {
    var total: Int = 0
    var count: Int = 0
    var page: Int = 0
    var limit: Int = 0
    @SerializedName("total_pages")
    var totalPages: Int = 0
}

class SearchContentResponse: Collection() {
    var total: Int = 0
    var count: Int = 0
    var page: Int = 0
    var limit: Int = 0
    @SerializedName("total_pages")
    var totalPages: Int = 0

    @SerializedName("collections")
    var collections: MutableList<Content> = mutableListOf()
}

class News() : Collection() {}

class Favourites() : Collection() {}

class Recommendations() : Collection() {}