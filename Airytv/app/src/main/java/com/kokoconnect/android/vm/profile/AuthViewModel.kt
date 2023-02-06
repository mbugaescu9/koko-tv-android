package com.kokoconnect.android.vm.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.model.auth.CheckTokenRequest
import com.kokoconnect.android.model.auth.TokenResponse
import com.kokoconnect.android.model.auth.User
import com.kokoconnect.android.model.player.ArchiveApiKey
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.repo.AuthRepository
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.repo.AuthError
import timber.log.Timber
import javax.inject.Inject

class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val airyRepo: AiryRepository
) : ViewModel() {
    var needOpenAuth = MutableLiveData<Boolean?>(null)

    var needUpdateArchiveApiKey = MutableLiveData<ArchiveApiKey?>()
    var archiveApiKey: ArchiveApiKey
        get() {
            val header = Preferences(AiryTvApp.instance).ArchiveOrg().getAuthKey() ?: ""
            return ArchiveApiKey(header)
        }
        set(value) {
            Preferences(AiryTvApp.instance).ArchiveOrg().setAuthKey(value.header)
        }

    var user = MutableLiveData<User?>()

    var isAuthorized: MutableLiveData<Boolean> = MutableLiveData(getToken() != null)
    private var needUpdateTokenStatus = MutableLiveData<Boolean?>().apply {}

    init {
        needUpdateTokenStatus.observeForever {
            val isTokenValid = it != null
            if (!isTokenValid) {
                signOut()
            } else {
                updateIsAuthorized()
            }
        }
        needUpdateArchiveApiKey.observeForever {
            it?.let {
                archiveApiKey = it
            }
        }
    }

    fun isAuthorized(): Boolean {
        return isAuthorized.value ?: false
    }

    fun updateIsAuthorized() {
        isAuthorized.postValue(getToken() != null)
    }

    fun getToken() = authRepo.getToken()

    fun setToken(token: String) {
        authRepo.setToken(token)
    }

    fun onSignedIn(
        user: User?,
        token: String?,
        email: String?
    ) {
        Timber.d("onSignedIn() token ${token} email ${email}")
        this.user.postValue(user)
        authRepo.setToken(token)
        authRepo.setEmail(email)
        updateIsAuthorized()
    }

    fun signOut() {
        this.user.postValue(null)
        authRepo.setEmail(null)
        authRepo.setToken(null)
        updateIsAuthorized()
    }

    fun checkToken() {
        val token = getToken()
        if (token != null) {
            authRepo.checkToken(token, needUpdateTokenStatus)
        } else {
            needUpdateTokenStatus.postValue(false)
        }
    }

    fun checkToken(
        token: String?,
        result: MutableLiveData<TokenResponse?>,
        errorLiveData: MutableLiveData<AuthError?>
    ) {
        authRepo.checkToken(CheckTokenRequest(token), result, errorLiveData)
    }

    suspend fun checkToken(
        token: String?
    ): Boolean {
        return try {
            authRepo.checkToken(CheckTokenRequest(token))
            true
        } catch (ex: ApiErrorThrowable) {
            false
        }
    }

    fun updateArchiveApiKey() {
        airyRepo.getArchiveApiKey(needUpdateArchiveApiKey)
    }

    fun openAuth() {
        needOpenAuth.postValue(true)
    }
}