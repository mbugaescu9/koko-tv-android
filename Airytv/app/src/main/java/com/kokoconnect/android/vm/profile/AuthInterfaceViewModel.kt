package com.kokoconnect.android.vm.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.facebook.CallbackManager


class AuthInterfaceViewModel: ViewModel() {
    var facebookAuthCallback = CallbackManager.Factory.create()
    val onAuthCompleted = MutableLiveData<Boolean>()
    val needCloseFragment = MutableLiveData<Boolean>()

    fun completeAuth() {
        onAuthCompleted.postValue(true)
    }

    fun closeFragment() {
        needCloseFragment.postValue(true)
    }
}