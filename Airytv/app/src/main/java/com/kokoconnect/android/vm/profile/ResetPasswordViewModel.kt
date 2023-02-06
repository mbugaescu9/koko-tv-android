package com.kokoconnect.android.vm.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kokoconnect.android.model.auth.ResetPasswordResponse
import com.kokoconnect.android.repo.AuthError

class ResetPasswordViewModel: ViewModel() {
    val loginLiveData = MutableLiveData<String>().apply{value = ""}

    val resultLiveData = MutableLiveData<ResetPasswordResponse?>()
    val errorLiveData = MutableLiveData<AuthError?>()
}