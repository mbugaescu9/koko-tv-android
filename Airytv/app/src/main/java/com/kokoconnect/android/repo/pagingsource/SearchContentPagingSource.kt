package com.kokoconnect.android.repo.pagingsource

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.model.vod.ContentDataGridRow
import com.kokoconnect.android.model.vod.ContentRow
import com.kokoconnect.android.repo.AiryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class SearchContentPagingSource(
    private var repo: AiryRepository,
    var bridge: Bridge,
    var query: String = ""
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
    private val resultEmpty = LoadResult.Page<Int, ContentRow>(emptyList(), null, null)

    init {
        registerInvalidatedCallback {
            lastLoadedPage = 0
            totalPagesCount = 0
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ContentRow> {
        val pageNumber = params.key ?: 1
        if (query.isEmpty()) return resultEmpty
        return try {
            bridge.onLoadingStarted()
            val searchResponse = withContext(Dispatchers.IO) {
                repo.searchContent(query, pageNumber)
            }
            bridge.onLoadingFinished()
            lastLoadedPage = searchResponse.page
            totalPagesCount = searchResponse.totalPages

            val contents = searchResponse.collections
            val newRows = mutableListOf<ContentRow>()
            contents.forEachIndexed { idx, content ->
//                    if ((idx+1) % 6 == 0 && idx > 0) {
//                        //add banner after every 6 contents
//                        newRows.addAll(listOf(ContentBannerRow()))
//                    }
                newRows.add(
                    ContentDataGridRow(content)
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