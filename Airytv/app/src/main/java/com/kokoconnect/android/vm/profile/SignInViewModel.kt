package com.kokoconnect.android.vm.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kokoconnect.android.model.auth.TokenResponse
import com.kokoconnect.android.repo.AuthError

class SignInViewModel: ViewModel() {
    val loginLiveData = MutableLiveData<String>().apply{value = ""}
    val passwordLiveData = MutableLiveData<String>().apply{value = ""}

    val resultLiveData = MutableLiveData<TokenResponse?>()
    val errorLiveData = MutableLiveData<AuthError?>()
}