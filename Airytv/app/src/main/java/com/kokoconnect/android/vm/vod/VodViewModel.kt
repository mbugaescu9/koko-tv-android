package com.kokoconnect.android.vm.vod

import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.cachedIn
import com.kokoconnect.android.model.response.ApiError
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.repo.pagingsource.CollectionPagingSource
import com.kokoconnect.android.repo.pagingsource.CollectionsPagingSource
import com.kokoconnect.android.repo.pagingsource.SearchContentPagingSource
import javax.inject.Inject

class VodViewModel @Inject constructor(val airyRepo: AiryRepository) : ViewModel() {
    private var collectionsPagingSource: CollectionsPagingSource? = null
    val collectionPaging = Pager(
        config = CollectionPagingSource.pagingConfig,
        pagingSourceFactory = ::createCollectionsPagingSource
    )
    val collectionsRows = collectionPaging.flow.cachedIn(viewModelScope).asLiveData()
    private var searchContentPagingSource: SearchContentPagingSource? = null
    val searchPager = Pager(
        config = SearchContentPagingSource.pagingConfig,
        pagingSourceFactory = ::createSearchContentPagingSource
    )
    val searchRows = searchPager.flow.cachedIn(viewModelScope).asLiveData()
    var searchQuery = MutableLiveData<String>("")
    var errorLiveData = MutableLiveData<ApiError>()
    var loading = MutableLiveData<Boolean>()
    var searchLoading = MutableLiveData<Boolean>()
    var searchResultsVisible = searchQuery.map {
        it.isNotEmpty()
    }

    init {
        searchQuery.observeForever {
            searchContentPagingSource?.query = it
        }
    }

    private fun createSearchContentPagingSource(): SearchContentPagingSource {
        return SearchContentPagingSource(
            airyRepo,
            object : SearchContentPagingSource.Bridge {
                override fun onApiError(apiThrowable: ApiErrorThrowable) {
                    errorLiveData.postValue(apiThrowable.errorType)
                }

                override fun onLoadingStarted() {
                    searchLoading.postValue(true)
                }

                override fun onLoadingFinished() {
                    searchLoading.postValue(false)
                }
            },
            getSearchQuery()
        ).apply {
            searchContentPagingSource = this
        }
    }


    private fun createCollectionsPagingSource(): CollectionsPagingSource {
        return CollectionsPagingSource(
            airyRepo,
            object : CollectionsPagingSource.Bridge {
                override fun onApiError(apiThrowable: ApiErrorThrowable) {
                    errorLiveData.postValue(apiThrowable.errorType)
                }

                override fun onLoadingStarted() {
                    loading.postValue(true)
                }

                override fun onLoadingFinished() {
                    loading.postValue(false)
                }
            }
        ).apply {
            collectionsPagingSource = this
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun getSearchQuery(): String {
        return searchQuery.value ?: ""
    }

    fun searchContent() {
        searchContentPagingSource?.invalidate()
    }

    fun reloadCollections() {
        collectionsPagingSource?.invalidate()
    }
}