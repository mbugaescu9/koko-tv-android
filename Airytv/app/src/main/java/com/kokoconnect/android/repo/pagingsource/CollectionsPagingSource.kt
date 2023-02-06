package com.kokoconnect.android.repo.pagingsource

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.model.vod.CollectionBannerRow
import com.kokoconnect.android.model.vod.CollectionRow
import com.kokoconnect.android.model.vod.CollectionsRow
import com.kokoconnect.android.model.vod.CollectionsRowTypes
import com.kokoconnect.android.repo.AiryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionsPagingSource(
    private var repo: AiryRepository,
    var bridge: Bridge
) : PagingSource<Int, CollectionsRow>() {
    companion object {
        val pagingConfig = PagingConfig(
            initialLoadSize = 5,
            pageSize = 5,
            prefetchDistance = 1
        )
    }

    var lastLoadedPage: Int = 0
    var totalPagesCount: Int = 0

    init {
        registerInvalidatedCallback {
            lastLoadedPage = 0
            totalPagesCount = 0
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CollectionsRow> {
        return try {
            val pageNumber = params.key ?: 1
            bridge.onLoadingStarted()
            val collectionsResponse = withContext(Dispatchers.IO) {
                repo.getAllCollections(pageNumber)
            }
            bridge.onLoadingFinished()
            lastLoadedPage = collectionsResponse.page
            totalPagesCount = collectionsResponse.totalPages

            val collections = collectionsResponse.collections
            val newRows = mutableListOf<CollectionsRow>()
            collections.forEachIndexed { idx, value ->
                if ((idx + 1) % 2 == 0 && idx > 0) {
                    //add banner after every 2 episodes
                    newRows.addAll(listOf(CollectionBannerRow()))
                }
                newRows.add(
                    CollectionRow(CollectionsRowTypes.COLLECTION_LIST_HORIZONTAL, value)
                )
            }
            val nextKey = if (totalPagesCount <= lastLoadedPage) null else lastLoadedPage + 1
            LoadResult.Page(newRows, null, nextKey)
        } catch (ex: ApiErrorThrowable) {
            bridge.onLoadingFinished()
            bridge.onApiError(ex)
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CollectionsRow>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    interface Bridge {
        fun onApiError(apiThrowable: ApiErrorThrowable)
        fun onLoadingStarted()
        fun onLoadingFinished()
    }
}