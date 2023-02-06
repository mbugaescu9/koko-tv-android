package com.kokoconnect.android.vm.freegift

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.model.auth.CheckTokenRequest
import com.kokoconnect.android.model.auth.TokenResponse
import com.kokoconnect.android.model.error.GiveawaysError
import com.kokoconnect.android.model.error.GiveawaysErrorType
import com.kokoconnect.android.model.giveaways.*
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.model.response.GiveawaysEntryResponse
import com.kokoconnect.android.model.response.TransactionsResponse
import com.kokoconnect.android.repo.AuthRepository
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.repo.AuthError
import kotlinx.coroutines.*
import javax.inject.Inject

class GiveawaysViewModel @Inject constructor(
    private val airyRepo: AiryRepository,
    private val authRepositoryApi: AuthRepository,
    private val app: AiryTvApp
) : AndroidViewModel(app) {
    private var context = getApplication<Application>()
    private val authPrefs = Preferences(context).Auth()
    val giveawaysErrorLiveData = MutableLiveData<GiveawaysError?>()
    val giveawaysInfoLiveData = MediatorLiveData<GiveawaysInfo>()
    private val activeGiveaways = MutableLiveData<Giveaways>()
    private val completedGiveaways = MutableLiveData<Giveaways>()
    val allGiveaways = MediatorLiveData<List<GiveawaysItem>>().apply {
        addSource(activeGiveaways) {
            postValue(getAllGiveaways())
        }
        addSource(completedGiveaways) {
            postValue(getAllGiveaways())
        }
    }
    val transactions = MutableLiveData<List<Transaction>?>(null)

    val giveawaysLoaded: Boolean
        get() {
            return activeGiveaways.value != null
        }

    init {
        giveawaysInfoLiveData.postValue(GiveawaysInfo())
        giveawaysInfoLiveData.addSource(activeGiveaways) {
            giveawaysInfoLiveData.postValue(GiveawaysInfo(it.count, it.limit, it.available))
        }
    }

    private fun getAllGiveaways(): List<GiveawaysItem> {
        val activeGiveawaysList = activeGiveaways.value?.events ?: emptyList()
        val completedGiveawaysList = completedGiveaways.value?.events ?: emptyList()
        activeGiveawaysList?.forEach {
            it.isActive = true
        }
        completedGiveawaysList?.forEach {
            it.isActive = false
        }
        val allGiveawaysList = mutableListOf<GiveawaysItem>()
        allGiveawaysList.addAll(activeGiveawaysList)
        allGiveawaysList.addAll(completedGiveawaysList)
        return allGiveawaysList
    }

    fun getTransactions(): List<Transaction>? {
        return transactions.value
    }

    suspend fun requestGiveaways() {
        var activeGiveaways: Giveaways? = null
        var completedGiveaways: Giveaways? = null
        var error: GiveawaysError? = null
        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            val asyncRequestActive = async {
                try {
                    activeGiveaways = airyRepo.getGiveawaysActive(
                        authPrefs.getToken()
                    )
                } catch (ex: ApiErrorThrowable) {
                    error = GiveawaysError(GiveawaysErrorType.ERROR_REQUEST_ACTIVE_LIST, ex)
                }
            }
            val asyncRequestCompleted = async {
                try {
                    completedGiveaways = airyRepo.getGiveawaysCompleted()
                } catch (ex: ApiErrorThrowable) {
                    error = GiveawaysError(GiveawaysErrorType.ERROR_REQUEST_COMPLETED_LIST, ex)
                }
            }
            awaitAll(asyncRequestActive, asyncRequestCompleted)
        }
        if (activeGiveaways != null) {
            this.activeGiveaways.postValue(activeGiveaways)
        } else {
            setGiveawaysError(error)
        }
        if (completedGiveaways != null) {
            this.completedGiveaways.postValue(completedGiveaways)
        } else {
            setGiveawaysError(error)
        }
    }

    suspend fun requestTransactions() {
        val token = authPrefs.getToken()
        if (token == null) {
            return
        }
        var transactionsResponse: TransactionsResponse? = null
        withContext(viewModelScope.coroutineContext) {
            try {
                transactionsResponse = airyRepo.getGiveawaysTransactions(token)
            } catch (ex: ApiErrorThrowable) {

            }
        }
        val transactions = transactionsResponse?.transactions
        if (transactions != null) {
            this.transactions.postValue(transactions)
        }
    }

    fun setGiveawaysError(error: GiveawaysError?) {
        this.giveawaysErrorLiveData.postValue(error)
    }

    suspend fun getGiveawaysTicket(): Boolean {
        val token = authPrefs.getToken()
        if (token == null) {
            setGiveawaysError(GiveawaysError(GiveawaysErrorType.ERROR_NOT_AUTHORIZED))
            return false
        }
        var error: GiveawaysError? = null
        var entryResponse: GiveawaysEntryResponse? = null
        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                entryResponse = airyRepo.getGiveawaysTicket(token)
            } catch (ex: ApiErrorThrowable) {
                error = GiveawaysError(GiveawaysErrorType.ERROR_GET_TICKET)
            }
        }
        val newAvailable = entryResponse?.available
        return if (newAvailable != null) {
            setAvailableTickets(newAvailable)
            true
        } else {
            setGiveawaysError(error)
            false
        }
    }

    suspend fun postGiveawaysEntry(item: GiveawaysItem): Boolean {
        val token = authPrefs.getToken()
        if (token == null) {
            setGiveawaysError(GiveawaysError(GiveawaysErrorType.ERROR_NOT_AUTHORIZED))
            return false
        }
        var error: GiveawaysError? = null
        var entryResponse: GiveawaysEntryResponse? = null
        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                entryResponse = airyRepo.postGiveawaysEntry(
                    token,
                    item.id ?: 0
                )
            } catch (ex: ApiErrorThrowable) {
                error = GiveawaysError(GiveawaysErrorType.ERROR_GET_TICKET)
            }
        }
        val newAvailable = entryResponse?.available
        return if (newAvailable != null) {
            setAvailableTickets(newAvailable)
            true
        } else {
            setGiveawaysError(error)
            false
        }

    }

    fun setAvailableTickets(availableTickets: Int) {
        giveawaysInfoLiveData.value?.let{ giveawaysInfo ->
            giveawaysInfo.available = availableTickets
            giveawaysInfoLiveData.postValue(giveawaysInfo)
        }
    }

    fun checkToken(
        token: String?,
        result: MutableLiveData<TokenResponse?>,
        errorLiveData: MutableLiveData<AuthError?>
    ) {
        authRepositoryApi.checkToken(CheckTokenRequest(token), result, errorLiveData)
    }

    suspend fun checkActiveGiveaways(token: String?): Boolean {
        var hasActiveGiveaways = false
        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                hasActiveGiveaways = airyRepo.checkActiveGiveaways(token)
            } catch (ex: ApiErrorThrowable) {

            }
        }
        return hasActiveGiveaways
    }
}