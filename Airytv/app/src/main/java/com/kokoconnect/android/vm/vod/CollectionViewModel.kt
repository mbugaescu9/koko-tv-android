package com.kokoconnect.android.vm.vod

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.cachedIn
import com.kokoconnect.android.model.response.ApiError
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.model.vod.Collection
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.repo.pagingsource.CollectionPagingSource
import timber.log.Timber
import javax.inject.Inject

class CollectionViewModel @Inject constructor(private val airyRepo: AiryRepository) : ViewModel() {
    private var collectionPagingSource: CollectionPagingSource? = null
    var collectionLiveData = MutableLiveData<Collection?>()
    var errorLiveData = MutableLiveData<ApiError>()
    var loading = MutableLiveData<Boolean>()
    val contentPager = Pager(
        config = CollectionPagingSource.pagingConfig,
        pagingSourceFactory = ::createCollectionPagingSource
    )
    val contentRows = contentPager.flow.cachedIn(viewModelScope).asLiveData()

    fun isLoading(): Boolean = loading.value == true

    fun getCollection(): Collection? = collectionLiveData.value

    private fun createCollectionPagingSource(): CollectionPagingSource {
        return CollectionPagingSource(airyRepo, object : CollectionPagingSource.Bridge {
            override fun onApiError(apiThrowable: ApiErrorThrowable) {
                errorLiveData.postValue(apiThrowable.errorType)
            }

            override fun onLoadingStarted() {
                loading.postValue(true)
            }

            override fun onLoadingFinished() {
                loading.postValue(false)
            }
        }, getCollection()?.id).apply {
            collectionPagingSource = this
        }
    }

    fun setCollection(collection: Collection) {
        collectionLiveData.value = collection
        val collectionId = collection.id
        collectionPagingSource?.collectionId = collectionId
        collectionPagingSource?.invalidate()
    }

    fun reloadCollection() {
        if (!isLoading() && getCollection() != null) {
            Timber.d("reloadCollection() collection id = ${getCollection()?.id}")
            collectionPagingSource?.invalidate()
        }
    }

    fun reset() {
        collectionPagingSource?.collectionId = null
        collectionLiveData.value = null
        loading.value = false
    }
}