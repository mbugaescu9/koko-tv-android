package com.kokoconnect.android.repo.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kokoconnect.android.model.vod.ContentDataListHorizontalRow
import com.kokoconnect.android.repo.AiryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

class ContentListHorizontalPagingSource(
    private var repo: AiryRepository,
    var collectionId: Long,
    var items: MutableList<ContentDataListHorizontalRow>
) : PagingSource<Int, ContentDataListHorizontalRow>() {

    val DEFAULT_PAGE_INDEX = 1

    var lastLoadedPage: Int = 0
    var totalPagesCount: Int = 0
    var loadedCount: Int = 0

    init {
        registerInvalidatedCallback {
            loadedCount = 0
            lastLoadedPage = 0
            totalPagesCount = 0
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ContentDataListHorizontalRow>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ContentDataListHorizontalRow> {
        val page = params.key ?: DEFAULT_PAGE_INDEX
        return try {
            Timber.d("ContentListHorizontalPagingSource: load() page == $page, params.loadSize == ${params.loadSize}")
            if (page == 1) {
                LoadResult.Page(
                    items,
                    prevKey = null,
                    nextKey = if (items.size < 6) null else page + 1
                )
            } else {
                val collectionResponse = withContext(Dispatchers.IO) {
                    repo.getCollectionContentForHorizontalAdapter(collectionId, page)
                }
                lastLoadedPage = collectionResponse.page
                totalPagesCount = collectionResponse.totalPages
                val contents = collectionResponse.contents
                val newRows = mutableListOf<ContentDataListHorizontalRow>()
                contents.forEachIndexed { idx, content ->
                    newRows.add(
                        ContentDataListHorizontalRow(content)
                    )
                }
                loadedCount += contents.size
                val nextKey = if (totalPagesCount <= lastLoadedPage) null else lastLoadedPage + 1
                LoadResult.Page(newRows, null, nextKey)
            }
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

}