package com.rgbstudios.alte.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.canhub.cropper.CropImage.CancelledResult.isSuccessful
import com.google.firebase.auth.PhoneAuthProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.data.remote.FirebaseAccess
import com.rgbstudios.alte.data.repository.AlteRepository
import com.rgbstudios.alte.utils.SharedPreferencesManager

class AlteViewModel(application: Application, private val alteRepository: AlteRepository) : ViewModel() {

    private val firebase = FirebaseAccess()
    private val sharedPreferences = SharedPreferencesManager(application)

    // LiveData to hold the isFirstLaunch status
    private val _isFirstLaunch = MutableLiveData<Boolean>()
    val isFirstLaunch: LiveData<Boolean> = _isFirstLaunch

    // LiveData to hold the isUsernameSet status
    private val _isUsernameSet = MutableLiveData<Boolean>()
    val isUsernameSet: LiveData<Boolean> = _isUsernameSet

    // LiveData to hold current user
    private val _currentUser = MutableLiveData<UserDetails>()
    val currentUser: LiveData<UserDetails> = _currentUser

    private val _verificationInfo = MutableLiveData<Triple<String, PhoneAuthProvider.ForceResendingToken, String>>()
    val verificationInfo: LiveData<Triple<String, PhoneAuthProvider.ForceResendingToken, String>> = _verificationInfo

    init {
        _isFirstLaunch.value = getIsFirstLaunch()
        _isUsernameSet.value = getIsUsernameSet()
    }

    private fun getIsFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean("isFirstLaunch", true)
    }

    private fun getIsUsernameSet(): Boolean {
        return sharedPreferences.getBoolean("isUsernameSet", false)
    }
    fun saveVerificationInfo(verificationId: String, resendingToken: PhoneAuthProvider.ForceResendingToken, fullPhoneNumber: String) {
        val info = Triple(verificationId, resendingToken, fullPhoneNumber)
        _verificationInfo.value = info
    }

    // Function to update the isFirstLaunch status
    fun updateFirstLaunchStatus(isFirstLaunch: Boolean) {
        _isFirstLaunch.value = isFirstLaunch

        // Store the updated isFirstLaunch status in SharedPreferences
        sharedPreferences.putBoolean("isFirstLaunch", isFirstLaunch)
    }

    // Function to update the isUsernameSet status
    fun updateUsernameSetStatus(isUsernameSet: Boolean) {
        _isUsernameSet.value = isUsernameSet

        // Store the updated isUsernameSet status in SharedPreferences
        sharedPreferences.putBoolean("isUsernameSet", isUsernameSet)
    }

    fun saveUserDetails(user: UserDetails, callback: (Boolean) -> Unit) {
        alteRepository.saveUserDetailsToDatabase(user){ successful ->
                if (successful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
    }

    fun logOut(callback: (Boolean, String?) -> Unit) {

        firebase.logOut { logOutSuccessful, errorMessage ->
            if (logOutSuccessful) {
                firebase.addLog("sign out successful")


                callback(true, null)
            } else {
                firebase.addLog("sign out failed")
                callback(false, errorMessage)
            }
        }
    }

}