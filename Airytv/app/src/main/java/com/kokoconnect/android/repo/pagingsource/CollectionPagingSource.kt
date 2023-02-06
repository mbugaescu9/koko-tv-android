package com.kokoconnect.android.repo.pagingsource

import androidx.paging.*
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.repo.AiryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionPagingSource(
    private var repo: AiryRepository,
    var bridge: Bridge,
    var collectionId: Long? = null
) : PagingSource<Int, ContentRow>() {
    companion object {
        val pagingConfig = PagingConfig(
            pageSize = 10,
            initialLoadSize = 10,
            prefetchDistance = 1
        )
    }

    var lastLoadedPage: Int = 0
    var totalPagesCount: Int = 0
    var loadedCount: Int = 0
    private val resultEmpty = LoadResult.Page<Int, ContentRow>(emptyList(), null, null)

    init {
        registerInvalidatedCallback {
            loadedCount = 0
            lastLoadedPage = 0
            totalPagesCount = 0
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ContentRow> {
        val id = collectionId ?: return resultEmpty
        return try {
            val pageNumber = params.key ?: 1
            bridge.onLoadingStarted()
            val collectionResponse = withContext(Dispatchers.IO) {
                repo.getCollectionContent(id, pageNumber)
            }
            bridge.onLoadingFinished()
            lastLoadedPage = collectionResponse.page
            totalPagesCount = collectionResponse.totalPages

            val contents = collectionResponse.contents
            val newRows = mutableListOf<ContentRow>()
            contents.forEachIndexed { idx, content ->
                val loadedIdx = loadedCount + idx
                if (loadedIdx % 6 == 0 && loadedIdx > 0) {
                    //add banner after every 6 contents
                    newRows.addAll(listOf(ContentBannerRow()))
                }
                newRows.add(
                    ContentDataGridRow(content)
                )
            }

            loadedCount += contents.size
            val nextKey = if (totalPagesCount <= lastLoadedPage) null else lastLoadedPage + 1
            LoadResult.Page(newRows, null, nextKey)
        } catch (ex: ApiErrorThrowable) {
            bridge.onLoadingFinished()
            bridge.onApiError(ex)
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ContentRow>): Int? {
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