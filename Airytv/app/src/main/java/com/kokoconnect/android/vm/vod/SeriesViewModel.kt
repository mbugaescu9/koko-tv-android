package com.kokoconnect.android.vm.vod

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.cachedIn
import com.kokoconnect.android.model.response.ApiError
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.model.vod.Episode
import com.kokoconnect.android.model.vod.Series
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.repo.pagingsource.SeriesPagingSource
import timber.log.Timber
import javax.inject.Inject

class SeriesViewModel @Inject constructor(private val airyRepo: AiryRepository) : ViewModel() {
    private var seriesPagingSource: SeriesPagingSource? = null
    val seriesPager = Pager(
        config = SeriesPagingSource.pagingConfig,
        pagingSourceFactory = ::createSeriesPagingSource
    )
    val seriesRows = seriesPager.flow.cachedIn(viewModelScope).asLiveData()

    var needOpenEpisode = MutableLiveData<Episode>()
    var needShowSeriesDescription = MutableLiveData<Boolean>()
    var needShowBanners = MutableLiveData<Boolean>()

    var seriesLiveData = MutableLiveData<Series?>()
    var seriesIdLiveData = MutableLiveData<Long?>()
    var errorLiveData = MutableLiveData<ApiError?>()

    var loading = MutableLiveData<Boolean>()

    fun createSeriesPagingSource(): SeriesPagingSource {
        return SeriesPagingSource(
            airyRepo,
            object : SeriesPagingSource.Bridge {
                override fun onApiError(apiThrowable: ApiErrorThrowable) {
                    Timber.d("onApiError() ${apiThrowable.errorType}")
                    errorLiveData.postValue(apiThrowable.errorType)
                }

                override fun onLoadingStarted() {
                    Timber.d("onLoadingStarted()")
                    loading.postValue(true)
                }

                override fun onLoadingFinished() {
                    Timber.d("onLoadingFinished()")
                    loading.postValue(false)
                }

                override fun onSeriesReceived(series: Series) {
                    seriesLiveData.postValue(series)
                }
            },
            seriesId = getSeriesId()
        ).apply {
            seriesPagingSource = this
        }
    }


    fun setSeries(series: Series) {
        seriesLiveData.value = series
        series.id?.let {
            setSeriesId(it)
        }
    }

    fun setSeriesId(seriesId: Long) {
        seriesIdLiveData.value = seriesId
        seriesPagingSource?.seriesId = seriesId
        seriesPagingSource?.invalidate()
    }

    fun getSeries(): Series? = seriesLiveData.value

    fun getSeriesId(): Long? = seriesIdLiveData.value

    fun isLoading(): Boolean = loading.value ?: false

    fun reloadEpisodes() {
        if (!isLoading() && getSeriesId() != null) {
            Timber.d("reloadSeries() seriesId ${getSeriesId()}")
            seriesPagingSource?.invalidate()
        }
    }

    fun reset() {
        seriesPagingSource?.series = null
        seriesPagingSource?.seriesId = null
        seriesLiveData.value = null
        loading.value = false
    }
}