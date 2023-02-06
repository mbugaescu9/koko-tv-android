package com.kokoconnect.android.repo.pagingsource

import androidx.paging.*
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.model.vod.*
import com.kokoconnect.android.repo.AiryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SeriesPagingSource(
    private var repo: AiryRepository,
    var bridge: Bridge,
    var series: Series? = null,
    var seriesId: Long? = null
) : PagingSource<Int, SeriesRow>() {
    companion object {
        val pagingConfig = PagingConfig(
            initialLoadSize = 5,
            pageSize = 5,
            prefetchDistance = 1
        )
    }

    var lastLoadedPage: Int = 0
    var totalPagesCount: Int = 0
    private val resultEmpty = LoadResult.Page<Int, SeriesRow>(emptyList(), null, null)

    init {
        registerInvalidatedCallback {
            lastLoadedPage = 0
            totalPagesCount = 0
            series?.clear()
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SeriesRow> {
        val seriesId = seriesId ?: series?.id ?: return resultEmpty
        return try {
            val pageNumber = params.key ?: 1
            bridge.onLoadingStarted()
            val seriesResponse = withContext(Dispatchers.IO) {
                repo.getSeries(seriesId, pageNumber)
            }
            bridge.onLoadingFinished()

            lastLoadedPage = seriesResponse.page
            totalPagesCount = seriesResponse.totalPages

            seriesResponse.prepareEpisodes()
            val newEpisodes = seriesResponse.getAllEpisodes()
            seriesResponse.series?.let {
                series = it
                bridge.onSeriesReceived(it)
            }
            series?.addEpisodesFrom(seriesResponse)
            series?.prepare()

            val newRows = mutableListOf<SeriesRow>()
            newRows.add(SeriesHeaderRow(series))
            newEpisodes.forEachIndexed { idx, value ->
                if ((idx) % 2 == 0 && idx > 0) {
                    //add banner after every 2 episodes
                    val bannersRows = listOf(SeriesBannerRow())
                    newRows.addAll(bannersRows)
                }
                newRows.add(SeriesEpisodeRow(value))
            }
            val nextKey = if (totalPagesCount <= lastLoadedPage) null else lastLoadedPage + 1
            LoadResult.Page(newRows, null, nextKey)
        } catch (ex: ApiErrorThrowable) {
            bridge.onLoadingFinished()
            bridge.onApiError(ex)
            return LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SeriesRow>): Int? {
        return state.anchorPosition?.let{ anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    interface Bridge {
        fun onApiError(apiThrowable: ApiErrorThrowable)
        fun onLoadingStarted()
        fun onLoadingFinished()
        fun onSeriesReceived(series: Series)
    }
}