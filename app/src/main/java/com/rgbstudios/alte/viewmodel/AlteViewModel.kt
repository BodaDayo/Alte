package com.rgbstudios.alte.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.PhoneAuthProvider

class AlteViewModel : ViewModel() {

    private val _verificationInfo = MutableLiveData<Triple<String, PhoneAuthProvider.ForceResendingToken, String>>()
    val verificationInfo: LiveData<Triple<String, PhoneAuthProvider.ForceResendingToken, String>> = _verificationInfo

    fun saveVerificationInfo(verificationId: String, resendingToken: PhoneAuthProvider.ForceResendingToken, fullPhoneNumber: String) {
        val info = Triple(verificationId, resendingToken, fullPhoneNumber)
        _verificationInfo.value = info
    }
}